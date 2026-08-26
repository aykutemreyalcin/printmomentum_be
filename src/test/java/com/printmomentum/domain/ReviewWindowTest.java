package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewWindowTest {

	private final ReviewWindow window = new ReviewWindow();

	@Test
	void countsOnlyLastThirtyDaysAndKeepsLatestTimestamp() {
		Instant now = Instant.parse("2026-08-26T00:00:00Z");
		ReviewWindow.Summary summary = window.summarize(
				List.of(
						Instant.parse("2026-06-01T00:00:00Z"),
						Instant.parse("2026-08-01T00:00:00Z"),
						Instant.parse("2026-08-20T12:00:00Z")),
				now);
		assertThat(summary.reviews30d()).isEqualTo(2);
		assertThat(summary.lastReviewAt()).isEqualTo(Instant.parse("2026-08-20T12:00:00Z"));
	}
}
