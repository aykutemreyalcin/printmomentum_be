package com.printmomentum.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Period deltas from our crawl history (Podkon top-mover).
 * Baseline is the latest snapshot at or before {@code now - window};
 * if the listing is younger than the window, the oldest snapshot is used.
 */
public final class SnapshotDeltas {

	public static final Duration WINDOW_7D = Duration.ofDays(7);

	public record Point(Instant observedAt, int numFavorers, Integer views) {
	}

	public record Delta(int favorers, Integer views) {
	}

	public Delta delta7d(List<Point> snapshots, Instant now) {
		return delta(snapshots, now, WINDOW_7D);
	}

	public Delta delta(List<Point> snapshots, Instant now, Duration window) {
		if (snapshots == null || snapshots.isEmpty() || now == null || window == null) {
			return new Delta(0, null);
		}
		List<Point> ordered = snapshots.stream()
				.filter(point -> point != null && point.observedAt() != null)
				.sorted(Comparator.comparing(Point::observedAt))
				.toList();
		if (ordered.isEmpty()) {
			return new Delta(0, null);
		}
		Point latest = ordered.get(ordered.size() - 1);
		Instant cutoff = now.minus(window);
		Point baseline = ordered.get(0);
		for (int i = ordered.size() - 1; i >= 0; i--) {
			Point candidate = ordered.get(i);
			if (!candidate.observedAt().isAfter(cutoff)) {
				baseline = candidate;
				break;
			}
		}
		int favorers = latest.numFavorers() - baseline.numFavorers();
		Integer views = minus(latest.views(), baseline.views());
		return new Delta(favorers, views);
	}

	private static Integer minus(Integer latest, Integer baseline) {
		if (latest == null || baseline == null) {
			return null;
		}
		return latest - baseline;
	}
}
