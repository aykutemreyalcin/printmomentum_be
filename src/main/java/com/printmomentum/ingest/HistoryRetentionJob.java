package com.printmomentum.ingest;

import com.printmomentum.config.IngestProperties;
import com.printmomentum.domain.HistoryRetention;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingQueryHitRepository;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HistoryRetentionJob {

	private static final Logger log = LoggerFactory.getLogger(HistoryRetentionJob.class);

	private final ListingRepository listingRepository;
	private final ListingSnapshotRepository listingSnapshotRepository;
	private final ListingQueryHitRepository listingQueryHitRepository;
	private final HistoryRetention historyRetention;
	private final IngestProperties ingestProperties;

	public HistoryRetentionJob(
			ListingRepository listingRepository,
			ListingSnapshotRepository listingSnapshotRepository,
			ListingQueryHitRepository listingQueryHitRepository,
			HistoryRetention historyRetention,
			IngestProperties ingestProperties) {
		this.listingRepository = listingRepository;
		this.listingSnapshotRepository = listingSnapshotRepository;
		this.listingQueryHitRepository = listingQueryHitRepository;
		this.historyRetention = historyRetention;
		this.ingestProperties = ingestProperties;
	}

	@Scheduled(cron = "0 0 3 * * *", zone = "Europe/Istanbul")
	@Transactional
	public void compact() {
		Instant now = Instant.now();
		int removedSnapshots = 0;
		for (Listing listing : listingRepository.findByPrintTeeTrue()) {
			List<ListingSnapshot> snapshots =
					listingSnapshotRepository.findByListingListingIdOrderByObservedAtAscIdAsc(listing.getListingId());
			List<Long> deleteIds = historyRetention.idsToDelete(
					snapshots.stream()
							.filter(row -> row.getId() != null)
							.map(row -> new HistoryRetention.SnapshotRef(row.getId(), row.getObservedAt()))
							.toList(),
					now);
			if (!deleteIds.isEmpty()) {
				listingSnapshotRepository.deleteAllById(deleteIds);
				removedSnapshots += deleteIds.size();
			}
		}
		LocalDate cutoff = now.atZone(HistoryRetention.ISTANBUL).toLocalDate().minusDays(ingestProperties.queryHitRetentionDays());
		int removedHits = listingQueryHitRepository.deleteByObservedDayBefore(cutoff);
		log.info("history compact snapshots={} query_hits={}", removedSnapshots, removedHits);
	}
}
