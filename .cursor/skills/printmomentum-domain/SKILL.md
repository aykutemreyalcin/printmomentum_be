---
name: printmomentum-domain
description: PrintMomentum ranking and print-tee classification rules. Use when scoring listings, filtering printable t-shirts, designing snapshot/momentum logic, or discussing bestsellers vs velocity.
---

# PrintMomentum domain

Product: surface **printable** t-shirts that **climbed fast**, not every tee and not long-lived bestsellers.

## Ranking

Etsy does not expose "became bestseller at". We own that clock.

1. Ingest listings on a schedule.
2. Store a **rank snapshot** per crawl (listing_id, observed_at, position_in_query, favorites, reviews).
3. `first_seen_in_top_at` = first time listing entered our top-N for that query.
4. `days_to_top` = `first_seen_in_top_at - listing.created_at`.
5. Sort key (higher is better): high velocity (`1 / max(days_to_top, 0.5)`), recent entry into top-N, positive delta in favorites/reviews. A listing that hit top-N in 2 days outranks one that has sat there for 30 days.

Do not use listing age alone. Do not treat Etsy search `score` as sales.

## Print-tee filter (must implement as scored rules, not a single keyword)

Include signals: taxonomy clothing/t-shirts, tags/title with print, graphic, DTG, sublimation, mockup tee language.

Exclude signals: vintage, used, embroidered/embroidery, blank Gildan wholesale, hoodie/sweatshirt unless later in scope, non-apparel.

Return `print_tee_score` 0–1 and a boolean `is_print_tee` at threshold 0.7. Keep reject reasons for debugging.

## Data we never invent

- Daily Etsy unit sales
- Official bestseller badge time
- Views (unless a later official field appears)
