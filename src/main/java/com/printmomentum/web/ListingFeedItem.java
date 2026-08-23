package com.printmomentum.web;

import java.math.BigDecimal;
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
		String shopName) {
}
