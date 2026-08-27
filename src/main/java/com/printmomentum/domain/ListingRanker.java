package com.printmomentum.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Momentum scores for print tees.
 * <p>
 * <b>Daily</b> — climb speed into top-N (short-term spike detection):<br>
 * {@code velocity = 1 / max(days_to_top, 0.5)}<br>
 * {@code recency = 1 / max(days_in_top, 0.5)}<br>
 * {@code daily = velocity + recency + 1e-9 * max(favorers_delta, 0)}<br>
 * <p>
 * <b>Weekly / monthly</b> — sustained trend from crawl history:<br>
 * favori rate + view rate + rankable position climb + favori acceleration.
 */
public class ListingRanker {

	static final double MIN_DAYS = 0.5;
	static final double FAVORERS_TIE_WEIGHT = 1e-9;

	public record TrendInput(
			int favorersDelta,
			Integer viewsDelta,
			int positionImprovement,
			int accelerationFavorers,
			double windowDays) {
	}

	public double scoreDaily(Instant createdAt, Instant firstSeenInTopAt, Instant scoredAt, int favorersDelta) {
		return score(createdAt, firstSeenInTopAt, scoredAt, favorersDelta);
	}

	public double score(Instant createdAt, Instant firstSeenInTopAt, Instant scoredAt, int favorersDelta) {
		if (createdAt == null || firstSeenInTopAt == null || scoredAt == null) {
			return 0.0;
		}
		double velocity = 1.0 / clampDays(daysBetween(createdAt, firstSeenInTopAt));
		double recency = 1.0 / clampDays(daysBetween(firstSeenInTopAt, scoredAt));
		double favorersTie = Math.max(favorersDelta, 0) * FAVORERS_TIE_WEIGHT;
		return velocity + recency + favorersTie;
	}

	/**
	 * Trend momentum for weekly (7d) or monthly (30d) windows.
	 * Engagement and acceleration are log-scaled per day; position climb is normalized to top-N.
	 */
	public double scoreTrend(TrendInput input) {
		if (input == null) {
			return 0.0;
		}
		double windowDays = clampDays(input.windowDays());
		double engagement = Math.log1p(Math.max(input.favorersDelta(), 0)) / windowDays;
		double views = input.viewsDelta() != null
				? Math.log1p(Math.max(input.viewsDelta(), 0)) / windowDays
				: 0.0;
		double position = input.positionImprovement() > 0 ? input.positionImprovement() / 100.0 : 0.0;
		double acceleration = Math.log1p(Math.max(input.accelerationFavorers(), 0)) / (windowDays / 2.0);
		return engagement * 2.0 + views * 0.5 + position * 1.5 + acceleration * 1.0;
	}

	public Double daysToTop(Instant createdAt, Instant firstSeenInTopAt) {
		if (createdAt == null || firstSeenInTopAt == null) {
			return null;
		}
		return daysBetween(createdAt, firstSeenInTopAt);
	}

	private static double daysBetween(Instant start, Instant end) {
		return Duration.between(start, end).toSeconds() / 86_400.0;
	}

	private static double clampDays(double days) {
		return Math.max(days, MIN_DAYS);
	}
}
