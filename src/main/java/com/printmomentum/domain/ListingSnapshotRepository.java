package com.printmomentum.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingSnapshotRepository extends JpaRepository<ListingSnapshot, Long> {

	List<ListingSnapshot> findByListingListingIdOrderByIdAsc(Long listingId);
}
