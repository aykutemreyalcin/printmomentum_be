package com.printmomentum.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingQueryHitRepository extends JpaRepository<ListingQueryHit, ListingQueryHitId> {

	List<ListingQueryHit> findByIdObservedDayAndListingListingIdIn(LocalDate observedDay, Collection<Long> listingIds);

	List<ListingQueryHit> findByListingListingIdAndIdObservedDay(long listingId, LocalDate observedDay);

	@Query("select max(h.id.observedDay) from ListingQueryHit h where h.listing.listingId = :listingId")
	java.util.Optional<LocalDate> findLatestDayForListing(@Param("listingId") long listingId);

	@Modifying
	@Query("delete from ListingQueryHit h where h.id.observedDay < :cutoff")
	int deleteByObservedDayBefore(@Param("cutoff") LocalDate cutoff);
}
