package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryRetentionTest {

	private final HistoryRetention retention = new HistoryRetention();

	@Test
	void keepsAllRowsInsideFourteenDays() {
		Instant now = Instant.parse("2026-08-26T12:00:00Z");
		List<HistoryRetention.SnapshotRef> rows = List.of(
				ref(1, "2026-08-20T00:00:00Z"),
				ref(2, "2026-08-20T06:00:00Z"),
				ref(3, "2026-08-26T12:00:00Z"));
		assertThat(retention.idsToDelete(rows, now)).isEmpty();
	}

	@Test
	void compactOldDaysToNoonAndAlwaysKeepLatestTwo() {
		Instant now = Instant.parse("2026-08-26T12:00:00Z");
		List<HistoryRetention.SnapshotRef> rows = new ArrayList<>();
		rows.add(ref(10, "2026-06-01T00:00:00Z"));
		rows.add(ref(11, "2026-06-01T12:00:00Z"));
		rows.add(ref(12, "2026-06-01T18:00:00Z"));
		rows.add(ref(20, "2026-08-25T12:00:00Z"));
		rows.add(ref(21, "2026-08-26T12:00:00Z"));
		assertThat(retention.idsToDelete(rows, now)).containsExactly(10L, 12L);
	}

	@Test
	void dropsNonMondayRowsOlderThanNinetyDays() {
		ZonedDateTime now = Instant.parse("2026-08-26T12:00:00Z").atZone(ZoneOffset.UTC);
		assertThat(now).isNotNull();
		List<HistoryRetention.SnapshotRef> rows = List.of(
				ref(1, "2026-04-06T12:00:00Z"),
				ref(2, "2026-04-07T12:00:00Z"),
				ref(8, "2026-08-25T12:00:00Z"),
				ref(9, "2026-08-26T12:00:00Z"));
		assertThat(retention.idsToDelete(rows, Instant.parse("2026-08-26T12:00:00Z"))).contains(2L);
		assertThat(retention.idsToDelete(rows, Instant.parse("2026-08-26T12:00:00Z"))).doesNotContain(1L, 8L, 9L);
	}

	private static HistoryRetention.SnapshotRef ref(long id, String iso) {
		return new HistoryRetention.SnapshotRef(id, Instant.parse(iso));
	}
}
