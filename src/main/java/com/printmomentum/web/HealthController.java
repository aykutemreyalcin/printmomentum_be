package com.printmomentum.web;

import com.printmomentum.domain.ListingRepository;
import com.printmomentum.ingest.CrawlSchedule;
import com.printmomentum.ingest.IngestStatusStore;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class HealthController {

	private final ListingRepository listingRepository;
	private final IngestStatusStore ingestStatusStore;

	public HealthController(ListingRepository listingRepository, IngestStatusStore ingestStatusStore) {
		this.listingRepository = listingRepository;
		this.ingestStatusStore = ingestStatusStore;
	}

	@GetMapping("/health")
	public HealthResponse health() {
		Instant lastCrawlAt = ingestStatusStore.lastFinishedAt();
		if (lastCrawlAt == null) {
			lastCrawlAt = listingRepository.findMaxPrintTeeLastSeenAt();
		}
		return new HealthResponse(
				"ok",
				"printmomentum-be",
				listingRepository.countByPrintTeeTrue(),
				lastCrawlAt,
				ingestStatusStore.lastAttemptAt(),
				CrawlSchedule.nextCrawlAt(Instant.now()),
				ingestStatusStore.lastOutcome().name(),
				ingestStatusStore.lastStored(),
				ingestStatusStore.lastSkipped(),
				ingestStatusStore.lastError());
	}

	public record HealthResponse(
			String status,
			String service,
			long indexedListings,
			Instant lastCrawlAt,
			Instant lastAttemptAt,
			Instant nextCrawlAt,
			String lastOutcome,
			Integer lastStored,
			Integer lastSkipped,
			String lastError) {
	}
}
