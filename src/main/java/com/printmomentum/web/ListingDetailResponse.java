package com.printmomentum.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ListingDetailResponse(
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
		Integer quantityDelta,
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
		List<QueryHitItem> queryHits,
		List<String> tags,
		List<String> takeaway,
		List<QueryPeerItem> queryPeers,
		List<TimelinePointItem> timeline,
		List<ListingSnapshotItem> snapshots,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> rejectReasons) {

	public ListingDetailResponse {
		queryHits = queryHits == null ? List.of() : List.copyOf(queryHits);
		tags = tags == null ? List.of() : List.copyOf(tags.size() > 13 ? tags.subList(0, 13) : tags);
		takeaway = takeaway == null ? List.of() : List.copyOf(takeaway);
		queryPeers = queryPeers == null ? List.of() : List.copyOf(queryPeers);
		timeline = timeline == null ? List.of() : List.copyOf(timeline);
		snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
		rejectReasons = rejectReasons == null ? null : List.copyOf(rejectReasons);
	}
}
