package com.printmomentum.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

	List<Listing> findByPrintTeeTrue();

	List<Listing> findByPrintTeeTrueAndEtsyBestsellerTrue();

	List<Listing> findByPrintTeeTrueAndPmBestsellerTrue();

	List<Listing> findByPrintTeeTrueOrderByLastScoreDesc(org.springframework.data.domain.Pageable pageable);

	long countByShopShopIdAndPrintTeeTrue(Long shopId);
}
