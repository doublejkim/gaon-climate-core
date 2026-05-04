# Local Development MariaDB

MariaDB 10.11 local development database for `gaon-climate-core`.

This Compose file runs only the database. Use `docker/compose.yaml` to run the full application stack.

## Start

```bash
docker compose -f docker/compose.local-db.yaml up -d
```

If your Docker installation uses Compose v1:

```bash
docker-compose -f docker/compose.local-db.yaml up -d
```

## Stop

```bash
docker compose -f docker/compose.local-db.yaml down
```

## Reset Data

```bash
docker compose -f docker/compose.local-db.yaml down -v
```

If your Docker installation uses Compose v1:

```bash
docker-compose -f docker/compose.local-db.yaml down -v
```

## Default Connection

- Host: `localhost`
- Port: `3306`
- Database: `gaon_climate`
- Username: `testuser`
- Password: `testpwd`
- Root password: `rootpassword`

Spring Boot uses the same defaults in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/gaon_climate
    username: testuser
    password: testpwd
```

To override values, copy `docker/local-db.env.example` to `docker/.env.local-db` and edit it, then run:

```bash
docker compose --env-file docker/.env.local-db -f docker/compose.local-db.yaml up -d
```

`docker/database.sql` runs only when the database volume is first created.

## Fix Login Mismatch

MariaDB only applies `MARIADB_USER` and `MARIADB_PASSWORD` when the data volume is first created. If the container was created with different credentials, reset the development volume:

```bash
docker-compose -f docker/compose.local-db.yaml down -v
docker-compose -f docker/compose.local-db.yaml up -d
```
