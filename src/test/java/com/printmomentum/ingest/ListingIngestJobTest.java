package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
		"printmomentum.ingest.pages-per-sweep=1",
		"printmomentum.ingest.created-page=false",
		"printmomentum.ingest.stale-refresh-limit=0"
})
class ListingIngestJobTest {

	@Autowired
	private ListingIngestJob ingestJob;

	@Autowired
	private ListingRepository listingRepository;

	@Autowired
	private org.springframework.core.env.Environment environment;

	@MockitoBean
	private EtsyClient etsyClient;

	@Test
	void scheduleIsDisabledInTests() {
		assertThat(environment.getProperty("printmomentum.ingest.enabled")).isEqualTo("false");
		assertThat(environment.getProperty("printmomentum.ingest.cron")).isEqualTo("-");
	}

	@Test
	void upsertsPrintTeesSkipsExcludedAndDoesNotDuplicate() {
		when(etsyClient.searchActive(nullable(String.class), nullable(Long.class), anyInt(), anyInt(), anyString(), anyString()))
				.thenReturn(new EtsySearchPage(3, List.of(printTee(5001L), printTee(5002L), excludedHoodie(5003L))));

		IngestResult first = ingestJob.run();
		assertThat(storedIds()).containsExactlyInAnyOrder(5001L, 5002L);
		assertThat(listingRepository.findById(5003L)).isEmpty();
		assertThat(listingRepository.findAll().stream().filter(listing -> Set.of(5001L, 5002L).contains(listing.getListingId())))
				.allMatch(Listing::isPrintTee);
		assertThat(first.crawlRunId()).isNotNull();
		assertThat(first.stored()).isEqualTo(2);
		assertThat(first.skipped()).isEqualTo(1);

		listingRepository.findById(5001L).orElseThrow().setNumFavorers(99);

		IngestResult second = ingestJob.run();
		assertThat(storedIds()).containsExactlyInAnyOrder(5001L, 5002L);
		assertThat(listingRepository.findById(5001L).orElseThrow().getNumFavorers()).isEqualTo(12);
		assertThat(second.crawlRunId()).isNotEqualTo(first.crawlRunId());
		assertThat(second.stored()).isEqualTo(2);
		assertThat(second.skipped()).isEqualTo(1);

		verify(etsyClient, atLeastOnce())
				.searchActive(nullable(String.class), nullable(Long.class), anyInt(), anyInt(), anyString(), anyString());
	}

	private List<Long> storedIds() {
		return listingRepository.findAll().stream()
				.map(Listing::getListingId)
				.filter(id -> Set.of(5001L, 5002L, 5003L).contains(id))
				.toList();
	}

	private static EtsyListing printTee(long listingId) {
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
				12,
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-02T00:00:00Z"),
				"active",
				List.of(new EtsyImage("https://i.etsystatic.com/" + listingId + ".jpg", 1)),
				120,
				"i_did",
				"made_to_order",
				null);
	}

	private static EtsyListing excludedHoodie(long listingId) {
		return new EtsyListing(
				listingId,
				10L,
				"Graphic print hoodie",
				"DTG printed hoodie",
				List.of("hoodie", "graphic", "print", "dtg"),
				1603L,
				"https://www.etsy.com/listing/" + listingId,
				new EtsyMoney(3499, 100, "USD"),
				2,
				8,
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-02T00:00:00Z"),
				"active",
				List.of(),
				null,
				null,
				null,
				null);
	}
}
