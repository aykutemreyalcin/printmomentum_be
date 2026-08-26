package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListingTimelineTest {

	private final ListingTimeline timeline = new ListingTimeline();

	@Test
	void ordersListedThenTopThenReview() {
		Listing listing = listing();
		listing.setOriginalCreatedAt(Instant.parse("2026-08-01T00:00:00Z"));
		listing.setFirstSeenAt(Instant.parse("2026-08-02T00:00:00Z"));
		listing.setFirstSeenInTopAt(Instant.parse("2026-08-03T00:00:00Z"));
		listing.setLastReviewAt(Instant.parse("2026-08-10T00:00:00Z"));

		List<ListingTimeline.Point> points = timeline.points(listing, List.of());

		assertThat(points).extracting(ListingTimeline.Point::kind)
				.containsExactly("LISTED", "FIRST_SEEN", "ENTERED_TOP", "LAST_REVIEW");
		assertThat(points.get(0).label()).isEqualTo("Listed");
		assertThat(points.get(2).label()).isEqualTo("Entered our top-N");
	}

	@Test
	void keepsListedAndTheLatestWhenMoreThanEight() {
		Listing listing = listing();
		listing.setOriginalCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		List<ListingEvent> events = new ArrayList<>();
		for (int i = 2; i <= 12; i++) {
			events.add(new ListingEvent(
					listing, ListingEvent.ETSY_BESTSELLER_ON, Instant.parse("2026-01-%02dT00:00:00Z".formatted(i))));
		}

		List<ListingTimeline.Point> points = timeline.points(listing, events);

		assertThat(points).hasSize(8);
		assertThat(points.get(0).kind()).isEqualTo("LISTED");
		assertThat(points.get(points.size() - 1).at()).isEqualTo(Instant.parse("2026-01-12T00:00:00Z"));
	}

	private static Listing listing() {
		Shop shop = new Shop(1L, "Shop", "https://www.etsy.com/shop/1");
		return new Listing(1L, shop, "Graphic tee", "https://www.etsy.com/listing/1");
	}
}
