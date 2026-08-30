package com.printmomentum.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NicheSnapshotItem(
		LocalDate observedDay,
		String window,
		int listingCount,
		int newEntrants14d,
		BigDecimal cloneDensity7d,
		BigDecimal breakInRate) {
}
