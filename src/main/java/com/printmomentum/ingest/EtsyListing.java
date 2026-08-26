package com.printmomentum.ingest;

import java.time.Instant;
import java.util.List;

public record EtsyListing(
		long listingId,
		Long shopId,
		String title,
		String description,
		List<String> tags,
		Long taxonomyId,
		String url,
		EtsyMoney price,
		Integer quantity,
		Integer numFavorers,
		Instant createdAt,
		Instant originalCreatedAt,
		Instant updatedAt,
		String state,
		List<EtsyImage> images,
		Integer views,
		String whoMade,
		String whenMade,
		EtsyShop shop) {
}
