package com.printmomentum.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QueryStatsItem(
		String query,
		LocalDate observedDay,
		int listingCount,
		Integer etsyCount,
		BigDecimal medianPrice,
		Integer medianFavorers,
		Integer medianViews) {
}
