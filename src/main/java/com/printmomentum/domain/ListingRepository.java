package com.printmomentum.domain;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

	long countByPrintTeeTrue();

	@Query("select max(l.lastSeenAt) from Listing l where l.printTee = true")
	Instant findMaxPrintTeeLastSeenAt();

	List<Listing> findByPrintTeeTrue();

	List<Listing> findByPrintTeeTrueAndEtsyBestsellerTrue();

	List<Listing> findByPrintTeeTrueAndPmBestsellerTrue();

	List<Listing> findByPrintTeeTrueOrderByLastScoreDesc(Pageable pageable);

	List<Listing> findByPrintTeeTrueOrderByLastScoreWeeklyDesc(Pageable pageable);

	List<Listing> findByPrintTeeTrueOrderByLastSeenAtAsc(Pageable pageable);

	@Query("select count(l) from Listing l where l.printTee = true and l.reviews30d is not null")
	long countWithReviews30d();

	long countByShopShopIdAndPrintTeeTrue(Long shopId);
}
