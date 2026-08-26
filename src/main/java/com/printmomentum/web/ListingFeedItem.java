package com.printmomentum.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingFeedItem(
		long listingId,
		String title,
		BigDecimal price,
		String currency,
		String imageUrl,
		String etsyUrl,
		Double daysToTop,
		BigDecimal momentumScore,
		int numFavorers,
		String shopName,
		Long shopId,
		Integer shopSales,
		BigDecimal shopRating,
		Integer shopReviewCount,
		String shopUrl,
		Double shopAgeDays,
		Integer listingActiveCount,
		Integer views,
		Integer quantity,
		Double ageDays,
		Instant originalCreatedAt,
		Instant firstSeenAt,
		Instant firstSeenInTopAt,
		Instant lastSeenAt,
		Instant lastReviewAt,
		String whoMade,
		String whenMade,
		boolean etsyBestseller,
		Instant etsyBestsellerSince,
		Instant etsyBestsellerEndedAt,
		boolean pmBestseller,
		boolean favorite,
		Integer reviews30d,
		Double estSales30d,
		Double estRevenue30d,
		Integer deltaFavorers7d,
		Integer deltaViews7d,
		Double viewsPerDay,
		List<QueryHitItem> queryHits) {

	public ListingFeedItem {
		queryHits = queryHits == null
				? List.of()
				: List.copyOf(queryHits.size() > 4 ? queryHits.subList(0, 4) : queryHits);
	}
}
