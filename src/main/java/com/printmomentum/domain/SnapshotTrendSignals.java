package com.printmomentum.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Derives weekly/monthly trend inputs from crawl snapshots:
 * favori/view deltas, rankable search-position climb, and favori acceleration.
 */
public final class SnapshotTrendSignals {

	static final int TOP_N = 100;

	public record TrendPoint(Instant observedAt, int numFavorers, Integer views, int position) {
	}

	private final SnapshotDeltas snapshotDeltas = new SnapshotDeltas();

	public ListingRanker.TrendInput trendInput(List<TrendPoint> snapshots, Instant now, Duration window) {
		if (snapshots == null || snapshots.isEmpty() || now == null || window == null) {
			return new ListingRanker.TrendInput(0, null, 0, 0, windowDays(window));
		}
		List<TrendPoint> ordered = snapshots.stream()
				.filter(point -> point != null && point.observedAt() != null)
				.sorted(Comparator.comparing(TrendPoint::observedAt))
				.toList();
		if (ordered.isEmpty()) {
			return new ListingRanker.TrendInput(0, null, 0, 0, windowDays(window));
		}
		List<SnapshotDeltas.Point> deltaPoints = ordered.stream()
				.map(point -> new SnapshotDeltas.Point(point.observedAt(), point.numFavorers(), point.views()))
				.toList();
		SnapshotDeltas.Delta delta = snapshotDeltas.delta(deltaPoints, now, window);
		return new ListingRanker.TrendInput(
				delta.favorers(),
				delta.views(),
				positionImprovement(ordered, now, window),
				accelerationFavorers(ordered, now, window),
				windowDays(window));
	}

	private static double windowDays(Duration window) {
		return window == null ? ListingRanker.MIN_DAYS : window.toSeconds() / 86_400.0;
	}

	private static int positionImprovement(List<TrendPoint> ordered, Instant now, Duration window) {
		Instant cutoff = now.minus(window);
		Integer baseline = null;
		Integer current = null;
		for (TrendPoint point : ordered) {
			if (point.observedAt().isAfter(now)) {
				continue;
			}
			Integer rankable = rankablePosition(point.position());
			if (rankable == null) {
				continue;
			}
			if (!point.observedAt().isBefore(cutoff)) {
				current = bestRank(current, rankable);
			}
			if (!point.observedAt().isAfter(cutoff)) {
				baseline = bestRank(baseline, rankable);
			}
		}
		if (baseline == null || current == null || baseline <= current) {
			return 0;
		}
		return baseline - current;
	}

	private static int accelerationFavorers(List<TrendPoint> ordered, Instant now, Duration window) {
		Instant cutoff = now.minus(window);
		Instant midpoint = now.minus(window.dividedBy(2));
		Integer baselineFavorers = null;
		Integer midpointFavorers = null;
		Integer latestFavorers = null;
		for (TrendPoint point : ordered) {
			if (point.observedAt().isAfter(now)) {
				continue;
			}
			if (!point.observedAt().isBefore(cutoff)) {
				latestFavorers = point.numFavorers();
			}
			if (!point.observedAt().isAfter(midpoint)) {
				midpointFavorers = point.numFavorers();
			}
			if (!point.observedAt().isAfter(cutoff)) {
				baselineFavorers = point.numFavorers();
			}
		}
		if (baselineFavorers == null || midpointFavorers == null || latestFavorers == null) {
			return 0;
		}
		int olderHalf = midpointFavorers - baselineFavorers;
		int recentHalf = latestFavorers - midpointFavorers;
		return recentHalf - olderHalf;
	}

	private static Integer rankablePosition(int position) {
		if (position <= 0 || position > TOP_N) {
			return null;
		}
		return position;
	}

	private static Integer bestRank(Integer current, int candidate) {
		if (current == null) {
			return candidate;
		}
		return Math.min(current, candidate);
	}
}
