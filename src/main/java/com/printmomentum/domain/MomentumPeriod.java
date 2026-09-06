package com.printmomentum.domain;

import java.time.Duration;
import java.util.Locale;

public enum MomentumPeriod {
	DAILY("daily", "lastScore", Duration.ofDays(1)),
	WEEKLY("weekly", "lastScoreWeekly", Duration.ofDays(7)),
	MONTHLY("monthly", "lastScoreMonthly", Duration.ofDays(30));

	private final String param;
	private final String sortField;
	private final Duration window;

	MomentumPeriod(String param, String sortField, Duration window) {
		this.param = param;
		this.sortField = sortField;
		this.window = window;
	}

	public String param() {
		return param;
	}

	public String sortField() {
		return sortField;
	}

	public Duration window() {
		return window;
	}

	public int windowDays() {
		return (int) window.toDays();
	}

	public static MomentumPeriod parse(String value) {
		if (value == null || value.isBlank()) {
			return WEEKLY;
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		for (MomentumPeriod period : values()) {
			if (period.param.equals(normalized)) {
				return period;
			}
		}
		throw new IllegalArgumentException("momentumPeriod must be daily, weekly, or monthly");
	}
}
