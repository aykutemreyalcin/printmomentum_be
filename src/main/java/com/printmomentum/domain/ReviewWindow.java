package com.printmomentum.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Compresses Etsy listing reviews to window counts. Review bodies are never stored.
 */
public final class ReviewWindow {

	public static final Duration DAYS_30 = Duration.ofDays(30);

	public record Summary(int reviews1d, int reviews7d, int reviews30d, Instant lastReviewAt) {
	}

	public Summary summarize(List<Instant> createdAt, Instant now) {
		if (createdAt == null || createdAt.isEmpty() || now == null) {
			return new Summary(0, 0, 0, null);
		}
		int reviews1d = countInWindow(createdAt, now, Duration.ofDays(1));
		int reviews7d = countInWindow(createdAt, now, Duration.ofDays(7));
		int reviews30d = countInWindow(createdAt, now, DAYS_30);
		Instant last = null;
		for (Instant created : createdAt) {
			if (created == null) {
				continue;
			}
			if (last == null || created.isAfter(last)) {
				last = created;
			}
		}
		return new Summary(reviews1d, reviews7d, reviews30d, last);
	}

	private static int countInWindow(List<Instant> createdAt, Instant now, Duration window) {
		Instant cutoff = now.minus(window);
		int recent = 0;
		for (Instant created : createdAt) {
			if (created != null && !created.isBefore(cutoff)) {
				recent++;
			}
		}
		return recent;
	}
}
