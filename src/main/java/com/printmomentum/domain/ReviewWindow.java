package com.printmomentum.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Compresses Etsy listing reviews to two numbers. Review bodies are never stored.
 */
public final class ReviewWindow {

	public static final Duration DAYS_30 = Duration.ofDays(30);

	public record Summary(int reviews30d, Instant lastReviewAt) {
	}

	public Summary summarize(List<Instant> createdAt, Instant now) {
		if (createdAt == null || createdAt.isEmpty() || now == null) {
			return new Summary(0, null);
		}
		Instant cutoff = now.minus(DAYS_30);
		int recent = 0;
		Instant last = null;
		for (Instant created : createdAt) {
			if (created == null) {
				continue;
			}
			if (!created.isBefore(cutoff)) {
				recent++;
			}
			if (last == null || created.isAfter(last)) {
				last = created;
			}
		}
		return new Summary(recent, last);
	}
}
