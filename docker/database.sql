CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       name VARCHAR(100) NOT NULL,
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6) NOT NULL
);

CREATE TABLE user_api_keys (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               api_key_hash VARCHAR(255) NOT NULL UNIQUE,
                               key_prefix VARCHAR(20) NOT NULL,
                               name VARCHAR(100) NULL,
                               status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                               last_used_at DATETIME(6) NULL,
                               expires_at DATETIME(6) NULL,
                               created_at DATETIME(6) NOT NULL,
                               updated_at DATETIME(6) NOT NULL,

                               CONSTRAINT fk_user_api_keys_user
                                   FOREIGN KEY (user_id) REFERENCES users(id),

                               CONSTRAINT uk_user_api_keys_user
                                   UNIQUE (user_id)
);

CREATE TABLE devices (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL,
                         device_key VARCHAR(100) NOT NULL UNIQUE,
                         name VARCHAR(100) NOT NULL,
                         location_name VARCHAR(100) NULL,
                         status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                         last_seen_at DATETIME(6) NULL,
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6) NOT NULL,

                         CONSTRAINT fk_devices_01
                             FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE device_measurements (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     device_id BIGINT NOT NULL,
                                     temperature DECIMAL(5,2) NOT NULL,
                                     humidity DECIMAL(5,2) NULL,
                                     measured_at DATETIME(6) NOT NULL,
                                     created_at DATETIME(6) NOT NULL,

                                     CONSTRAINT fk_device_measurements_01
                                         FOREIGN KEY (device_id) REFERENCES devices(id)
);

CREATE INDEX idx_device_measurements_device_01
    ON device_measurements (device_id, measured_at DESC);


-- Test 용 초기 데이터
INSERT INTO users (email, name, status, created_at, updated_at)
VALUES ('test@email.com', '홍길동', 'ACTIVE', now(), now());
