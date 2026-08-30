package com.printmomentum.niche;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NicheReindexJob {

	private static final Logger log = LoggerFactory.getLogger(NicheReindexJob.class);

	private final NicheTermService nicheTermService;
	private final AtomicBoolean running = new AtomicBoolean(false);

	public NicheReindexJob(NicheTermService nicheTermService) {
		this.nicheTermService = nicheTermService;
	}

	public boolean triggerAsync() {
		if (!running.compareAndSet(false, true)) {
			log.info("niche reindex already running; skip duplicate trigger");
			return false;
		}
		Thread.startVirtualThread(() -> {
			try {
				log.info("niche reindex async start");
				int assignments = nicheTermService.reindexAll();
				log.info("niche reindex async done assignments={}", assignments);
			} catch (RuntimeException ex) {
				log.error("niche reindex async failed", ex);
			} finally {
				running.set(false);
			}
		});
		return true;
	}

	public boolean isRunning() {
		return running.get();
	}
}
