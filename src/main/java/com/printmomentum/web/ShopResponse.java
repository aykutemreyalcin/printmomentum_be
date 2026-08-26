package com.printmomentum.web;

import java.math.BigDecimal;
import java.time.Instant;

public record ShopResponse(
		long shopId,
		String name,
		String url,
		String iconUrl,
		Integer transactionSoldCount,
		Integer listingActiveCount,
		Integer reviewCount,
		BigDecimal reviewAverage,
		Instant etsyCreatedAt,
		Double ageDays,
		long indexedListingCount) {
}
