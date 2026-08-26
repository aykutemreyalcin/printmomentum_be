package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotDeltasTest {

	private final SnapshotDeltas deltas = new SnapshotDeltas();

	@Test
	void usesSnapshotAtOrBeforeSevenDaysAsBaseline() {
		Instant now = Instant.parse("2026-08-26T12:00:00Z");
		List<SnapshotDeltas.Point> points = List.of(
				new SnapshotDeltas.Point(Instant.parse("2026-08-10T12:00:00Z"), 10, 100),
				new SnapshotDeltas.Point(Instant.parse("2026-08-19T12:00:00Z"), 18, 180),
				new SnapshotDeltas.Point(Instant.parse("2026-08-26T12:00:00Z"), 30, 250));
		SnapshotDeltas.Delta delta = deltas.delta7d(points, now);
		assertThat(delta.favorers()).isEqualTo(12);
		assertThat(delta.views()).isEqualTo(70);
	}

	@Test
	void youngListingUsesOldestSnapshot() {
		Instant now = Instant.parse("2026-08-26T12:00:00Z");
		List<SnapshotDeltas.Point> points = List.of(
				new SnapshotDeltas.Point(Instant.parse("2026-08-24T12:00:00Z"), 4, 40),
				new SnapshotDeltas.Point(Instant.parse("2026-08-26T12:00:00Z"), 9, 55));
		SnapshotDeltas.Delta delta = deltas.delta7d(points, now);
		assertThat(delta.favorers()).isEqualTo(5);
		assertThat(delta.views()).isEqualTo(15);
	}
}
