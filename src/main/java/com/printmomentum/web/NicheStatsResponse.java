package com.printmomentum.web;

import java.time.Instant;

public record NicheStatsResponse(
		int open,
		int closing,
		int closed,
		int lowData,
		int total,
		Instant computedAt) {
}
