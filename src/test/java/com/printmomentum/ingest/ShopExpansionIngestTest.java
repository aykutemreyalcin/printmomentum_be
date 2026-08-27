package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopCrawlQueue;
import com.printmomentum.domain.ShopCrawlQueueRepository;
import com.printmomentum.domain.ShopRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
		"printmomentum.ingest.stale-refresh-limit=0",
		"printmomentum.ingest.review-limit=0"
})
class ShopExpansionIngestTest {

	private static final long SHOP_ID = 77L;
	private static final long LISTING_ID = 77001L;

	@Autowired
	private ListingIngestJob ingestJob;

	@Autowired
	private ListingRepository listingRepository;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ShopCrawlQueueRepository shopCrawlQueueRepository;

	@MockitoBean
	private EtsyClient etsyClient;

	@MockitoBean
	private Clock clock;

	@BeforeEach
	void shopExpansionAtSixteenIstanbul() {
		Instant at16 =
				ZonedDateTime.of(2026, 8, 27, 16, 0, 0, 0, ZoneId.of("Europe/Istanbul")).toInstant();
		when(clock.instant()).thenReturn(at16);

		shopRepository.save(new Shop(SHOP_ID, "Shop Seventy", "https://www.etsy.com/shop/ShopSeventy"));
		shopCrawlQueueRepository.save(new ShopCrawlQueue(SHOP_ID, at16));

		when(etsyClient.searchActiveByShop(anyLong(), anyInt(), anyInt(), anyString(), anyString()))
				.thenReturn(new EtsySearchPage(1, List.of(printTee(LISTING_ID))));
	}

	@Test
	void shopCatalogListingsAreIndexedButNotRankable() {
		ingestJob.run();

		Listing listing = listingRepository.findById(LISTING_ID).orElseThrow();
		assertThat(listing.isPrintTee()).isTrue();
		assertThat(listing.getFirstSeenInTopAt()).isNull();
		assertThat(listing.getLastScore()).isNull();
	}

	private static EtsyListing printTee(long listingId) {
		return new EtsyListing(
				listingId,
				SHOP_ID,
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
}
