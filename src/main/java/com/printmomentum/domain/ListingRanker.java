package com.printmomentum.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Momentum score for print tees: climb speed beats occupancy.
 * <p>
 * {@code days_to_top = first_seen_in_top_at - created_at}<br>
 * {@code days_in_top = scored_at - first_seen_in_top_at}<br>
 * {@code velocity = 1 / max(days_to_top, 0.5)} — hit top in 2d scores higher than 370d to top<br>
 * {@code recency = 1 / max(days_in_top, 0.5)} — recent entry into top-N beats a 30d occupant<br>
 * {@code score = velocity + recency + 1e-9 * max(favorers_delta, 0)}<br>
 * Favorers delta is a tie-breaker only (cannot overturn a 2-day climb vs a 30-day occupant).
 * Dividing by {@code max(days, 0.5)} guards zero and negative durations.
 */
public class ListingRanker {

	static final double MIN_DAYS = 0.5;
	static final double FAVORERS_TIE_WEIGHT = 1e-9;

	public double score(Instant createdAt, Instant firstSeenInTopAt, Instant scoredAt, int favorersDelta) {
		if (createdAt == null || firstSeenInTopAt == null || scoredAt == null) {
			return 0.0;
		}
		double velocity = 1.0 / clampDays(daysBetween(createdAt, firstSeenInTopAt));
		double recency = 1.0 / clampDays(daysBetween(firstSeenInTopAt, scoredAt));
		double favorersTie = Math.max(favorersDelta, 0) * FAVORERS_TIE_WEIGHT;
		return velocity + recency + favorersTie;
	}

	private static double daysBetween(Instant start, Instant end) {
		return Duration.between(start, end).toSeconds() / 86_400.0;
	}

	private static double clampDays(double days) {
		return Math.max(days, MIN_DAYS);
	}
}
