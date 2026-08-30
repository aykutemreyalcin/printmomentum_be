package com.printmomentum.niche;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class NicheStartupRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(NicheStartupRunner.class);

	private final NicheReindexJob nicheReindexJob;
	private final com.printmomentum.domain.NicheTermRepository nicheTermRepository;

	public NicheStartupRunner(
			NicheReindexJob nicheReindexJob,
			com.printmomentum.domain.NicheTermRepository nicheTermRepository) {
		this.nicheReindexJob = nicheReindexJob;
		this.nicheTermRepository = nicheTermRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (nicheTermRepository.count() > 0) {
			return;
		}
		log.info("niche index empty; scheduling initial reindex");
		nicheReindexJob.triggerAsync();
	}
}
