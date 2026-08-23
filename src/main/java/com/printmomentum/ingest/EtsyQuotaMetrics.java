package com.printmomentum.ingest;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class EtsyQuotaMetrics {

	public EtsyQuotaMetrics(MeterRegistry meterRegistry, EtsyQuotaTracker tracker) {
		Gauge.builder("etsy.remaining.today", tracker, EtsyQuotaMetrics::remainingOrNan)
				.description("Etsy Open API remaining requests today (x-remaining-today)")
				.register(meterRegistry);
	}

	private static double remainingOrNan(EtsyQuotaTracker tracker) {
		Integer remaining = tracker.remainingToday();
		return remaining == null ? Double.NaN : remaining.doubleValue();
	}
}
