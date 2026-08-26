package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ListingEstimatorTest {

	private final ListingEstimator estimator = new ListingEstimator();

	@Test
	void tenReviewsBecomeOneHundredEstimatedSales() {
		ListingEstimator.Estimate estimate = estimator.estimate(
				10,
				new BigDecimal("20.00"),
				300,
				Instant.parse("2026-07-27T00:00:00Z"),
				Instant.parse("2026-08-26T00:00:00Z"));
		assertThat(estimate.estSales30d()).isEqualTo(100.0);
		assertThat(estimate.estRevenue30d()).isEqualTo(2000.0);
		assertThat(estimate.viewsPerDay()).isEqualTo(10.0);
	}

	@Test
	void missingReviewsStayNullNotZero() {
		ListingEstimator.Estimate estimate = estimator.estimate(null, new BigDecimal("20.00"), null, null, Instant.now());
		assertThat(estimate.estSales30d()).isNull();
		assertThat(estimate.estRevenue30d()).isNull();
		assertThat(estimate.viewsPerDay()).isNull();
	}
}
