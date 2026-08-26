package com.printmomentum.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopRepository;
import com.printmomentum.domain.ListingRepository;
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
class ShopsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ListingRepository listingRepository;

	@Test
	void missingShopReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/shops/999999")).andExpect(status().isNotFound());
	}

	@Test
	void shopReturnsAgeSalesAndIndexedCount() throws Exception {
		Shop shop = new Shop(44_001L, "Atlas Prints", "https://www.etsy.com/shop/AtlasPrints");
		shop.setTransactionSoldCount(1280);
		shop.setListingActiveCount(42);
		shop.setReviewCount(310);
		shop.setReviewAverage(new BigDecimal("4.80"));
		shop.setEtsyCreatedAt(Instant.parse("2022-01-01T00:00:00Z"));
		shopRepository.save(shop);
		Listing listing = new Listing(44_101L, shop, "Graphic print tee", "https://www.etsy.com/listing/44101");
		listing.setPrintTee(true);
		listingRepository.save(listing);

		mockMvc.perform(get("/api/v1/shops/{id}", 44_001L))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shopId").value(44001))
				.andExpect(jsonPath("$.name").value("Atlas Prints"))
				.andExpect(jsonPath("$.url").value("https://www.etsy.com/shop/AtlasPrints"))
				.andExpect(jsonPath("$.transactionSoldCount").value(1280))
				.andExpect(jsonPath("$.listingActiveCount").value(42))
				.andExpect(jsonPath("$.indexedListingCount").value(1))
				.andExpect(jsonPath("$.ageDays").isNumber());
	}
}
