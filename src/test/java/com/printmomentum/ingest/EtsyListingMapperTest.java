package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class EtsyListingMapperTest {

	private final EtsyListingMapper mapper = new EtsyListingMapper(new ObjectMapper());

	@Test
	void mapsDocumentedListingFieldsFromFixture() throws Exception {
		String json = new String(
				new ClassPathResource("etsy/listing.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		EtsyListing listing = mapper.readListing(json);

		assertThat(listing.listingId()).isEqualTo(1147645830L);
		assertThat(listing.shopId()).isEqualTo(12345L);
		assertThat(listing.title()).isEqualTo("Graphic DTG Print Tee");
		assertThat(listing.numFavorers()).isEqualTo(42);
		assertThat(listing.taxonomyId()).isEqualTo(1603L);
		assertThat(listing.tags()).containsExactly("graphic", "tee");
		assertThat(listing.price().currencyCode()).isEqualTo("USD");
		assertThat(listing.price().toDecimal()).isEqualByComparingTo(new BigDecimal("24.99"));
		assertThat(listing.createdAt()).isEqualTo(Instant.ofEpochSecond(1700000000));
		assertThat(listing.images()).containsExactly(new EtsyImage("https://i.etsystatic.com/example.jpg", 1));
		assertThat(listing.views()).isEqualTo(999999);
		assertThat(json).contains("views");
	}

	@Test
	void mapsSearchListingIdTitleAndFavorers() throws Exception {
		String json = new String(
				new ClassPathResource("etsy/search-active.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		EtsySearchPage page = mapper.readSearch(json);

		assertThat(page.count()).isEqualTo(1);
		assertThat(page.results()).hasSize(1);
		assertThat(page.results().get(0).listingId()).isEqualTo(1147645830L);
		assertThat(page.results().get(0).title()).isEqualTo("Graphic DTG Print Tee");
		assertThat(page.results().get(0).numFavorers()).isEqualTo(42);
	}
}
