package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "listing_image")
public class ListingImage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id", nullable = false)
	private Listing listing;

	@Column(nullable = false, length = 2048)
	private String url;

	@Column(name = "`rank`", nullable = false)
	private int rank;

	@Column(name = "storage_key", length = 512)
	private String storageKey;

	protected ListingImage() {
	}

	public ListingImage(String url, int rank) {
		this.url = url;
		this.rank = rank;
	}

	public Long getId() {
		return id;
	}

	public Listing getListing() {
		return listing;
	}

	void setListing(Listing listing) {
		this.listing = listing;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public String getStorageKey() {
		return storageKey;
	}

	public void setStorageKey(String storageKey) {
		this.storageKey = storageKey;
	}
}
