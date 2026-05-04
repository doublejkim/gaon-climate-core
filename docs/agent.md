# 1. 프로그램 개요

- 라즈베리 클라이언트로부터 온도, 습도 정보를 저장받아 관리하는 서버


# 2. 서비스 API 요구 사항

# 2.2. 디바이스 등록 및 api key 생성 API

- 디바이스에서 호출하는 API 임 
- 디바이스로부터 전송되는 device_key 기준으로 디바이스를 생성
- api key 를 생성해서 특정 prefix 를 붙여서 해싱한 값을 api_key_hash 에 저장함.
- api key 는 유저별로 등록되는것이기때문에 해당 유저에 매핑되는 api key 가 존재한다면 이미 존재하는 값을 응답함 
- Authorization 헤더의 Bearer 토큰으로 유저를 식별. 존재하지 않는 사용자라면 실패처리
- 디바이스 등록 및 api key 생성이 성공했다면 생성된 api_key_hash 값을 전송함

# 2.3. 온도 습도 저장 API

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
  - Authorization 헤더의 Bearer 토큰이 유효하다면 동작 수행
  - device_key 가 유효하다면 동작 수행
- 모두 유효성이 정상이라면 device_measurements 테이블에 데이터 저장 

## 2.4. 외부에서의 현재 온도, 습도 조회 API

- Discord, Telegram 등을 통해 현재 온도, 습도 조회
- 현재 온도, 습도를 조회해서 응답

## 2.5. 외부에서의 1시간 기준 온도, 습도 변동 데이터 조회 API

- Discord, Telegram 등을 통해 온도, 습도 변동 데이터 조회
- 10분정도로 나눠서 6개의 데이터 변화치를 응답 
- 평균이나 합이 아닌 6개의 데이터 변화를 차례대로 응답 필요

# 3. 관리자용 API 요구 사항

# 3.1. 디바이스 및 api key 생성 API

- 관리자 용 API 임
- 관리자로부터 email, (device) name 만 입력받음
- Optional 한 값으로 device_key 를 전달받아 사용하고, 입력받지 않으면 디바이스 아이디는 UUID 로 생성하되 하이픈은 제거해서 사용
- api key 를 생성해서 특정 prefix 를 붙여서 해싱한 값을 api_key_hash 에 저장함.
- - api key 는 유저별로 등록되는것이기때문에 해당 유저에 매핑되는 api key 가 존재한다면 이미 존재하는 값을 응답함
- 편의 성을 위해서 key_prefix 는 임의로 고정
- Authorization 헤더 체크를 하지 않는 API 임. 다만 별도의 체크는 필요해서 간단한방법으로 가이드할것
- 응답으로 디바이스 정보와 api_key_hash 응

# 3.2. 단일 유저 정보 및 api key 조회 API

- 관리자용 API 임
- 관리자로부터 user_id or email 정보를 전달받음 (둘중에 하나는 전달값이 존재해야함)
- 파라미터 존재 유무의 유효성 체크는 ConstraintValidator 사용하여 체크. 이메일일 경우 이메일형태인지 정규식으로 판별하는 로직 추가.
- 유효성체크
  - 전달받은 값으로 유저정보가 없다면 4xx 응답 
  - 디바이스 가 없다면 유저정보만 채워서 응답. (정상응답
- 유효성체크가 정상일경우 유저정보와 디바이스 정보를 응답하고 api_key_hash 값도 응답 
- Querydsl 도입하여 조회하도록함 (차후 확장 API 개발시 참고 샘플용)

# 4. 스케줄링 요구사항 

- 매일 01:00 에 스케줄링 작업으로 7일이 초과된 device_measurements 데이터는 삭제 
