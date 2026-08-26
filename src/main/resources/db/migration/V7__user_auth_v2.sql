CREATE TABLE user_role (
    id INT NOT NULL AUTO_INCREMENT,
    role_value VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_role_value UNIQUE (role_value)
);

CREATE TABLE app_user (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NULL,
    display_name VARCHAR(128) NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role INT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT fk_app_user_role FOREIGN KEY (role) REFERENCES user_role (id)
);

CREATE TABLE user_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    refresh_token_hash VARCHAR(128) NOT NULL,
    device_id VARCHAR(128) NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    revoke_reason VARCHAR(128) NULL,
    last_used_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_session_refresh_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_user_session_user_active ON user_session (user_id, revoked_at, expires_at);

INSERT INTO user_role (role_value) VALUES ('admin'), ('user');

INSERT INTO app_user (name, display_name, email, password, role, active)
SELECT 'Admin', 'Admin', 'admin@printmomentum.local',
       '$2b$10$QoRVX4YwsM.EdODKv9PzQOQpap1f1ZAZZRkfWA14148pMqebhaUli',
       id, 1
FROM user_role WHERE role_value = 'admin';

INSERT INTO app_user (name, display_name, email, password, role, active)
SELECT 'User', 'User', 'user@printmomentum.local',
       '$2b$10$bJZISmP2hQ.CCUZuY9LSMuwipolE8QWt.1fy7spejNpfLwdiku3DK',
       id, 1
FROM user_role WHERE role_value = 'user';
