package com.printmomentum.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ShopCrawlQueueRepository extends JpaRepository<ShopCrawlQueue, Long> {

	@Query("select q from ShopCrawlQueue q where q.status = 'pending' order by q.enqueuedAt asc")
	List<ShopCrawlQueue> findPendingOrderByEnqueuedAtAsc();
}
