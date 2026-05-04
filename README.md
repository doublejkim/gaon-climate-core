# gaon-climate-core

라즈베리파이 같은 디바이스에서 전송하는 온도/습도 데이터를 저장하고 조회하는 Spring Boot API 서버입니다.

## 주요 기능

- 유저 기준 디바이스 등록
- 유저별 1:1 API key 발급 및 재사용
- API key 기반 온도/습도 저장
- API key 기반 현재 온도/습도 조회
- 최근 구간별 온도/습도 변화 데이터 조회
- 관리자용 디바이스 생성
- 관리자용 단일 유저, 디바이스, API key 조회
- 오래된 측정 데이터 삭제 스케줄링
- 요청/응답 로깅 및 프로파일별 파일 로그 정책

## 기술 스펙

- Kotlin 2.2.21
- Java 21
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Data JPA
- Querydsl JPA 5.1.0
- MariaDB
- Gradle Kotlin DSL
- Docker / Docker Compose

## API 개요

응답은 공통적으로 `code`, `message`, `data` 형태로 wrapping됩니다.

### 디바이스

- `POST /devices/register`
  - 디바이스 등록 및 API key 응답
  - `Authorization: Bearer {userId 또는 email}` 필요

### 온도/습도

- `POST /climate/{deviceKey}`
  - 온도/습도 저장
  - `Authorization: Bearer {api_key_hash}` 필요

- `GET /climate/{deviceKey}/current`
  - 현재 온도/습도 조회
  - `Authorization: Bearer {api_key_hash}` 필요

- `GET /climate/{deviceKey}/last-hour`
  - 최근 설정 구간 기준 온도/습도 변화 조회
  - `Authorization: Bearer {api_key_hash}` 필요

### 관리자

- `POST /admin/devices`
  - 관리자용 디바이스 생성
  - `X-Admin-Token` 필요

- `GET /admin/users?user_id={id}`
- `GET /admin/users?email={email}`
  - 관리자용 단일 유저/디바이스/API key 조회
  - `X-Admin-Token` 필요

## 설정

주요 환경변수:

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mariadb://your-mariadb-host:3306/gaon_climate
SPRING_DATASOURCE_USERNAME=gaon
SPRING_DATASOURCE_PASSWORD=change-db-password
APP_ADMIN_TOKEN=change-this-admin-token
```

온도/습도 변화 조회 구간:

```env
APP_CLIMATE_HISTORY_WINDOW_SECONDS=3600
APP_CLIMATE_HISTORY_BUCKET_SECONDS=600
```

기본 운영 설정은 60분 범위를 10분 단위로 나눕니다. `local` 프로파일에서는 1분 범위를 10초 단위로 나눕니다.

## Docker 실행

Docker 관련 파일은 `docker/` 디렉토리에 있습니다.

- Docker Compose v2 명령인 `docker compose`를 기준으로 합니다.
- `docker/compose.yaml`: 앱 컨테이너 실행용입니다. MariaDB는 별도 운영을 전제로 합니다.
- `docker/compose.local-db.yaml`: 로컬 개발용 MariaDB만 실행합니다.
- `docker/full.env.example`: 앱 실행용 환경변수 예시입니다.
- `docker/local-db.env.example`: 로컬 DB 실행용 환경변수 예시입니다.

QNAP NAS에서 public Git repo를 pull 받아 실행하는 절차는 [docs/qnap-docker.md](docs/qnap-docker.md)를 기준으로 진행합니다.

Compose v2 설치 확인:

```bash
docker compose version
```

요약:

```bash
cp docker/full.env.example docker/.env
vi docker/.env
docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

### 특정 태그 또는 특정 빌드로 배포

앱 이미지는 Docker build 단계에서 public Git repo를 받아 빌드합니다. 기본 repo와 ref는 다음과 같습니다.

- `REPOSITORY_URL`: `https://github.com/doublejkim/gaon-climate-core.git`
- `GIT_REF`: `main`

특정 버전을 배포하려면 `GIT_REF`에 브랜치명, 태그명, 또는 커밋 SHA를 지정합니다. 배포 서버에서 해당 ref를 public repo에서 접근할 수 있어야 합니다.

필요 조건:

- 배포할 코드가 `https://github.com/doublejkim/gaon-climate-core`에 push되어 있어야 합니다.
- 태그로 배포할 경우 해당 태그가 remote에 push되어 있어야 합니다.
- 커밋 SHA로 배포할 경우 해당 커밋이 remote branch 또는 tag에서 접근 가능한 상태여야 합니다.
- 배포 서버가 Docker build 중 GitHub와 Gradle dependency repository에 접근할 수 있어야 합니다.

태그 배포 예시:

```bash
GIT_REF=v1.0.0 docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

특정 커밋 SHA 배포 예시:

```bash
GIT_REF=abc1234 docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

repo URL을 명시해서 빌드해야 하는 경우:

```bash
REPOSITORY_URL=https://github.com/doublejkim/gaon-climate-core.git \
GIT_REF=v1.0.0 \
docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

별도 운영 중인 MariaDB에는 [docker/database.sql](docker/database.sql)을 먼저 적용해야 합니다.

## 로컬 개발

로컬에서 DB만 Docker로 실행:

```bash
cp docker/local-db.env.example docker/.env.local-db
docker compose --env-file docker/.env.local-db -f docker/compose.local-db.yaml up -d
```

애플리케이션 실행:

```bash
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

## 로그

- `local`: 콘솔 로그 중심, DEBUG 레벨
- `dev`: DEBUG 레벨, 일 단위 파일 로그, 7일 보관
- `prod`: INFO 레벨, 시간 단위 파일 로그, 7일 보관

요청 로그에는 시간, 서버 정보, HTTP method, URL, request header, request param, request body, response 정보가 포함됩니다. 인증/토큰/비밀번호 계열 값은 마스킹됩니다.
