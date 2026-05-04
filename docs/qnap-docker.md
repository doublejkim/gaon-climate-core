# QNAP Docker Deployment

이 문서는 QNAP NAS에서 Docker Compose v2로 `gaon-climate-core` 앱을 배포하는 기준입니다.

## 배포 구조

서버의 Docker build는 로컬 소스를 이미지에 `COPY`하지 않습니다.

현재 `docker/Dockerfile`은 build stage 안에서 다음 순서로 동작합니다.

1. JDK 이미지에서 `git`, `ca-certificates`를 설치합니다.
2. `REPOSITORY_URL`의 public Git repo를 등록합니다.
3. `GIT_REF`로 지정한 브랜치, 태그, 또는 커밋 SHA를 fetch합니다.
4. fetch한 소스에서 `./gradlew bootJar`를 실행합니다.
5. 생성된 jar만 JRE 런타임 이미지로 복사합니다.

따라서 QNAP 서버에 repo를 clone하는 목적은 앱 소스를 직접 빌드하기 위해서가 아니라, `docker/compose.yaml`, `docker/Dockerfile`, `.env` 예시, DB 스키마 같은 배포 파일을 가져오기 위해서입니다.

## Compose 파일 구분

- `docker/compose.yaml`: 앱 컨테이너 실행용 Compose 파일입니다. MariaDB는 별도로 운영한다고 가정합니다.
- `docker/compose.local-db.yaml`: 로컬 개발 중 MariaDB만 Docker로 띄우는 테스트용 Compose 파일입니다. QNAP 운영 배포에서는 보통 사용하지 않습니다.

## 사전 조건

QNAP 서버에서 다음 항목이 준비되어 있어야 합니다.

- Docker Engine 또는 Container Station 기반 Docker 실행 환경
- Docker Compose v2 명령
- GitHub 접근 가능 네트워크
- Gradle dependency repository 접근 가능 네트워크
- 별도로 운영 중인 MariaDB

Compose v2가 인식되는지 확인합니다.

```bash
docker compose version
```

하이픈 없는 `docker compose` 명령을 사용합니다.

QTS 5.2에는 `git`이 기본 설치되어 있지 않을 수 있습니다. 이 문서는 QNAP OS에 git을 직접 설치하지 않고, Docker 임시 컨테이너 안에서 git을 실행하는 방식을 기본으로 안내합니다.

## 최초 배포

배포 파일을 받을 위치로 이동합니다.

```bash
cd /share/Container
```

QNAP OS에 git이 없다면 Docker 임시 컨테이너로 repo를 clone합니다.

```bash
docker run --rm \
  -v /share/Container:/work \
  -w /work \
  alpine:3.20 \
  sh -c "apk add --no-cache git && git clone https://github.com/doublejkim/gaon-climate-core.git"
```

clone된 배포 디렉토리로 이동합니다.

```bash
cd gaon-climate-core
```

운영 환경변수 파일을 생성합니다.

```bash
cp docker/full.env.example docker/.env
vi docker/.env
```

필수로 바꿀 값:

```env
APP_PORT=8080
SPRING_PROFILES_ACTIVE=prod
APP_ADMIN_TOKEN=change-this-admin-token
SPRING_DATASOURCE_URL=jdbc:mariadb://your-mariadb-host:3306/gaon_climate
SPRING_DATASOURCE_USERNAME=gaon
SPRING_DATASOURCE_PASSWORD=change-db-password
TZ=Asia/Seoul
```

별도 운영 중인 MariaDB에는 최초 실행 전에 `docker/database.sql`의 스키마를 적용합니다. DB 클라이언트가 설치되어 있다면 다음처럼 적용할 수 있습니다.

```bash
mariadb -h your-mariadb-host -u gaon -p gaon_climate < docker/database.sql
```

앱 이미지를 빌드하고 컨테이너를 실행합니다.

```bash
docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

기본값은 다음 public repo의 `main` ref를 빌드합니다.

```env
REPOSITORY_URL=https://github.com/doublejkim/gaon-climate-core.git
GIT_REF=main
```

## 특정 버전 배포

운영 배포는 재현 가능한 태그 또는 커밋 SHA 사용을 권장합니다.

태그 배포:

```bash
GIT_REF=v1.0.0 docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

특정 커밋 배포:

```bash
GIT_REF=0123456789abcdef0123456789abcdef01234567 docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

항상 같은 버전을 쓰려면 `docker/.env`에 직접 추가해도 됩니다.

```env
REPOSITORY_URL=https://github.com/doublejkim/gaon-climate-core.git
GIT_REF=v1.0.0
```

이 경우 실행 명령은 그대로 유지합니다.

```bash
docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

## 업데이트

배포 설정 파일 자체가 바뀐 경우 먼저 서버의 배포 repo를 갱신합니다.

```bash
cd /share/Container/gaon-climate-core
```

QNAP OS에 git이 없다면 Docker 임시 컨테이너로 pull합니다.

```bash
docker run --rm \
  -v /share/Container/gaon-climate-core:/repo \
  -w /repo \
  alpine:3.20 \
  sh -c "apk add --no-cache git && git pull"
```

태그 또는 커밋 SHA를 바꿔 배포하는 경우:

```bash
GIT_REF=v1.0.1 docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

`GIT_REF=main`을 유지하면서 최신 main을 다시 받아 배포하는 경우 Docker 캐시 때문에 fetch 단계가 재사용될 수 있습니다. 이때는 no-cache로 이미지를 다시 빌드한 뒤 컨테이너를 올립니다.

```bash
docker compose --env-file docker/.env -f docker/compose.yaml build --no-cache app
docker compose --env-file docker/.env -f docker/compose.yaml up -d app
```

## 상태 확인

컨테이너 상태:

```bash
docker ps --filter name=gaon-climate-core
```

컨테이너 표준 로그:

```bash
docker logs -f gaon-climate-core
```

애플리케이션 파일 로그는 Docker volume `gaon-climate_app_logs`에 저장됩니다.

- `local`, `dev`: 1일 단위 파일명, 7일 보관
- `prod`: 1시간 단위 파일명, 7일 보관

## 중지

컨테이너를 중지하고 Compose 리소스를 정리합니다.

```bash
docker compose --env-file docker/.env -f docker/compose.yaml down
```

로그 volume까지 삭제하려면 `-v`를 붙입니다. 운영 환경에서는 로그가 삭제되므로 신중하게 실행합니다.

```bash
docker compose --env-file docker/.env -f docker/compose.yaml down -v
```

## QNAP OS에 git을 직접 설치하는 경우

현재 배포 방식에서는 QNAP OS에 git을 직접 설치할 필요가 없습니다. Docker 임시 컨테이너 방식이 QTS 시스템을 덜 건드리므로 기본 권장입니다.

그래도 QNAP SSH 콘솔에서 `git` 명령을 직접 쓰고 싶다면 Entware를 사용할 수 있습니다. Entware는 QNAP 공식 App Center 기본 검색에 보통 나오지 않으며, QPKG를 다운로드해서 App Center에서 수동 설치하는 방식입니다.

대략적인 흐름:

1. Entware QPKG를 다운로드합니다.
2. QNAP 웹 UI에서 App Center를 엽니다.
3. 수동 설치 메뉴에서 QPKG 파일을 선택해 설치합니다.
4. SSH에 다시 접속한 뒤 Entware profile을 로드합니다.
5. `opkg`로 git을 설치합니다.

```bash
source /opt/etc/profile
opkg update
opkg install git
git --version
```

Entware는 QNAP 공식 지원 패키지가 아니므로 운영 NAS에는 신중하게 적용합니다.

## 로컬 개발 DB만 실행

앱은 IDE에서 실행하고 DB만 Docker로 띄울 때 사용합니다.

```bash
cp docker/local-db.env.example docker/.env.local-db
docker compose --env-file docker/.env.local-db -f docker/compose.local-db.yaml up -d
```
