ALTER TABLE shop ADD icon_url VARCHAR(2048) NULL;
ALTER TABLE shop ADD transaction_sold_count INT NULL;
ALTER TABLE shop ADD listing_active_count INT NULL;
ALTER TABLE shop ADD review_count INT NULL;
ALTER TABLE shop ADD review_average DECIMAL(3, 2) NULL;
ALTER TABLE shop ADD etsy_created_at TIMESTAMP NULL;
ALTER TABLE shop ADD last_refreshed_at TIMESTAMP NULL;

ALTER TABLE listing ADD views INT NULL;
ALTER TABLE listing ADD quantity INT NULL;
ALTER TABLE listing ADD original_created_at TIMESTAMP NULL;
ALTER TABLE listing ADD last_review_at TIMESTAMP NULL;
ALTER TABLE listing ADD reviews_30d INT NULL;
ALTER TABLE listing ADD who_made VARCHAR(32) NULL;
ALTER TABLE listing ADD when_made VARCHAR(32) NULL;
ALTER TABLE listing ADD etsy_bestseller TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE listing ADD etsy_bestseller_since TIMESTAMP NULL;
ALTER TABLE listing ADD etsy_bestseller_ended_at TIMESTAMP NULL;
ALTER TABLE listing ADD pm_bestseller TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE listing ADD pm_bestseller_since TIMESTAMP NULL;
ALTER TABLE listing ADD pm_bestseller_ended_at TIMESTAMP NULL;

ALTER TABLE listing_snapshot ADD views INT NULL;
ALTER TABLE listing_snapshot ADD quantity INT NULL;
ALTER TABLE listing_snapshot ADD review_count INT NULL;

CREATE TABLE listing_query (
    listing_id BIGINT NOT NULL,
    query VARCHAR(191) NOT NULL,
    observed_day DATE NOT NULL,
    position INT NOT NULL,
    crawl_run_id VARCHAR(36) NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (listing_id, query, observed_day),
    CONSTRAINT fk_listing_query_listing FOREIGN KEY (listing_id) REFERENCES listing (listing_id)
);

CREATE INDEX idx_listing_query_query ON listing_query (query);

CREATE TABLE listing_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    kind VARCHAR(32) NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_listing_event_listing FOREIGN KEY (listing_id) REFERENCES listing (listing_id)
);

CREATE INDEX idx_listing_event_listing_at ON listing_event (listing_id, observed_at);

CREATE TABLE query_stats (
    query VARCHAR(191) NOT NULL,
    observed_day DATE NOT NULL,
    listing_count INT NOT NULL,
    median_price DECIMAL(12, 2) NULL,
    median_favorers INT NULL,
    median_views INT NULL,
    PRIMARY KEY (query, observed_day)
);

CREATE TABLE user_favorite (
    user_id INT NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, listing_id),
    CONSTRAINT fk_user_favorite_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_favorite_listing FOREIGN KEY (listing_id) REFERENCES listing (listing_id)
);

CREATE INDEX idx_listing_etsy_bestseller ON listing (etsy_bestseller, etsy_bestseller_since);
CREATE INDEX idx_listing_original_created_at ON listing (original_created_at);
CREATE INDEX idx_listing_last_review_at ON listing (last_review_at);
