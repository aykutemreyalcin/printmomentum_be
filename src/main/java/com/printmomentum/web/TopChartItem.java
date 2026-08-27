package com.printmomentum.web;

import java.math.BigDecimal;
import java.util.List;

public record TopChartItem(
		long listingId,
		String title,
		String imageUrl,
		String etsyUrl,
		BigDecimal momentumScore,
		Double daysToTop,
		int numFavorers,
		Integer views,
		List<ListingSnapshotItem> snapshots) {

	public TopChartItem {
		snapshots = snapshots == null ? List.of() : List.copyOf(snapshots);
	}
}
