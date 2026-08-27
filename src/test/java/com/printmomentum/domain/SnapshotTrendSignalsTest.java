package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotTrendSignalsTest {

	private final SnapshotTrendSignals signals = new SnapshotTrendSignals();

	@Test
	void positionImprovementCountsRankableClimb() {
		Instant now = Instant.parse("2026-08-27T12:00:00Z");
		List<SnapshotTrendSignals.TrendPoint> points = List.of(
				new SnapshotTrendSignals.TrendPoint(Instant.parse("2026-08-20T12:00:00Z"), 10, 100, 80),
				new SnapshotTrendSignals.TrendPoint(Instant.parse("2026-08-27T12:00:00Z"), 20, 200, 20));
		ListingRanker.TrendInput input = signals.trendInput(points, now, Duration.ofDays(7));
		assertThat(input.positionImprovement()).isEqualTo(60);
		assertThat(input.favorersDelta()).isEqualTo(10);
	}

	@Test
	void shopPositionOutsideTopNIsIgnored() {
		Instant now = Instant.parse("2026-08-27T12:00:00Z");
		List<SnapshotTrendSignals.TrendPoint> points = List.of(
				new SnapshotTrendSignals.TrendPoint(Instant.parse("2026-08-20T12:00:00Z"), 5, 50, 1),
				new SnapshotTrendSignals.TrendPoint(Instant.parse("2026-08-27T12:00:00Z"), 8, 80, 393));
		ListingRanker.TrendInput input = signals.trendInput(points, now, Duration.ofDays(7));
		assertThat(input.positionImprovement()).isZero();
	}

	@Test
	void accelerationPrefersRecentHalfGrowth() {
		Instant now = Instant.parse("2026-08-30T12:00:00Z");
		List<SnapshotTrendSignals.TrendPoint> points = List.of(
				new SnapshotTrendSignals.TrendPoint(Instant.parse("2026-08-16T12:00:00Z"), 0, 0, 50),
				new SnapshotTrendSignals.TrendPoint(Instant.parse("2026-08-23T12:00:00Z"), 2, 20, 40),
				new SnapshotTrendSignals.TrendPoint(Instant.parse("2026-08-30T12:00:00Z"), 10, 100, 30));
		ListingRanker.TrendInput input = signals.trendInput(points, now, Duration.ofDays(14));
		assertThat(input.accelerationFavorers()).isEqualTo(6);
	}
}
