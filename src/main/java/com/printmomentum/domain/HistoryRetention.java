package com.printmomentum.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Snapshot compaction so MariaDB does not grow 4 rows/listing/day forever.
 * <ul>
 *   <li>0–14 days: keep all 6-hour rows</li>
 *   <li>14–90 days: one per Istanbul day, closest to 12:00</li>
 *   <li>90+ days: one per Monday, closest to 12:00</li>
 * </ul>
 * Always keep the two newest rows (delta baseline).
 */
public final class HistoryRetention {

	public static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
	public static final int KEEP_RECENT_DAYS = 14;
	public static final int DAILY_UNTIL_DAYS = 90;
	public static final int KEEP_LATEST = 2;

	public record SnapshotRef(long id, Instant observedAt) {
	}

	public List<Long> idsToDelete(List<SnapshotRef> snapshots, Instant now) {
		if (snapshots == null || snapshots.isEmpty() || now == null) {
			return List.of();
		}
		List<SnapshotRef> ordered = snapshots.stream()
				.filter(row -> row != null && row.observedAt() != null)
				.sorted(Comparator.comparing(SnapshotRef::observedAt).thenComparing(SnapshotRef::id))
				.toList();
		if (ordered.size() <= KEEP_LATEST) {
			return List.of();
		}
		Set<Long> keep = new HashSet<>();
		for (int i = ordered.size() - KEEP_LATEST; i < ordered.size(); i++) {
			keep.add(ordered.get(i).id());
		}
		LocalDate today = now.atZone(ISTANBUL).toLocalDate();
		Map<LocalDate, List<SnapshotRef>> byDay = new LinkedHashMap<>();
		for (SnapshotRef row : ordered) {
			LocalDate day = row.observedAt().atZone(ISTANBUL).toLocalDate();
			byDay.computeIfAbsent(day, key -> new ArrayList<>()).add(row);
		}
		for (Map.Entry<LocalDate, List<SnapshotRef>> entry : byDay.entrySet()) {
			long ageDays = ChronoUnit.DAYS.between(entry.getKey(), today);
			if (ageDays <= KEEP_RECENT_DAYS) {
				entry.getValue().forEach(row -> keep.add(row.id()));
				continue;
			}
			if (ageDays <= DAILY_UNTIL_DAYS || entry.getKey().getDayOfWeek() == DayOfWeek.MONDAY) {
				keep.add(closestToNoon(entry.getValue()).id());
			}
		}
		return ordered.stream().map(SnapshotRef::id).filter(id -> !keep.contains(id)).toList();
	}

	private static SnapshotRef closestToNoon(List<SnapshotRef> dayRows) {
		Instant noon = dayRows.get(0).observedAt().atZone(ISTANBUL).toLocalDate().atTime(LocalTime.NOON).atZone(ISTANBUL).toInstant();
		return dayRows.stream()
				.min(Comparator.comparingLong((SnapshotRef row) -> Math.abs(DurationSeconds(row.observedAt(), noon)))
						.thenComparingLong(SnapshotRef::id))
				.orElse(dayRows.get(0));
	}

	private static long DurationSeconds(Instant a, Instant b) {
		return Math.abs(a.getEpochSecond() - b.getEpochSecond());
	}
}
