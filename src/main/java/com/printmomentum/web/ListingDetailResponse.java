package com.printmomentum.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
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
		List<ListingSnapshotItem> snapshots,
		@JsonInclude(JsonInclude.Include.NON_NULL) List<String> rejectReasons) {

	public ListingDetailResponse {
		snapshots = List.copyOf(snapshots);
		rejectReasons = rejectReasons == null ? null : List.copyOf(rejectReasons);
	}
}
