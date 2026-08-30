package com.printmomentum.niche;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "printmomentum.ingest", name = "enabled", havingValue = "true")
public class NicheMaintenanceJob {

	private static final Logger log = LoggerFactory.getLogger(NicheMaintenanceJob.class);

	private final NicheTermService nicheTermService;

	public NicheMaintenanceJob(NicheTermService nicheTermService) {
		this.nicheTermService = nicheTermService;
	}

	@Scheduled(cron = "0 30 8 * * *", zone = "Europe/Istanbul")
	public void dailyRecompute() {
		Instant now = Instant.now();
		log.info("niche daily recompute start");
		nicheTermService.recomputeWindows(now);
		nicheTermService.validateEtsyCounts(now);
		log.info("niche daily recompute done");
	}
}
