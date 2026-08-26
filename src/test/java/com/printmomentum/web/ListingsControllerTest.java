package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopRepository;
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
class ListingsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ListingRepository listingRepository;

	@Test
	void emptyQueryReturnsEmptyItems() throws Exception {
		mockMvc.perform(get("/api/v1/listings").param("q", "__no_such_print_tee__"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isArray())
				.andExpect(jsonPath("$.items").isEmpty())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void twoListingsAreOrderedByMomentumScoreDesc() throws Exception {
		seedPrintTee(9101L, "BE009 high momentum tee", "0.900000000", "Shop High");
		seedPrintTee(9102L, "BE009 low momentum tee", "0.100000000", "Shop Low");

		mockMvc.perform(get("/api/v1/listings").param("q", "BE009"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].listingId").value(9101))
				.andExpect(jsonPath("$.items[0].title").value("BE009 high momentum tee"))
				.andExpect(jsonPath("$.items[0].price").value(24.99))
				.andExpect(jsonPath("$.items[0].currency").value("USD"))
				.andExpect(jsonPath("$.items[0].imageUrl").value("https://i.etsystatic.com/9101.jpg"))
				.andExpect(jsonPath("$.items[0].etsyUrl").value("https://www.etsy.com/listing/9101"))
				.andExpect(jsonPath("$.items[0].daysToTop").value(2.0))
				.andExpect(jsonPath("$.items[0].momentumScore").value(0.9))
				.andExpect(jsonPath("$.items[0].numFavorers").value(12))
				.andExpect(jsonPath("$.items[0].shopName").value("Shop High"))
				.andExpect(jsonPath("$.items[0].printTee").doesNotExist())
				.andExpect(jsonPath("$.items[1].listingId").value(9102))
				.andExpect(jsonPath("$.items[1].shopName").value("Shop Low"));
	}

	@Test
	void sizeOneReturnsOneListing() throws Exception {
		seedPrintTee(9111L, "BE009size high", "0.800000000", "Shop A");
		seedPrintTee(9112L, "BE009size low", "0.200000000", "Shop B");

		mockMvc.perform(get("/api/v1/listings").param("q", "BE009size").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.total").value(2))
				.andExpect(jsonPath("$.items[0].listingId").value(9111));
	}

	@Test
	void sizeOneThousandIsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/listings").param("size", "1000"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void bestsellerPresetReturnsOnlyFlaggedListings() throws Exception {
		seedPrintTee(9121L, "BE009 bestseller tee", "0.900000000", "Shop High");
		Listing listing = listingRepository.findById(9121L).orElseThrow();
		listing.setEtsyBestseller(true);
		listingRepository.save(listing);
		seedPrintTee(9122L, "BE009 regular tee", "0.800000000", "Shop Low");

		mockMvc.perform(get("/api/v1/listings").param("q", "BE009").param("bestseller", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].listingId").value(9121))
				.andExpect(jsonPath("$.items[0].etsyBestseller").value(true));
	}

	private void seedPrintTee(long listingId, String title, String lastScore, String shopName) {
		Shop shop = shopRepository.save(new Shop(listingId, shopName, "https://www.etsy.com/shop/" + listingId));
		Listing listing = new Listing(listingId, shop, title, "https://www.etsy.com/listing/" + listingId);
		listing.setPrintTee(true);
		listing.setPriceAmount(new BigDecimal("24.99"));
		listing.setCurrency("USD");
		listing.setNumFavorers(12);
		listing.setLastScore(new BigDecimal(lastScore));
		Instant created = Instant.parse("2026-01-01T00:00:00Z");
		listing.setEtsyCreatedAt(created);
		listing.setFirstSeenInTopAt(created.plusSeconds(2 * 86_400));
		listing.addImage(new ListingImage("https://i.etsystatic.com/" + listingId + ".jpg", 1));
		listingRepository.save(listing);
	}
}
