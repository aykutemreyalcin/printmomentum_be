package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class CrawlScheduleTest {

	@Test
	void nextSlotAfterEveningIsMidnight() {
		Instant now = LocalDateTime.of(2026, 8, 26, 22, 38).atZone(CrawlSchedule.ISTANBUL).toInstant();
		Instant next = CrawlSchedule.nextCrawlAt(now);
		assertThat(next.atZone(CrawlSchedule.ISTANBUL).toLocalDateTime())
				.isEqualTo(LocalDateTime.of(2026, 8, 27, 0, 0));
	}

	@Test
	void nextSlotAfterMidnightIsSix() {
		Instant now = LocalDateTime.of(2026, 8, 27, 0, 0, 1).atZone(CrawlSchedule.ISTANBUL).toInstant();
		assertThat(CrawlSchedule.nextCrawlAt(now).atZone(CrawlSchedule.ISTANBUL).toLocalDateTime())
				.isEqualTo(LocalDateTime.of(2026, 8, 27, 6, 0));
	}

	@Test
	void exactSlotMovesToTheFollowingOne() {
		Instant noon = LocalDateTime.of(2026, 8, 27, 12, 0).atZone(CrawlSchedule.ISTANBUL).toInstant();
		assertThat(CrawlSchedule.nextCrawlAt(noon).atZone(CrawlSchedule.ISTANBUL).toLocalDateTime())
				.isEqualTo(LocalDateTime.of(2026, 8, 27, 18, 0));
		assertThat(noon.atZone(ZoneOffset.UTC)).isNotNull();
	}
}
