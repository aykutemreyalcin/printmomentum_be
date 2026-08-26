package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithUserDetails("user@printmomentum.local")
class FavoritesControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ListingRepository listingRepository;

	@Test
	void putThenGetThenDeleteFavorite() throws Exception {
		seedPrintTee(9401L, "Favorite graphic print tee");

		mockMvc.perform(put("/api/v1/listings/{id}/favorite", 9401L)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/favorites"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(1))
				.andExpect(jsonPath("$.items[0].listingId").value(9401))
				.andExpect(jsonPath("$.items[0].favorite").value(true));

		mockMvc.perform(get("/api/v1/listings").param("q", "Favorite graphic"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].favorite").value(true));

		mockMvc.perform(delete("/api/v1/listings/{id}/favorite", 9401L)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/favorites"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(0));
	}

	@Test
	void missingListingFavoriteIsNotFound() throws Exception {
		mockMvc.perform(put("/api/v1/listings/{id}/favorite", 999999L)).andExpect(status().isNotFound());
	}

	private void seedPrintTee(long listingId, String title) {
		Shop shop = shopRepository.save(new Shop(listingId, "Fav Shop", "https://www.etsy.com/shop/" + listingId));
		Listing listing = new Listing(listingId, shop, title, "https://www.etsy.com/listing/" + listingId);
		listing.setPrintTee(true);
		listing.setPriceAmount(new BigDecimal("24.99"));
		listing.setCurrency("USD");
		listing.setNumFavorers(12);
		listing.setLastScore(new BigDecimal("0.500000000"));
		Instant created = Instant.parse("2026-01-01T00:00:00Z");
		listing.setEtsyCreatedAt(created);
		listing.setFirstSeenInTopAt(created.plusSeconds(2 * 86_400));
		listing.addImage(new ListingImage("https://i.etsystatic.com/" + listingId + ".jpg", 1));
		listingRepository.save(listing);
	}
}
