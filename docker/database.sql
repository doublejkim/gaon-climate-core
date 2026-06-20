CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 ID',
                       email VARCHAR(255) NOT NULL UNIQUE COMMENT '사용자 이메일',
                       name VARCHAR(100) NOT NULL COMMENT '사용자 이름',
                       password VARCHAR(60) NULL COMMENT '비밀번호 (bcrypt)',
                       password_key_index INT NOT NULL DEFAULT 0 COMMENT '비밀번호 pepper 키 인덱스',
                       status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '사용자 상태 (PENDING: 가입 직후/로그인 불가, ACTIVE: 활성/로그인 가능, INACTIVE: 비활성/로그인 불가)',
                       created_at DATETIME(6) NOT NULL COMMENT '생성 일시',
                       updated_at DATETIME(6) NOT NULL COMMENT '수정 일시'
) COMMENT = '사용자';

CREATE TABLE admin_users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '관리자 ID',
                       email VARCHAR(255) NOT NULL UNIQUE COMMENT '관리자 이메일',
                       password VARCHAR(60) NOT NULL COMMENT '비밀번호 (bcrypt)',
                       password_key_index INT NOT NULL DEFAULT 0 COMMENT '비밀번호 pepper 키 인덱스',
                       role VARCHAR(30) NOT NULL DEFAULT 'ADMIN' COMMENT '관리자 역할',
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '관리자 상태',
                       created_at DATETIME(6) NOT NULL COMMENT '생성 일시',
                       updated_at DATETIME(6) NOT NULL COMMENT '수정 일시'
) COMMENT = '관리자';

CREATE TABLE devices (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '디바이스 ID',
                         user_id BIGINT NOT NULL COMMENT '사용자 ID',
                         device_key VARCHAR(100) NOT NULL UNIQUE COMMENT '디바이스 키',
                         name VARCHAR(100) NOT NULL COMMENT '디바이스 이름',
                         location_name VARCHAR(100) NULL COMMENT '설치 위치 이름',
                         type VARCHAR(30) NOT NULL DEFAULT 'TEMP_HUMIDITY' COMMENT '디바이스 타입 (TEMP_HUMIDITY, MIC)',
                         status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '디바이스 상태',
                         last_seen_at DATETIME(6) NULL COMMENT '마지막 통신 일시',
                         created_at DATETIME(6) NOT NULL COMMENT '생성 일시',
                         updated_at DATETIME(6) NOT NULL COMMENT '수정 일시',

                         CONSTRAINT fk_devices_01
                             FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT = '디바이스';

CREATE TABLE user_api_keys (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '사용자 API 키 ID',
                               user_id BIGINT NOT NULL COMMENT '사용자 ID (소유자, devices.user_id 와 동일)',
                               device_id BIGINT NOT NULL COMMENT '디바이스 ID (이 키가 인증하는 디바이스)',
                               api_key_hash VARCHAR(255) NOT NULL UNIQUE COMMENT 'API 키 해시 (sha256(raw key))',
                               key_prefix VARCHAR(20) NOT NULL COMMENT 'API 키 접두사',
                               name VARCHAR(100) NULL COMMENT 'API 키 이름',
                               status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' COMMENT 'API 키 상태',
                               last_used_at DATETIME(6) NULL COMMENT '마지막 사용 일시',
                               expires_at DATETIME(6) NULL COMMENT '만료 일시',
                               created_at DATETIME(6) NOT NULL COMMENT '생성 일시',
                               updated_at DATETIME(6) NOT NULL COMMENT '수정 일시',

                               CONSTRAINT fk_user_api_keys_user
                                   FOREIGN KEY (user_id) REFERENCES users(id),

                               CONSTRAINT fk_user_api_keys_device
                                   FOREIGN KEY (device_id) REFERENCES devices(id),

                               CONSTRAINT uk_user_api_keys_device
                                   UNIQUE (device_id)
) COMMENT = '사용자 API 키 (디바이스당 1개)';

CREATE TABLE device_measurements (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '디바이스 측정값 ID',
                                     device_id BIGINT NOT NULL COMMENT '디바이스 ID',
                                     temperature DECIMAL(5,2) NOT NULL COMMENT '온도',
                                     humidity DECIMAL(5,2) NULL COMMENT '습도',
                                     measured_at DATETIME(6) NOT NULL COMMENT '측정 일시',
                                     created_at DATETIME(6) NOT NULL COMMENT '생성 일시',

                                     CONSTRAINT fk_device_measurements_01
                                         FOREIGN KEY (device_id) REFERENCES devices(id)
) COMMENT = '디바이스 측정값';

CREATE INDEX idx_device_measurements_device_01
    ON device_measurements (device_id, measured_at DESC);

CREATE TABLE refresh_tokens (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '리프레쉬 토큰 ID',
                                user_id BIGINT NOT NULL COMMENT '사용자 ID',
                                refresh_token VARCHAR(32) NOT NULL COMMENT '리프레쉬 토큰 (UUID 하이픈 제거)',
                                expires_at DATETIME(6) NOT NULL COMMENT '만료 일시',
                                created_at DATETIME(6) NOT NULL COMMENT '생성 일시',

                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id) REFERENCES users(id),

                                CONSTRAINT uk_refresh_tokens_token
                                    UNIQUE (refresh_token)
) COMMENT = '리프레쉬 토큰';

CREATE TABLE device_claim_codes (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '디바이스 클레임 코드 ID',
                                    user_id BIGINT NOT NULL COMMENT '사용자 ID',
                                    code VARCHAR(32) NOT NULL COMMENT '클레임 코드 (대문자, 예: GAON-XXXX-XXXX)',
                                    expires_at DATETIME(6) NOT NULL COMMENT '만료 일시',
                                    used_at DATETIME(6) NULL COMMENT '사용 완료 일시 (NULL 이면 미사용)',
                                    created_at DATETIME(6) NOT NULL COMMENT '생성 일시',

                                    CONSTRAINT fk_device_claim_codes_user
                                        FOREIGN KEY (user_id) REFERENCES users(id),

                                    CONSTRAINT uk_device_claim_codes_code
                                        UNIQUE (code)
) COMMENT = '디바이스 클레임 코드';


-- Test 용 초기 데이터
INSERT INTO users (email, name, status, created_at, updated_at)
VALUES ('test@email.com', '홍길동', 'ACTIVE', now(), now());
