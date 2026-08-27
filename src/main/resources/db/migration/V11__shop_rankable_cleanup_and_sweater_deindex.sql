-- De-index knitwear that slipped through (cardigans, sweaters, pullovers).
UPDATE listing
SET is_print_tee = 0,
    print_tee_score = 0,
    first_seen_in_top_at = NULL,
    last_score = NULL,
    last_scored_at = NULL
WHERE is_print_tee = 1
  AND (
    LOWER(title) LIKE '%sweater%'
    OR LOWER(title) LIKE '%cardigan%'
    OR LOWER(title) LIKE '%pullover%'
  );

DELETE FROM user_favorite
WHERE listing_id IN (
    SELECT listing_id
    FROM listing
    WHERE is_print_tee = 0
      AND (
        LOWER(title) LIKE '%sweater%'
        OR LOWER(title) LIKE '%cardigan%'
        OR LOWER(title) LIKE '%pullover%'
      )
);

-- Shop expansion used shop catalog position as Etsy top-N. Clear momentum for shop-only listings.
UPDATE listing
SET first_seen_in_top_at = NULL,
    last_score = NULL,
    last_scored_at = NULL
WHERE is_print_tee = 1
  AND first_seen_in_top_at IS NOT NULL
  AND EXISTS (
    SELECT 1 FROM listing_query q WHERE q.listing_id = listing.listing_id AND q.query LIKE 'shop:%'
  )
  AND NOT EXISTS (
    SELECT 1
    FROM listing_query q
    WHERE q.listing_id = listing.listing_id
      AND (q.query LIKE 'taxonomy:%' OR q.query LIKE 'benchmark:%')
  );

-- Restore first_seen_in_top_at from real Etsy search hits (taxonomy + benchmark sweeps).
UPDATE listing
SET first_seen_in_top_at = (
    SELECT MIN(q.observed_at)
    FROM listing_query q
    WHERE q.listing_id = listing.listing_id
      AND (q.query LIKE 'taxonomy:%' OR q.query LIKE 'benchmark:%')
      AND q.position <= 100
)
WHERE is_print_tee = 1
  AND EXISTS (
    SELECT 1
    FROM listing_query q
    WHERE q.listing_id = listing.listing_id
      AND (q.query LIKE 'taxonomy:%' OR q.query LIKE 'benchmark:%')
      AND q.position <= 100
  );

-- Recompute momentum from ListingRanker formula (velocity + recency + favorers tie-break).
UPDATE listing
SET last_score = (
        (1 / GREATEST(
            TIMESTAMPDIFF(SECOND, COALESCE(original_created_at, etsy_created_at), first_seen_in_top_at) / 86400.0,
            0.5))
        + (1 / GREATEST(
            TIMESTAMPDIFF(SECOND, first_seen_in_top_at, CURRENT_TIMESTAMP) / 86400.0,
            0.5))
        + (GREATEST(COALESCE(delta_favorers_7d, 0), 0) * 0.000000001)
    ),
    last_scored_at = CURRENT_TIMESTAMP
WHERE is_print_tee = 1
  AND first_seen_in_top_at IS NOT NULL
  AND COALESCE(original_created_at, etsy_created_at) IS NOT NULL;
