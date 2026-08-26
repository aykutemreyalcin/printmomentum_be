CREATE TABLE shop_crawl_queue (
    shop_id BIGINT NOT NULL,
    enqueued_at TIMESTAMP NOT NULL,
    last_crawled_at TIMESTAMP NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    PRIMARY KEY (shop_id),
    CONSTRAINT fk_scq_shop FOREIGN KEY (shop_id) REFERENCES shop (shop_id)
);

CREATE INDEX idx_scq_status_enqueued ON shop_crawl_queue (status, enqueued_at);
