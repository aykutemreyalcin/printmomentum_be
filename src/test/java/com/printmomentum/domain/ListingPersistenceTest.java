package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ListingPersistenceTest {

	@Autowired
	private ShopRepository shopRepository;

	@Autowired
	private ListingRepository listingRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void saveAndLoadListingWithShopAndImage() {
		Shop shop = shopRepository.save(new Shop(1L, "Print Shop", "https://www.etsy.com/shop/printshop"));

		Instant seenAt = Instant.parse("2026-08-23T12:00:00Z");
		Listing listing = new Listing(100L, shop, "Graphic print tee", "https://www.etsy.com/listing/100");
		listing.setDescription("A DTG printed t-shirt");
		listing.setTaxonomyId(123L);
		listing.setPriceAmount(new BigDecimal("24.99"));
		listing.setCurrency("USD");
		listing.setTags("[\"graphic\",\"tee\"]");
		listing.setNumFavorers(42);
		listing.setEtsyCreatedAt(seenAt);
		listing.setEtsyUpdatedAt(seenAt);
		listing.setPrintTeeScore(new BigDecimal("0.850"));
		listing.setPrintTee(true);
		listing.setFirstSeenAt(seenAt);
		listing.setLastSeenAt(seenAt);
		listing.addImage(new ListingImage("https://i.etsystatic.com/example.jpg", 1));

		listingRepository.saveAndFlush(listing);
		entityManager.clear();

		Listing loaded = listingRepository.findById(100L).orElseThrow();
		assertThat(loaded.getTitle()).isEqualTo("Graphic print tee");
		assertThat(loaded.getShop().getName()).isEqualTo("Print Shop");
		assertThat(loaded.isPrintTee()).isTrue();
		assertThat(loaded.getPrintTeeScore()).isEqualByComparingTo("0.850");
		assertThat(loaded.getImages()).hasSize(1);
		assertThat(loaded.getImages().get(0).getRank()).isEqualTo(1);
		assertThat(loaded.getImages().get(0).getUrl()).contains("etsystatic");
	}

	@Test
	void uniqueListingIdIsEnforced() {
		Shop shop = shopRepository.save(new Shop(2L, "Other Shop", "https://www.etsy.com/shop/othershop"));

		Listing first = new Listing(200L, shop, "First tee", "https://www.etsy.com/listing/200");
		listingRepository.saveAndFlush(first);
		entityManager.clear();

		Listing duplicate = new Listing(200L, shop, "Duplicate tee", "https://www.etsy.com/listing/200-dup");
		assertThatThrownBy(() -> {
			entityManager.persist(duplicate);
			entityManager.flush();
		}).isInstanceOf(PersistenceException.class);
	}
}
