package com.printmomentum.web;

import java.math.BigDecimal;

public record NicheTopListingItem(
		long listingId,
		String title,
		String imageUrl,
		String etsyUrl,
		BigDecimal momentumScore) {
}
