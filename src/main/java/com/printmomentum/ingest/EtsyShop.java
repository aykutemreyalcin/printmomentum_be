package com.printmomentum.ingest;

import java.math.BigDecimal;
import java.time.Instant;

public record EtsyShop(
		Long shopId,
		String name,
		String url,
		String iconUrl,
		Integer transactionSoldCount,
		Integer listingActiveCount,
		Integer reviewCount,
		BigDecimal reviewAverage,
		Instant createdAt) {
}
