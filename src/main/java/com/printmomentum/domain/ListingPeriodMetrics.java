package com.printmomentum.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Maps stored 1d / 7d / 30d signals to the active momentum window for API responses.
 */
public final class ListingPeriodMetrics {

	public record Values(
			Integer reviews,
			Integer deltaFavorers,
			Integer deltaViews,
			Double estSales,
			Double estRevenue) {
	}

	private ListingPeriodMetrics() {
	}

	public static Values forListing(Listing listing, MomentumPeriod period, ListingEstimator estimator) {
		MomentumPeriod active = period == null ? MomentumPeriod.WEEKLY : period;
		Integer reviews = reviews(listing, active);
		Double estSales = estimator.estSales(reviews);
		BigDecimal price = listing.getPriceAmount();
		Double estRevenue = estSales == null || price == null
				? null
				: round(estSales * price.doubleValue());
		return new Values(reviews, deltaFavorers(listing, active), deltaViews(listing, active), estSales, estRevenue);
	}

	private static Integer reviews(Listing listing, MomentumPeriod period) {
		return switch (period) {
			case DAILY -> listing.getReviews1d();
			case WEEKLY -> listing.getReviews7d();
			case MONTHLY -> listing.getReviews30d();
		};
	}

	private static Integer deltaFavorers(Listing listing, MomentumPeriod period) {
		return switch (period) {
			case DAILY -> listing.getDeltaFavorers1d();
			case WEEKLY -> listing.getDeltaFavorers7d();
			case MONTHLY -> listing.getDeltaFavorers30d();
		};
	}

	private static Integer deltaViews(Listing listing, MomentumPeriod period) {
		return switch (period) {
			case DAILY -> listing.getDeltaViews1d();
			case WEEKLY -> listing.getDeltaViews7d();
			case MONTHLY -> listing.getDeltaViews30d();
		};
	}

	private static Double round(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
