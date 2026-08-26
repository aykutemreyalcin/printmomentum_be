package com.printmomentum.web;

import java.math.BigDecimal;

public record QueryPeerItem(
		String query,
		int position,
		int listingCount,
		Integer etsyCount,
		BigDecimal medianPrice,
		Integer medianFavorers,
		Integer medianViews) {
}
