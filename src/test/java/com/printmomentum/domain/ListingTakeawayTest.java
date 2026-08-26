package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListingTakeawayTest {

	private final ListingTakeaway takeaway = new ListingTakeaway();

	@Test
	void fiveFactLinesForACompleteListing() {
		List<String> lines = takeaway.lines(new ListingTakeaway.Input(
				2.1,
				40.0,
				true,
				Instant.parse("2026-08-12T00:00:00Z"),
				false,
				List.of("teacher", "funny", "custom", "graphic", "tee", "extra"),
				new BigDecimal("24.99"),
				new BigDecimal("28.40"),
				"teacher shirt"));

		assertThat(lines).containsExactly(
				"Entered our top-N in 2.1 days.",
				"Est. 40 sales / 30d (reviews30d / 0.10).",
				"Bestseller since 12 Aug 2026.",
				"Tag pack: teacher, funny, custom, graphic, tee.",
				"Price is 12% below the “teacher shirt” median.");
	}

	@Test
	void skipsMissingSignalsAndCapsAtFive() {
		assertThat(takeaway.lines(new ListingTakeaway.Input(
				null, null, false, null, true, List.of(), null, null, null)))
				.containsExactly("Likely bestseller (PM signal from reviews).");
	}
}
