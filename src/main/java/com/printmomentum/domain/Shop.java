package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "shop")
public class Shop {

	@Id
	@Column(name = "shop_id", nullable = false)
	private Long shopId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(length = 2048)
	private String iconUrl;

	private Integer transactionSoldCount;

	private Integer listingActiveCount;

	private Integer reviewCount;

	@Column(precision = 3, scale = 2)
	private java.math.BigDecimal reviewAverage;

	private java.time.Instant etsyCreatedAt;

	private java.time.Instant lastRefreshedAt;

	protected Shop() {
	}

	public Shop(Long shopId, String name, String url) {
		this.shopId = shopId;
		this.name = name;
		this.url = url;
	}

	public Long getShopId() {
		return shopId;
	}

	public void setShopId(Long shopId) {
		this.shopId = shopId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}

	public Integer getTransactionSoldCount() {
		return transactionSoldCount;
	}

	public void setTransactionSoldCount(Integer transactionSoldCount) {
		this.transactionSoldCount = transactionSoldCount;
	}

	public Integer getListingActiveCount() {
		return listingActiveCount;
	}

	public void setListingActiveCount(Integer listingActiveCount) {
		this.listingActiveCount = listingActiveCount;
	}

	public Integer getReviewCount() {
		return reviewCount;
	}

	public void setReviewCount(Integer reviewCount) {
		this.reviewCount = reviewCount;
	}

	public java.math.BigDecimal getReviewAverage() {
		return reviewAverage;
	}

	public void setReviewAverage(java.math.BigDecimal reviewAverage) {
		this.reviewAverage = reviewAverage;
	}

	public java.time.Instant getEtsyCreatedAt() {
		return etsyCreatedAt;
	}

	public void setEtsyCreatedAt(java.time.Instant etsyCreatedAt) {
		this.etsyCreatedAt = etsyCreatedAt;
	}

	public java.time.Instant getLastRefreshedAt() {
		return lastRefreshedAt;
	}

	public void setLastRefreshedAt(java.time.Instant lastRefreshedAt) {
		this.lastRefreshedAt = lastRefreshedAt;
	}
}
