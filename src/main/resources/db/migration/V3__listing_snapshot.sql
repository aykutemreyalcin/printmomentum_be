ALTER TABLE listing
    ADD first_seen_in_top_at TIMESTAMP NULL;

CREATE TABLE listing_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    crawl_run_id VARCHAR(36) NOT NULL,
    observed_at TIMESTAMP NOT NULL,
    position INT NOT NULL,
    num_favorers INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_listing_snapshot_listing FOREIGN KEY (listing_id) REFERENCES listing (listing_id)
);

CREATE INDEX idx_listing_snapshot_listing_id ON listing_snapshot (listing_id);
CREATE INDEX idx_listing_snapshot_crawl_run_id ON listing_snapshot (crawl_run_id);
