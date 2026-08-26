package com.printmomentum.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingEventRepository extends JpaRepository<ListingEvent, Long> {

	List<ListingEvent> findByListingListingIdOrderByObservedAtAscIdAsc(long listingId);
}
