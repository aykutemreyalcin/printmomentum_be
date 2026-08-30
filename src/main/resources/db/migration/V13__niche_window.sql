CREATE TABLE niche_term (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(120) NOT NULL,
    label VARCHAR(191) NOT NULL,
    listing_count INT NOT NULL DEFAULT 0,
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    etsy_count INT NULL,
    etsy_checked_at TIMESTAMP NULL,
    window_state VARCHAR(16) NOT NULL DEFAULT 'LOW_DATA',
    new_entrants_14d INT NOT NULL DEFAULT 0,
    clone_density_7d DECIMAL(5, 4) NOT NULL DEFAULT 0,
    break_in_rate DECIMAL(5, 4) NOT NULL DEFAULT 0,
    incumbent_age_days DECIMAL(8, 2) NULL,
    entrant_momentum DECIMAL(10, 6) NULL,
    window_computed_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_niche_term_slug UNIQUE (slug),
    CONSTRAINT uk_niche_term_label UNIQUE (label)
);

CREATE TABLE listing_niche_term (
    listing_id BIGINT NOT NULL,
    niche_term_id BIGINT NOT NULL,
    weight DECIMAL(4, 3) NOT NULL,
    source VARCHAR(16) NOT NULL,
    PRIMARY KEY (listing_id, niche_term_id),
    CONSTRAINT fk_listing_niche_term_listing FOREIGN KEY (listing_id) REFERENCES listing (listing_id),
    CONSTRAINT fk_listing_niche_term_term FOREIGN KEY (niche_term_id) REFERENCES niche_term (id)
);

CREATE INDEX idx_listing_niche_term_term ON listing_niche_term (niche_term_id);

CREATE TABLE niche_window_snapshot (
    niche_term_id BIGINT NOT NULL,
    observed_day DATE NOT NULL,
    window_state VARCHAR(16) NOT NULL,
    listing_count INT NOT NULL,
    new_entrants_14d INT NOT NULL,
    clone_density_7d DECIMAL(5, 4) NOT NULL,
    break_in_rate DECIMAL(5, 4) NOT NULL,
    incumbent_age_days DECIMAL(8, 2) NULL,
    entrant_momentum DECIMAL(10, 6) NULL,
    etsy_count INT NULL,
    PRIMARY KEY (niche_term_id, observed_day),
    CONSTRAINT fk_niche_window_snapshot_term FOREIGN KEY (niche_term_id) REFERENCES niche_term (id)
);
