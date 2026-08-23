---
name: etsy-openapi-v3
description: Official Etsy Open API v3 facts for PrintMomentum ingest. Use when calling Etsy, mapping listing fields, handling rate limits, taxonomy, or search sort. Do not scrape.
---

# Etsy Open API v3 (this product)

Base: `https://openapi.etsy.com/v3/application`  
Auth: `x-api-key` header (app key). Request higher QPD in Etsy developer portal if needed.

## Use these endpoints

| Call | Path | Why |
|---|---|---|
| Active search | `GET /listings/active` | Ingest candidates |
| Buyer/seller taxonomy | `GET /buyer-taxonomy/nodes` or `/seller-taxonomy/nodes` | Resolve t-shirt taxonomy ids |
| One listing | `GET /listings/{listing_id}` | Refresh details/images |

## Search params that exist

`keywords`, `taxonomy_id`, `min_price`, `max_price`, `limit` (max 100), `offset`, `shop_location`.

`sort_on`: **only** `created` | `price` | `updated` | `score`.  
`sort_order`: `asc` | `desc`. `score` is always desc.

**There is no `bestseller` / `sales` sort on the official API.** Approximate popularity with `sort_on=score` plus our snapshots.

## Listing fields we persist

Keep: `listing_id`, `shop_id`, `title`, `description`, `tags`, `taxonomy_id`, `url`, `price`/`currency`, `quantity`, `num_favorers`, `created_timestamp` / `original_creation_timestamp`, `updated_timestamp`, `state`, images when included.

Do not assume `views` or sales counts exist.

## Rate limits

Application-level QPS + QPD (sliding window). Honor `x-limit-per-second`, `x-remaining-this-second`, `x-limit-per-day`, `x-remaining-today`, `retry-after` on 429.

Client rules: cap 8 req/s locally, exponential backoff on 429, cache taxonomy for 24h, never N+1 listing fetches in a tight loop.

## Legal

API Terms only. No HTML scrape, no captcha bypass, no unofficial "best selling" page parsers.
