package com.printmomentum.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Demand estimates used by EverBee / Alura / Podkon-style tables.
 * Etsy does not expose unit sales. Public reviews are a purchase floor;
 * dividing by a review-leave rate lifts that floor to an estimated 30-day volume.
 * <p>
 * {@code est_sales_30d = reviews_30d / 0.10}<br>
 * {@code est_revenue_30d = est_sales_30d * price}<br>
 * {@code views_per_day = views / max(age_days, 1)}
 * <p>
 * UI must label these Est. Missing reviews → null, never 0.
 */
public final class ListingEstimator {

	public static final double REVIEW_RATE = 0.10;
	private static final double MIN_AGE_DAYS = 1.0;

	public record Estimate(Double estSales30d, Double estRevenue30d, Double viewsPerDay) {
	}

	public Estimate estimate(Integer reviews30d, BigDecimal price, Integer views, Instant createdAt, Instant now) {
		Double sales = estSales(reviews30d);
		Double revenue = sales == null || price == null ? null : sales * price.doubleValue();
		Double viewsPerDay = viewsPerDay(views, createdAt, now);
		return new Estimate(round(sales), round(revenue), round(viewsPerDay));
	}

	public Double estSales(Integer reviews30d) {
		if (reviews30d == null) {
			return null;
		}
		return reviews30d / REVIEW_RATE;
	}

	public Double viewsPerDay(Integer views, Instant createdAt, Instant now) {
		if (views == null || createdAt == null || now == null) {
			return null;
		}
		double ageDays = Duration.between(createdAt, now).toSeconds() / 86_400.0;
		return views / Math.max(ageDays, MIN_AGE_DAYS);
	}

	private static Double round(Double value) {
		if (value == null) {
			return null;
		}
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
