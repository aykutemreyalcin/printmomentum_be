package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingNicheTerm;
import com.printmomentum.domain.ListingNicheTermRepository;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.NicheTerm;
import com.printmomentum.domain.NicheTermRepository;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopRepository;
import com.printmomentum.niche.NicheSlug;
import java.math.BigDecimal;
import java.time.Instant;
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
class NicheWindowControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ListingRepository listingRepository;

	@Autowired
	private NicheTermRepository nicheTermRepository;

	@Autowired
	private ListingNicheTermRepository listingNicheTermRepository;

	@Test
	void listAndDetailReturnNicheTerms() throws Exception {
		Instant now = Instant.parse("2026-08-30T12:00:00Z");
		Shop shop = shopRepository.save(new Shop(9201L, "Niche Shop", "https://etsy.com/shop/niche"));
		Listing listing = listingRepository.save(seedListing(92001L, shop, "Dolly Parton tee", now));
		NicheTerm term = nicheTermRepository.save(new NicheTerm(NicheSlug.fromLabel("dolly parton"), "dolly parton", now));
		term.setListingCount(1);
		term.applyWindow("OPEN", 3, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.5), BigDecimal.valueOf(14), BigDecimal.valueOf(0.7), now);
		nicheTermRepository.save(term);
		listingNicheTermRepository.save(new ListingNicheTerm(listing.getListingId(), term, BigDecimal.ONE, "tag"));

		mockMvc.perform(get("/api/v1/niches").param("window", "OPEN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].slug").value("dolly-parton"))
				.andExpect(jsonPath("$.items[0].window").value("OPEN"));

		mockMvc.perform(get("/api/v1/niches/stats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.open").value(1))
				.andExpect(jsonPath("$.total").value(1));

		mockMvc.perform(get("/api/v1/niches/dolly-parton"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.label").value("dolly parton"))
				.andExpect(jsonPath("$.topListings.length()").value(1));
	}

	@Test
	void unknownSlugReturns404() throws Exception {
		mockMvc.perform(get("/api/v1/niches/no-such-niche"))
				.andExpect(status().isNotFound());
	}

	private static Listing seedListing(long listingId, Shop shop, String title, Instant now) {
		Listing listing = new Listing(listingId, shop, title, "https://etsy.com/listing/" + listingId);
		listing.setPrintTee(true);
		listing.setPrintTeeScore(BigDecimal.valueOf(0.9));
		listing.setLastScoreWeekly(BigDecimal.valueOf(0.8));
		listing.setFirstSeenAt(now);
		listing.setLastSeenAt(now);
		listing.setNumFavorers(10);
		return listing;
	}
}
