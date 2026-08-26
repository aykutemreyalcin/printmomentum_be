package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "shop_crawl_queue")
public class ShopCrawlQueue {

	@Id
	@Column(name = "shop_id")
	private Long shopId;

	@Column(nullable = false)
	private Instant enqueuedAt;

	@Column
	private Instant lastCrawledAt;

	@Column(nullable = false, length = 16)
	private String status = "pending";

	protected ShopCrawlQueue() {
	}

	public ShopCrawlQueue(long shopId, Instant enqueuedAt) {
		this.shopId = shopId;
		this.enqueuedAt = enqueuedAt;
		this.status = "pending";
	}

	public Long getShopId() {
		return shopId;
	}

	public Instant getEnqueuedAt() {
		return enqueuedAt;
	}

	public Instant getLastCrawledAt() {
		return lastCrawledAt;
	}

	public void setLastCrawledAt(Instant lastCrawledAt) {
		this.lastCrawledAt = lastCrawledAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
