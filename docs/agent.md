# 1. 프로그램 개요

- 라즈베리 클라이언트로부터 온도, 습도 정보를 저장받아 관리하는 서버
- Kotlin 2.2 / Spring Boot 4.0 / JPA + Querydsl / MariaDB (테스트는 H2)
- 모든 API 응답은 `ApiResponse<T>` (`code`, `message`, `data`) 형태로 감싸서 응답 (`ApiResponseAdvice`)
- JSON 필드는 snake_case 사용 (`spring.jackson.property-naming-strategy: SNAKE_CASE`, null 필드는 미출력)

## 1.1. 인증 방식 요약

구현된 인증 수단은 4가지이며 용도별로 분리되어 있음

| 수단 | 적용 위치 | 검증 지점 |
| --- | --- | --- |
| 유저 Bearer 토큰 (user id 또는 email) | `POST /devices/register` | `AuthenticationFilter` |
| API key (raw key, sha256 해시 대조) | `/climate/**` 전체 | `AuthenticationFilter` → `AuthService.requireApiKey` |
| 유저 JWT 액세스토큰 | `@JwtAuth` 메소드 | `JwtAuthInterceptor` |
| 관리자 JWT 액세스토큰 | `@AdminAuth` 메소드 | `AdminJwtAuthInterceptor` |

- 별도로 관리자 계정 생성 API 만 고정 토큰(`X-Admin-Token`) 을 사용함
- 인증된 주체는 `AuthenticatedPrincipalArgumentResolver` 를 통해 컨트롤러 파라미터로 주입됨
  (`AuthenticatedUser`, `AuthenticatedApiKey`, `AuthenticatedJwtUser`, `AuthenticatedAdmin`)

# 2. 서비스 API 요구 사항

## 2.1. 디바이스용 기능

### 2.1.1. 디바이스 등록 및 api key 생성 API

- 디바이스에서 호출하는 API 임
- `POST /devices/register` 형태
- Authorization 헤더의 Bearer 토큰(user id 또는 email)으로 유저를 식별. 존재하지 않거나 비활성 사용자라면 실패처리
- 디바이스로부터 전송되는 device_key 기준으로 디바이스를 생성 (이미 등록된 device_key 면 409)
- request body: `device_key`(필수), `name`, `location_name`, `type`(optional)
  - `type` 은 `TEMP_HUMIDITY`(기본) / `MIC` 만 허용
  - `name` 미입력 시 device_key 를 이름으로 사용
- api key 는 **디바이스 단위로 1개** 유지함. 생성한 raw key 에 prefix(`gck_`) 를 붙이고 sha256 해싱한 값을 api_key_hash 에 저장
- 응답은 디바이스 정보 1건 + `api_key`
  - `api_key` 는 raw key 이며 **신규 발급 시에만 1회성으로 응답**. 이미 키가 있는 디바이스면 raw 를 알 수 없으므로 null
- 성공 시 201 Created

### 2.1.2. 온도 습도 저장 API

- 디바이스로부터 온도와 습도 를 획득하여 저장
- `POST /climate/{device_key}` 형태
- request body 는 아래와 같음
```json
{
  "temperature_c": 24.5,
  "humidity": 55.0,
  "measured_at": "2026-05-03T04:00:00"
}
```
- 유효성 확인
  - Authorization 헤더의 Bearer 토큰(raw api key)이 유효하다면 동작 수행
    - 저장된 api_key_hash 와 대조하고, api key / 유저 / 디바이스가 모두 ACTIVE 이며 만료되지 않아야 함
  - device_key 가 유효하고 ACTIVE 상태여야 함
  - api key 가 가리키는 디바이스와 path 의 device_key 가 동일해야 함 (다르면 403)
- 모두 유효성이 정상이라면 device_measurements 테이블에 데이터 저장
  - `measured_at` 은 요청값이 아닌 **서버 수신 시각**으로 저장함 (디바이스 시계 오차 방지)
  - 저장과 함께 devices.last_seen_at 갱신
- 인증 성공 시 user_api_keys.last_used_at 갱신
- 성공 시 201 Created

### 2.1.3. 디바이스 클레임 코드 발급 API

- 디바이스에 유저 자격을 심어주기 위한 온보딩용 API 임
- `POST /devices/claim-codes` 형태, `@JwtAuth` (유저 JWT 액세스토큰 필요)
- 유저가 웹에서 일회용 코드를 발급받아 디바이스에 입력하는 흐름
- 코드 형태는 `GAON-XXXX-XXXX` (혼동 문자 0/O/1/I/L/U 제외한 대문자+숫자 8자리)
- 코드 저장은 Caffeine 기반 in-process 저장소(`ClaimCodeStore`)를 사용하며 TTL 만료 시 자동 소멸
  - TTL 은 `app.device.claim-code.ttl-seconds` (기본 600초)
  - 실제 저장 TTL 에는 2초의 여유(grace)를 더해 막판 제출이 만료로 처리되지 않게 함
- 응답: `claim_code`, `expires_in`(설정 TTL, 초). 웹은 이 값으로 카운트다운
- 성공 시 201 Created

### 2.1.4. 디바이스 클레임 API

- 디바이스가 클레임 코드로 자가 등록하는 API 임
- `POST /devices/claim` 형태, **인증 불필요** (코드 자체가 자격 증명)
- request body: `claim_code`(필수), `name`, `location_name`, `type`(optional)
- 코드는 조회+소멸을 원자적으로 처리하여 1회만 사용 가능
  - 없음 / 만료 / 사용완료를 구분하지 않고 모두 `INVALID_CLAIM_CODE` (404) 로 응답
- device_key 는 UUID(하이픈 제거)로 서버가 자동 생성
- 디바이스 저장 후 api key 발급 (2.1.1 과 동일하게 raw key 1회성 응답)
- 성공 시 201 Created

## 2.2. 유저용 기능

### 2.2.1. 회원가입

- `POST /users/sign-up` 형태
- email 형태의 아이디, 패스워드를 전달받아 회원가입
- email 은 UK 이므로 이미 존재하는 회원이라면 "이미 가입되어있는 계정입니다." 라는 메시지로 응답 (409)
- 패스워드는 bcrypt 형태의 단방향 암호화 사용
- 암호화에 사용하는 pepper 키는 N 개를 롤링하여 사용하며, 사용한 키 인덱스를 users.password_key_index 에 저장
  - 키 목록은 `app.password.peppers` (콤마 구분, 환경변수 `APP_PASSWORD_PEPPERS`) 에서 읽음
  - 롤링 시 목록 끝에 새 키를 추가하면 신규 가입자부터 새 키를 사용함
- name 은 email 의 로컬파트(@ 앞부분)로 자동 설정
- 가입 직후 status 는 `PENDING` 으로 등록되며, `ACTIVE` 전환은 별도(관리자) 처리 필요
- 회원가입이 정상적으로되었다면 200 OK 응답 (응답 body 의 data 는 없음)

### 2.2.2. 로그인

- `POST /users/login` 형태
- email, password 값을 전달받아 해당 유저가 정상적이라면 액세스토큰, 리프레쉬토큰 발급
- 액세스토큰은 JWT 이며 액세스토큰안에 포함할 sub 값은 users 테이블의 id 값을 사용
- 액세스토큰의 만료 기간은 1일(`app.jwt.access-token-expiry-seconds`), 리프레쉬토큰의 만료기간은 3개월
- 리프레쉬토큰은 별도의 테이블(refresh_tokens)에 저장하며 uuid 에서 하이픈을 제거한값을 리프레쉬토큰으로사용
- 리프레쉬토큰 저장테이블의 UK 는 refresh_token 임
- 비밀번호 검증을 먼저 수행한 뒤 계정 상태를 검증함 (비밀번호를 모르는 사용자에게 계정 상태를 노출하지 않기 위함)
  - `PENDING` → `ACCOUNT_PENDING`(403), 그 외 비활성 → `ACCOUNT_INACTIVE`(401)
  - 자격 증명 불일치는 이메일/비밀번호를 구분하지 않고 `INVALID_CREDENTIALS`(401)

### 2.2.3. 유저 디바이스 목록 조회

- `GET /users/me/devices` 형태
- (Authorization) 액세스토큰이 정상적이라면 API 호출가능
  - `@JwtAuth` 어노테이션이 붙은 메소드만 `JwtAuthInterceptor` 에서 검증
- 유저 소유의 devices 에 조회되는 디바이스목록을 조회
- 응답시 id, name, location_name, type, status, created_at 정보를 응답

### 2.2.4. 유저의 특정 디바이스의 온도 조회

- `GET /users/me/devices/{device_id}/latest-measurement` 형태
- (Authorization) 액세스토큰이 정상적이라면 API 호출가능 (`@JwtAuth`)
- device 의 id 를 전달받으면 해당 device_id 의 온도 조회 (device_measurements 에서 조회)
  - 본인 소유 디바이스가 아니면 403
- 응답시 device_id, temperature, humidity, measured_at 응답
- 조회시 measured_at 내림차순정렬으로 첫번째 1건만 조회 (즉 가장 최신 데이터 1건)

### 2.2.5. 액세스토큰 재발급

- `POST /users/refresh` 형태, **인증 불필요** (리프레쉬 토큰을 body 로 전달)
- 전달받은 refresh_token 이 유효하면 액세스토큰 + 리프레쉬토큰을 재발급
- 만료된 토큰은 저장소에서 삭제 후 `REFRESH_TOKEN_EXPIRED`(401) 응답, 없는 토큰은 `INVALID_REFRESH_TOKEN`(401)
- 리프레쉬 토큰은 **회전(rotation)** 방식. 사용된 토큰은 폐기하고 새 토큰을 발급하여 재사용을 차단
- 계정 상태 검증은 로그인과 동일

## 2.3. 외부 메신저 봇 에서 호출 기능

> 두 API 모두 Authorization 헤더에 **api key** 를 Bearer 로 전달해야 함 (2.1.2 와 동일한 검증)

### 2.3.1. 외부에서의 현재 온도, 습도 조회 API

- `GET /climate/{device_key}/current` 형태
- Discord, Telegram 등을 통해 현재 온도, 습도 조회
- device_key 기준 최신 측정값 1건을 조회해서 응답 (device_key, temperature_c, humidity, measured_at)
- 측정 데이터가 없으면 404

### 2.3.2. 외부에서의 1시간 기준 온도, 습도 변동 데이터 조회 API

- `GET /climate/{device_key}/last-hour` 형태
- Discord, Telegram 등을 통해 온도, 습도 변동 데이터 조회
- 1시간을 10분 단위로 나눠 6개의 데이터 변화치를 응답
  - 구간은 설정값으로 조절 (`app.climate.history.window-seconds` 3600, `app.climate.history.bucket-seconds` 600)
  - window 가 bucket 으로 나누어 떨어지지 않으면 500 처리
- 평균이나 합이 아닌 6개의 데이터 변화를 차례대로 응답
  - 각 구간의 **마지막 측정값**을 대표값으로 사용하며, 데이터가 없는 구간은 값이 null 인 채로 응답
- 응답 항목: from, to, measured_at, temperature_c, humidity

# 3. 관리자용 API 요구 사항

## 3.1. 관리자용 디바이스관련 기능

### 3.1.1. 디바이스 및 api key 생성 API

- 관리자 용 API 임
- `POST /admin/devices` 형태, `@AdminAuth` (관리자 JWT 액세스토큰 필요)
- 관리자로부터 email, device_name 을 입력받음 (location_name, type 은 optional)
  - email 로 유저를 조회하며 없으면 404
  - device_name 미입력 시 `TEST_DEVICE` 로 생성
- Optional 한 값으로 device_key 를 전달받아 사용하고, 입력받지 않으면 디바이스 아이디는 UUID 로 생성하되 하이픈은 제거해서 사용
- api key 는 디바이스 단위로 1개 유지하며, 신규 발급 시 raw key 를 1회성으로 응답
- 편의성을 위해서 key_prefix 는 `gck_` 로 고정
- 응답으로 디바이스 목록(`devices`)과 `api_key` 응답
- 성공 시 201 Created

## 3.2. 관리자용 유저 관련 기능

### 3.2.1. 단일 유저 정보 및 api key 조회 API

- 관리자용 API 임
- `GET /admin/users?user_id=&email=` 형태, `@AdminAuth`
- 관리자로부터 user_id or email 정보를 전달받음 (둘중에 하나는 전달값이 존재해야함)
- 파라미터 존재 유무의 유효성 체크는 `@ValidAdminUserLookup` ConstraintValidator 로 체크. 이메일일 경우 이메일형태인지 정규식으로 판별
- 유효성체크
  - 전달받은 값으로 유저정보가 없다면 404 응답
  - 디바이스 가 없다면 유저정보만 채워서 응답 (정상응답)
- 유효성체크가 정상일경우 유저정보와 디바이스 목록을 응답하고 api_key_hash 를 포함한 `api_keys` 목록도 응답
  - api key 는 디바이스당 1개이므로 유저는 디바이스 수만큼 키를 가질 수 있음
- Querydsl 도입하여 조회하도록함 (`AdminUserLookupRepository`, 차후 확장 API 개발시 참고 샘플용)

## 3.3. 관리자 계정 및 인증 관련 기능

- 관리자 인증은 유저 인증과 완전히 분리함
  - 유저용 JWT secret(`app.jwt.secret`) 과 관리자용 JWT secret(`app.admin-jwt.secret`) 을 반드시 다른 값으로 사용하여, 유저 액세스토큰이 관리자 토큰으로 혼용되는 것을 차단
  - 관리자 액세스토큰 만료기간은 1시간 (유저보다 짧게, `app.admin-jwt.access-token-expiry-seconds`)
- 관리자 보호 API(3.1, 3.2)는 `Authorization` 헤더의 Bearer 토큰(관리자 JWT)으로 인증함
  - 유저용 `@JwtAuth` 와 대칭되는 `@AdminAuth` 어노테이션이 붙은 메소드에 한해 인터셉터에서 관리자 JWT 를 검증
  - 검증 시 `admin_users` 테이블의 상태(status)를 재확인하여, 계정 비활성화 시 토큰을 즉시 무효화할 수 있도록 함

### 3.3.1. 관리자 계정 생성 API

- 최초 관리자 계정을 만들기 위한 부트스트랩 API 임
- `POST /admin/admins` 형태
- `X-Admin-Token` 헤더로 전달한 고정 토큰(`app.admin-token`, 환경변수 `APP_ADMIN_TOKEN`)이 일치할 때만 동작
  - 이 고정 토큰은 오직 관리자 계정 생성에만 사용하며, 다른 관리자 API 에서는 사용하지 않음
- 관리자로부터 email, password, (optional) role 을 전달받음
  - role 미지정 시 `ADMIN`, 허용값은 `ADMIN` / `SUPER_ADMIN`
- email 은 UK 이므로 이미 존재하는 관리자라면 "이미 존재하는 관리자 계정입니다." 라는 메시지로 응답
- 패스워드는 유저와 동일하게 bcrypt + pepper 롤링 방식으로 단방향 암호화하여 저장 (`admin_users` 테이블, password / password_key_index 컬럼)
- 생성 성공 시 id, email, role, status, created_at 정보를 응답 (password 는 응답하지 않음), 201 Created

### 3.3.2. 관리자 로그인 API

- `POST /admin/login` 형태
- email, password 값을 전달받아 정상적인 활성 관리자라면 관리자 JWT 액세스토큰 발급
- 액세스토큰의 sub 값은 `admin_users` 테이블의 id 값, role claim 에 관리자 role 을 포함
- 응답은 `access_token` 만 내려주며 리프레쉬 토큰은 발급하지 않음

# 4. 스케줄링 요구사항

- `@EnableScheduling` 사용
- 매일 01:00 (Asia/Seoul) 에 스케줄링 작업으로 7일이 초과된 device_measurements 데이터는 삭제
  (`ClimateService.deleteOldMeasurements`)
- 클레임 코드 만료 정리는 별도 스케줄러 없이 Caffeine TTL 로 자동 처리

# 5. 공통

- 서버의 응답이 클라이언트가 그대로 받아볼수있는 구조가 아니라 비지니스 에러 응답의 경우 별도로 정의해서 응답할수있도록 정의
  - 성공 응답: `{"code": "OK", "data": ...}`
  - 에러 응답: `{"code": "<에러코드>", "message": "..."}`
- 비즈니스 에러는 `ErrorCode` enum + `BusinessException` 으로 정의하며 enum 이름이 그대로 응답 code 로 내려감
  - 정의된 코드: `INVALID_CREDENTIALS`, `ACCOUNT_PENDING`, `ACCOUNT_INACTIVE`, `TOKEN_EXPIRED`, `INVALID_TOKEN`,
    `REFRESH_TOKEN_EXPIRED`, `INVALID_REFRESH_TOKEN`, `INVALID_CLAIM_CODE`
  - 그 외 `ResponseStatusException` 은 HTTP 상태명(`BAD_REQUEST` 등)을 code 로 사용
  - 처리되지 않은 예외는 상세 원인을 서버 로그로만 남기고 클라이언트에는 일반화된 메시지만 응답
- CORS 는 `app.cors.allowed-origins`(콤마 구분) 로 제어
- 통신 확인용 `GET /ping` API 제공 (인증 불필요, pong + 서버 시각 응답)
- 모든 요청/응답은 `HttpLoggingFilter` 에서 액세스 로그로 기록
  - `Authorization`, `Cookie`, `X-Admin-Token` 헤더와 body 의 api_key / api_key_hash / claim_code / token / password 필드는 마스킹
  - body 는 4096자까지만 기록
