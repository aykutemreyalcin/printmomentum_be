package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ListingSnapshotIngestTest {

	private static final long TRACKED_ID = 6001L;

	@Autowired
	private ListingIngestJob ingestJob;

	@Autowired
	private ListingRepository listingRepository;

	@Autowired
	private ListingSnapshotRepository listingSnapshotRepository;

	@MockitoBean
	private EtsyClient etsyClient;

	@Test
	void twoCrawlsAppendSnapshotsAndSetFirstSeenInTopOnce() {
		when(etsyClient.searchActive(anyString(), nullable(Long.class), anyInt(), anyInt()))
				.thenReturn(pageWithTrackedAt(5, 10))
				.thenReturn(pageWithTrackedAt(5, 10))
				.thenReturn(pageWithTrackedAt(5, 10))
				.thenReturn(pageWithTrackedAt(40, 22))
				.thenReturn(pageWithTrackedAt(40, 22))
				.thenReturn(pageWithTrackedAt(40, 22));

		ingestJob.run();
		Listing afterFirst = listingRepository.findById(TRACKED_ID).orElseThrow();
		Instant firstSeenInTop = afterFirst.getFirstSeenInTopAt();
		assertThat(firstSeenInTop).isNotNull();
		List<ListingSnapshot> afterFirstSnapshots = listingSnapshotRepository.findByListingListingIdOrderByIdAsc(TRACKED_ID);
		assertThat(afterFirstSnapshots).hasSize(1);
		assertThat(afterFirstSnapshots.get(0).getPosition()).isEqualTo(5);
		assertThat(afterFirstSnapshots.get(0).getNumFavorers()).isEqualTo(10);
		Long firstSnapshotId = afterFirstSnapshots.get(0).getId();

		ingestJob.run();
		Listing afterSecond = listingRepository.findById(TRACKED_ID).orElseThrow();
		assertThat(afterSecond.getFirstSeenInTopAt()).isEqualTo(firstSeenInTop);
		List<ListingSnapshot> snapshots = listingSnapshotRepository.findByListingListingIdOrderByIdAsc(TRACKED_ID);
		assertThat(snapshots).hasSize(2);
		assertThat(snapshots.get(0).getId()).isEqualTo(firstSnapshotId);
		assertThat(snapshots.get(0).getPosition()).isEqualTo(5);
		assertThat(snapshots.get(1).getPosition()).isEqualTo(40);
		assertThat(snapshots.get(1).getNumFavorers()).isEqualTo(22);
		assertThat(snapshots.get(0).getCrawlRunId()).isNotEqualTo(snapshots.get(1).getCrawlRunId());
	}

	private static EtsySearchPage pageWithTrackedAt(int position, int favorers) {
		List<EtsyListing> results = new ArrayList<>();
		for (int i = 1; i <= position; i++) {
			long listingId = i == position ? TRACKED_ID : 8_000L + i;
			int listingFavorers = i == position ? favorers : 1;
			results.add(printTee(listingId, listingFavorers));
		}
		return new EtsySearchPage(results.size(), results);
	}

	private static EtsyListing printTee(long listingId, int favorers) {
		return new EtsyListing(
				listingId,
				10L,
				"Graphic DTG Print Tee",
				"Custom DTG artwork on a cotton t-shirt",
				List.of("graphic", "dtg", "t-shirt"),
				1603L,
				"https://www.etsy.com/listing/" + listingId,
				new EtsyMoney(2499, 100, "USD"),
				3,
				favorers,
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-02T00:00:00Z"),
				"active",
				List.of());
	}
}
