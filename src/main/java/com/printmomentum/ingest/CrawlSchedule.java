package com.printmomentum.ingest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class CrawlSchedule {

	public static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
	public static final int[] HOURS = {0, 4, 8, 12, 16, 20};

	private CrawlSchedule() {
	}

	public static Instant nextCrawlAt(Instant now) {
		ZonedDateTime clock = now.atZone(ISTANBUL);
		for (int hour : HOURS) {
			ZonedDateTime slot = clock.withHour(hour).withMinute(0).withSecond(0).withNano(0);
			if (slot.isAfter(clock)) {
				return slot.toInstant();
			}
		}
		return clock.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
	}
}
