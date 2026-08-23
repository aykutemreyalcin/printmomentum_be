CREATE TABLE app_meta (
    id BIGINT NOT NULL AUTO_INCREMENT,
    app_name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO app_meta (app_name) VALUES ('printmomentum');
