package com.printmomentum.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record NicheDetailResponse(
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
		List<NicheSnapshotItem> history,
		List<NicheTopListingItem> topListings,
		List<NicheTermItem> relatedTerms) {
}
