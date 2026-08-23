package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListingDetailControllerTest {

	private static final long LISTING_ID = 10_010L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ListingRepository listingRepository;

	@Autowired
	private ListingSnapshotRepository listingSnapshotRepository;

	@Test
	void missingListingReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/listings/999999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void detailMatchesFeedFieldsAndReturnsSnapshotsNewestLast() throws Exception {
		Listing listing = seedListing();
		Instant older = Instant.parse("2026-08-20T10:00:00Z");
		Instant newer = Instant.parse("2026-08-21T10:00:00Z");
		listingSnapshotRepository.save(new ListingSnapshot(listing, "crawl-older", older, 5, 20));
		listingSnapshotRepository.save(new ListingSnapshot(listing, "crawl-newer", newer, 40, 31));

		mockMvc.perform(get("/api/v1/listings/{id}", LISTING_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.listingId").value(LISTING_ID))
				.andExpect(jsonPath("$.title").value("BE010 graphic print tee"))
				.andExpect(jsonPath("$.price").value(24.99))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.imageUrl").value("https://i.etsystatic.com/10010.jpg"))
				.andExpect(jsonPath("$.etsyUrl").value("https://www.etsy.com/listing/10010"))
				.andExpect(jsonPath("$.daysToTop").value(2.0))
				.andExpect(jsonPath("$.momentumScore").value(0.75))
				.andExpect(jsonPath("$.numFavorers").value(31))
				.andExpect(jsonPath("$.shopName").value("BE010 Shop"))
				.andExpect(jsonPath("$.snapshots.length()").value(2))
				.andExpect(jsonPath("$.snapshots[0].observedAt").value("2026-08-20T10:00:00Z"))
				.andExpect(jsonPath("$.snapshots[0].position").value(5))
				.andExpect(jsonPath("$.snapshots[1].observedAt").value("2026-08-21T10:00:00Z"))
				.andExpect(jsonPath("$.snapshots[1].position").value(40))
				.andExpect(jsonPath("$.snapshots[1].numFavorers").value(31))
				.andExpect(jsonPath("$.rejectReasons").doesNotExist());
	}

	@Test
	void rejectReasonsAreIncludedOnlyInDebugMode() throws Exception {
		seedListing();

		mockMvc.perform(get("/api/v1/listings/{id}", LISTING_ID).param("debug", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rejectReasons").isArray())
				.andExpect(jsonPath("$.rejectReasons").isEmpty());
	}

	private Listing seedListing() {
		Shop shop = shopRepository.save(
				new Shop(LISTING_ID, "BE010 Shop", "https://www.etsy.com/shop/10010"));
		Listing listing = new Listing(
				LISTING_ID, shop, "BE010 graphic print tee", "https://www.etsy.com/listing/10010");
		listing.setDescription("DTG printed cotton t-shirt");
		listing.setTags("[\"graphic\",\"print\",\"tee\"]");
		listing.setTaxonomyId(1603L);
		listing.setPrintTee(true);
		listing.setPrintTeeScore(new BigDecimal("1.000"));
		listing.setPriceAmount(new BigDecimal("24.99"));
		listing.setCurrency("USD");
		listing.setNumFavorers(31);
		listing.setLastScore(new BigDecimal("0.750000000"));
		Instant created = Instant.parse("2026-08-01T00:00:00Z");
		listing.setEtsyCreatedAt(created);
		listing.setFirstSeenInTopAt(created.plusSeconds(2 * 86_400));
		listing.addImage(new ListingImage("https://i.etsystatic.com/10010.jpg", 1));
		return listingRepository.save(listing);
	}
}
