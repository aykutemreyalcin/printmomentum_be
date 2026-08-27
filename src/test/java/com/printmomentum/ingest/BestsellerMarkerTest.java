package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.printmomentum.config.BestsellerProperties;
import com.printmomentum.config.IngestProperties;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingEstimator;
import com.printmomentum.domain.ListingEventRepository;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.Shop;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BestsellerMarkerTest {

	@Mock
	private EtsyPublicListingClient publicListingClient;

	@Mock
	private ListingRepository listingRepository;

	@Mock
	private ListingEventRepository listingEventRepository;

	@Mock
	private ListingEstimator listingEstimator;

	@Test
	void marksIndexedPrintTeeWhenEtsyConfirmsBestseller() {
		BestsellerMarker marker = marker(true);
		Listing listing = printTee(9001L);
		when(listingRepository.findByPrintTeeTrueAndEtsyBestsellerTrue()).thenReturn(java.util.List.of());
		when(listingRepository.findByPrintTeeTrue()).thenReturn(java.util.List.of(listing));
		when(listingRepository.findByPrintTeeTrueAndPmBestsellerTrue()).thenReturn(java.util.List.of());
		when(listingRepository.findById(9001L)).thenReturn(Optional.of(listing));
		when(publicListingClient.isBestseller(eq(9001L))).thenReturn(Optional.of(true));

		marker.refresh(Instant.parse("2026-08-27T12:00:00Z"), Set.of(9001L));

		assertThat(listing.isEtsyBestseller()).isTrue();
		assertThat(listing.getEtsyBestsellerSince()).isEqualTo(Instant.parse("2026-08-27T12:00:00Z"));
	}

	@Test
	void clearsEtsyFlagWhenVerificationReturnsFalse() {
		BestsellerMarker marker = marker(true);
		Listing listing = printTee(9002L);
		listing.setEtsyBestseller(true);
		listing.setEtsyBestsellerSince(Instant.parse("2026-08-20T00:00:00Z"));
		when(listingRepository.findByPrintTeeTrueAndEtsyBestsellerTrue()).thenReturn(java.util.List.of(listing));
		when(listingRepository.findByPrintTeeTrue()).thenReturn(java.util.List.of(listing));
		when(listingRepository.findByPrintTeeTrueAndPmBestsellerTrue()).thenReturn(java.util.List.of());
		when(listingRepository.findById(9002L)).thenReturn(Optional.of(listing));
		when(publicListingClient.isBestseller(eq(9002L))).thenReturn(Optional.of(false));

		marker.refresh(Instant.parse("2026-08-27T12:00:00Z"), Set.of());

		assertThat(listing.isEtsyBestseller()).isFalse();
		assertThat(listing.getEtsyBestsellerEndedAt()).isEqualTo(Instant.parse("2026-08-27T12:00:00Z"));
	}

	private BestsellerMarker marker(boolean enabled) {
		IngestProperties ingestProperties = new IngestProperties(
				false, "-", 100, 4, 4, 1603L, false, 100, 90, 100, 100, 30, 4, 5, 25, null, null);
		return new BestsellerMarker(
				new BestsellerProperties(enabled, 400, 4, "https://www.etsy.com"),
				ingestProperties,
				listingEstimator,
				publicListingClient,
				listingRepository,
				listingEventRepository);
	}

	private static Listing printTee(long listingId) {
		Shop shop = new Shop(listingId, "Shop", "https://www.etsy.com/shop/" + listingId);
		Listing listing = new Listing(listingId, shop, "Tee", "https://www.etsy.com/listing/" + listingId);
		listing.setPrintTee(true);
		return listing;
	}
}
