# QNAP Docker Deployment

이 문서는 public Git repo에서 소스를 pull 받아 QNAP NAS의 Docker로 전체 프로젝트를 실행하는 기준입니다.

## Compose 파일 구분

- `docker/compose.yaml`: 앱 컨테이너 실행용 Compose 파일입니다. MariaDB는 별도로 운영한다고 가정합니다.
- `docker/compose.local-db.yaml`: 로컬 개발 중 MariaDB만 Docker로 띄우는 테스트용 Compose 파일입니다.

## QNAP 최초 실행

```bash
cd /share/Container
git clone https://github.com/{your-id}/gaon-climate-core.git
cd gaon-climate-core
cp docker/full.env.example docker/.env
vi docker/.env
docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

Compose v1 환경이면 `docker compose` 대신 `docker-compose`를 사용합니다.

`.env`에는 실제 운영 값을 넣고 Git에 commit하지 않습니다.

필수로 바꿀 값:

```env
APP_ADMIN_TOKEN=change-this-admin-token
SPRING_DATASOURCE_URL=jdbc:mariadb://your-mariadb-host:3306/gaon_climate
SPRING_DATASOURCE_USERNAME=gaon
SPRING_DATASOURCE_PASSWORD=change-db-password
```

별도 운영 중인 MariaDB에는 `docker/database.sql`의 스키마를 미리 적용해야 합니다.

## 업데이트

```bash
cd /share/Container/gaon-climate-core
git pull
docker compose --env-file docker/.env -f docker/compose.yaml up -d --build
```

## 로그 확인

컨테이너 표준 로그:

```bash
docker logs -f gaon-climate-core
```

애플리케이션 파일 로그는 Docker volume `gaon-climate_app_logs`에 저장됩니다.
`local`, `dev` 프로파일에서는 1일 단위 파일명으로 기록되고 7일간 보관됩니다.
`prod` 프로파일에서는 1시간 단위 파일명으로 기록되고 7일간 보관됩니다.

## 중지

```bash
docker compose --env-file docker/.env -f docker/compose.yaml down
```

로그 volume까지 삭제하려면 volume을 함께 제거합니다. 운영 환경에서는 신중하게 실행해야 합니다.

```bash
docker compose --env-file docker/.env -f docker/compose.yaml down -v
```

## 로컬 개발 DB만 실행

앱은 IntelliJ에서 실행하고 DB만 Docker로 띄울 때 사용합니다.

```bash
cp docker/local-db.env.example docker/.env.local-db
docker compose --env-file docker/.env.local-db -f docker/compose.local-db.yaml up -d
```
