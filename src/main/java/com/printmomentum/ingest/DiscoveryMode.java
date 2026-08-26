package com.printmomentum.ingest;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum DiscoveryMode {
	TAXONOMY_CREATED("taxonomy:created", "created", "desc"),
	TAXONOMY_UPDATED("taxonomy:updated", "updated", "desc"),
	TAXONOMY_SCORE("taxonomy:score", "score", "desc"),
	SHOP_EXPANSION("shop:expansion", null, null),
	BENCHMARK("benchmark", "score", "desc");

	public static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

	private final String sourceKey;
	private final String sortOn;
	private final String sortOrder;

	DiscoveryMode(String sourceKey, String sortOn, String sortOrder) {
		this.sourceKey = sourceKey;
		this.sortOn = sortOn;
		this.sortOrder = sortOrder;
	}

	public String sourceKey() {
		return sourceKey;
	}

	public String sortOn() {
		return sortOn;
	}

	public String sortOrder() {
		return sortOrder;
	}

	public static DiscoveryMode forInstant(Instant observedAt) {
		int hour = observedAt.atZone(ISTANBUL).getHour();
		return switch (hour) {
			case 4 -> TAXONOMY_UPDATED;
			case 8 -> TAXONOMY_SCORE;
			case 12 -> TAXONOMY_CREATED;
			case 16 -> SHOP_EXPANSION;
			case 20 -> BENCHMARK;
			default -> TAXONOMY_CREATED;
		};
	}

	public static boolean isReviewRun(Instant observedAt) {
		return observedAt.atZone(ISTANBUL).getHour() == 12;
	}

	public static String benchmarkSource(String keywords) {
		return "benchmark:" + keywords;
	}

	public static String shopSource(long shopId) {
		return "shop:" + shopId;
	}
}
