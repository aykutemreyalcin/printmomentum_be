package com.printmomentum.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "listing")
public class Listing {

	@Id
	@Column(name = "listing_id", nullable = false)
	private Long listingId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "shop_id", nullable = false)
	private Shop shop;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false, length = 2048)
	private String url;

	private Long taxonomyId;

	@Column(precision = 12, scale = 2)
	private BigDecimal priceAmount;

	@Column(length = 3)
	private String currency;

	@Column(columnDefinition = "TEXT")
	private String tags;

	@Column(nullable = false)
	private int numFavorers;

	private Instant etsyCreatedAt;

	private Instant etsyUpdatedAt;

	@Column(precision = 4, scale = 3)
	private BigDecimal printTeeScore;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "is_print_tee", nullable = false)
	private boolean printTee;

	@Column(nullable = false)
	private Instant firstSeenAt;

	@Column(nullable = false)
	private Instant lastSeenAt;

	private Instant firstSeenInTopAt;

	@OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ListingImage> images = new ArrayList<>();

	protected Listing() {
	}

	public Listing(Long listingId, Shop shop, String title, String url) {
		this.listingId = listingId;
		this.shop = shop;
		this.title = title;
		this.url = url;
	}

	@PrePersist
	void onPersist() {
		Instant now = Instant.now();
		if (firstSeenAt == null) {
			firstSeenAt = now;
		}
		if (lastSeenAt == null) {
			lastSeenAt = now;
		}
	}

	public void addImage(ListingImage image) {
		images.add(image);
		image.setListing(this);
	}

	public Long getListingId() {
		return listingId;
	}

	public void setListingId(Long listingId) {
		this.listingId = listingId;
	}

	public Shop getShop() {
		return shop;
	}

	public void setShop(Shop shop) {
		this.shop = shop;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Long getTaxonomyId() {
		return taxonomyId;
	}

	public void setTaxonomyId(Long taxonomyId) {
		this.taxonomyId = taxonomyId;
	}

	public BigDecimal getPriceAmount() {
		return priceAmount;
	}

	public void setPriceAmount(BigDecimal priceAmount) {
		this.priceAmount = priceAmount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public int getNumFavorers() {
		return numFavorers;
	}

	public void setNumFavorers(int numFavorers) {
		this.numFavorers = numFavorers;
	}

	public Instant getEtsyCreatedAt() {
		return etsyCreatedAt;
	}

	public void setEtsyCreatedAt(Instant etsyCreatedAt) {
		this.etsyCreatedAt = etsyCreatedAt;
	}

	public Instant getEtsyUpdatedAt() {
		return etsyUpdatedAt;
	}

	public void setEtsyUpdatedAt(Instant etsyUpdatedAt) {
		this.etsyUpdatedAt = etsyUpdatedAt;
	}

	public BigDecimal getPrintTeeScore() {
		return printTeeScore;
	}

	public void setPrintTeeScore(BigDecimal printTeeScore) {
		this.printTeeScore = printTeeScore;
	}

	public boolean isPrintTee() {
		return printTee;
	}

	public void setPrintTee(boolean printTee) {
		this.printTee = printTee;
	}

	public Instant getFirstSeenAt() {
		return firstSeenAt;
	}

	public void setFirstSeenAt(Instant firstSeenAt) {
		this.firstSeenAt = firstSeenAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public Instant getFirstSeenInTopAt() {
		return firstSeenInTopAt;
	}

	public void setFirstSeenInTopAt(Instant firstSeenInTopAt) {
		this.firstSeenInTopAt = firstSeenInTopAt;
	}

	public List<ListingImage> getImages() {
		return images;
	}
}
