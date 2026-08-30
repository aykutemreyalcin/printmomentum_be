package com.printmomentum.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record NicheTermItem(
		String slug,
		String label,
		String window,
		int listingCount,
		int newEntrants14d,
		BigDecimal cloneDensity7d,
		BigDecimal breakInRate,
		BigDecimal incumbentAgeDays,
		BigDecimal entrantMomentum,
		Integer etsyCount,
		Instant windowComputedAt,
		NicheTopListingItem topListing) {
}
