package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ListingPeriodMetricsTest {

	private final ListingEstimator estimator = new ListingEstimator();

	@Test
	void mapsDailyWeeklyAndMonthlySignals() {
		Listing listing = new Listing(null, null, "tee", "https://etsy.com");
		listing.setReviews1d(1);
		listing.setReviews7d(4);
		listing.setReviews30d(10);
		listing.setDeltaFavorers1d(2);
		listing.setDeltaFavorers7d(8);
		listing.setDeltaFavorers30d(20);
		listing.setDeltaViews1d(10);
		listing.setDeltaViews7d(40);
		listing.setDeltaViews30d(100);
		listing.setPriceAmount(new BigDecimal("20.00"));

		ListingPeriodMetrics.Values daily = ListingPeriodMetrics.forListing(listing, MomentumPeriod.DAILY, estimator);
		assertThat(daily.reviews()).isEqualTo(1);
		assertThat(daily.deltaFavorers()).isEqualTo(2);
		assertThat(daily.deltaViews()).isEqualTo(10);
		assertThat(daily.estSales()).isEqualTo(10.0);
		assertThat(daily.estRevenue()).isEqualTo(200.0);

		ListingPeriodMetrics.Values weekly = ListingPeriodMetrics.forListing(listing, MomentumPeriod.WEEKLY, estimator);
		assertThat(weekly.reviews()).isEqualTo(4);
		assertThat(weekly.deltaFavorers()).isEqualTo(8);
		assertThat(weekly.deltaViews()).isEqualTo(40);
		assertThat(weekly.estSales()).isEqualTo(40.0);

		ListingPeriodMetrics.Values monthly = ListingPeriodMetrics.forListing(listing, MomentumPeriod.MONTHLY, estimator);
		assertThat(monthly.reviews()).isEqualTo(10);
		assertThat(monthly.deltaFavorers()).isEqualTo(20);
		assertThat(monthly.deltaViews()).isEqualTo(100);
		assertThat(monthly.estSales()).isEqualTo(100.0);
	}
}
