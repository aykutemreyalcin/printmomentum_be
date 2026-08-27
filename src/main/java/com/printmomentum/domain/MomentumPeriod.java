package com.printmomentum.domain;

import java.util.Locale;

public enum MomentumPeriod {
	DAILY("daily", "lastScore"),
	WEEKLY("weekly", "lastScoreWeekly"),
	MONTHLY("monthly", "lastScoreMonthly");

	private final String param;
	private final String sortField;

	MomentumPeriod(String param, String sortField) {
		this.param = param;
		this.sortField = sortField;
	}

	public String param() {
		return param;
	}

	public String sortField() {
		return sortField;
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
