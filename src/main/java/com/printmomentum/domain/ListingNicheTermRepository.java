package com.printmomentum.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ListingNicheTermRepository extends JpaRepository<ListingNicheTerm, ListingNicheTerm.ListingNicheTermId> {

	List<ListingNicheTerm> findByNicheTermId(Long nicheTermId);

	List<ListingNicheTerm> findByListingIdIn(Collection<Long> listingIds);

	@Modifying
	@Query("delete from ListingNicheTerm l where l.listingId = :listingId")
	void deleteByListingId(long listingId);

	@Modifying
	void deleteAllInBatch();
}
