package com.printmomentum.ingest;

import java.util.concurrent.atomic.AtomicReference;

public class EtsyQuotaTracker {

	private final AtomicReference<Integer> remainingToday = new AtomicReference<>();

	public void recordRemainingToday(Integer remaining) {
		if (remaining != null) {
			remainingToday.set(remaining);
		}
	}

	public Integer remainingToday() {
		return remainingToday.get();
	}

	public void clear() {
		remainingToday.set(null);
	}
}
