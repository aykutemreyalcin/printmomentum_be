package com.printmomentum.domain;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingSnapshotRepository extends JpaRepository<ListingSnapshot, Long> {

	List<ListingSnapshot> findByListingListingIdOrderByIdAsc(Long listingId);

	List<ListingSnapshot> findTop2ByListingListingIdOrderByIdDesc(Long listingId);

	List<ListingSnapshot> findByListingListingIdOrderByObservedAtDescIdDesc(Long listingId, Pageable pageable);
}
