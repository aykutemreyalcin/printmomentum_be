CREATE TABLE shop (
    shop_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    PRIMARY KEY (shop_id)
);

CREATE TABLE listing (
    listing_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    url VARCHAR(2048) NOT NULL,
    taxonomy_id BIGINT,
    price_amount DECIMAL(12, 2),
    currency VARCHAR(3),
    tags TEXT,
    num_favorers INT NOT NULL DEFAULT 0,
    etsy_created_at TIMESTAMP NULL,
    etsy_updated_at TIMESTAMP NULL,
    print_tee_score DECIMAL(4, 3),
    is_print_tee TINYINT(1) NOT NULL DEFAULT 0,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (listing_id),
    CONSTRAINT fk_listing_shop FOREIGN KEY (shop_id) REFERENCES shop (shop_id)
);

CREATE INDEX idx_listing_is_print_tee ON listing (is_print_tee);
CREATE INDEX idx_listing_last_seen_at ON listing (last_seen_at);

CREATE TABLE listing_image (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    url VARCHAR(2048) NOT NULL,
    `rank` INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_listing_image_listing FOREIGN KEY (listing_id) REFERENCES listing (listing_id)
);
