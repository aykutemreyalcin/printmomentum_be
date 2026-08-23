ALTER TABLE listing
    ADD last_score DECIMAL(18, 9) NULL;

ALTER TABLE listing
    ADD last_scored_at TIMESTAMP NULL;
