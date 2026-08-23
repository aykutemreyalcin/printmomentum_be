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
}
