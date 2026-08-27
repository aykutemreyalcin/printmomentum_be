package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingQueryHit;
import com.printmomentum.domain.ListingQueryHitRepository;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import com.printmomentum.domain.QueryStats;
import com.printmomentum.domain.QueryStatsCalculator;
import com.printmomentum.domain.QueryStatsRepository;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "user")
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

	@Autowired
	private ListingQueryHitRepository listingQueryHitRepository;

	@Autowired
	private QueryStatsRepository queryStatsRepository;

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
		ListingSnapshot first = new ListingSnapshot(listing, "crawl-older", older, 5, 20);
		first.setQuantity(20);
		first.setViews(400);
		listingSnapshotRepository.save(first);
		ListingSnapshot second = new ListingSnapshot(listing, "crawl-newer", newer, 40, 31);
		second.setQuantity(15);
		second.setViews(520);
		listingSnapshotRepository.save(second);
		LocalDate day = LocalDate.of(2026, 8, 21);
		listingQueryHitRepository.save(
				new ListingQueryHit(listing, "graphic tee", day, 3, "crawl-newer", newer));
		QueryStats stats = new QueryStats("graphic tee", day);
		stats.apply(new QueryStatsCalculator.Row(12, 18400, new BigDecimal("28.40"), 40, 1200));
		queryStatsRepository.save(stats);

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
				.andExpect(jsonPath("$.shopUrl").value("https://www.etsy.com/shop/10010"))
				.andExpect(jsonPath("$.tags.length()").value(3))
				.andExpect(jsonPath("$.tags[0]").value("graphic"))
				.andExpect(jsonPath("$.quantityDelta").value(-5))
				.andExpect(jsonPath("$.takeaway.length()").value(5))
				.andExpect(jsonPath("$.takeaway[0]").value("Entered our top-N in 2.0 days."))
				.andExpect(jsonPath("$.queryPeers.length()").value(1))
				.andExpect(jsonPath("$.queryPeers[0].query").value("graphic tee"))
				.andExpect(jsonPath("$.queryPeers[0].position").value(3))
				.andExpect(jsonPath("$.queryPeers[0].etsyCount").value(18400))
				.andExpect(jsonPath("$.timeline[0].kind").value("LISTED"))
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
		listing.setLastScoreWeekly(new BigDecimal("0.750000000"));
		listing.setReviews30d(4);
		listing.setEtsyBestseller(true);
		listing.setEtsyBestsellerSince(Instant.parse("2026-08-12T00:00:00Z"));
		Instant created = Instant.parse("2026-08-01T00:00:00Z");
		listing.setEtsyCreatedAt(created);
		listing.setOriginalCreatedAt(created);
		listing.setFirstSeenAt(created.plusSeconds(86_400));
		listing.setFirstSeenInTopAt(created.plusSeconds(2 * 86_400));
		listing.addImage(new ListingImage("https://i.etsystatic.com/10010.jpg", 1));
		return listingRepository.save(listing);
	}
}
