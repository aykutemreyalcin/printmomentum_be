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

	@Column(name = "is_print_tee", nullable = false)
	private boolean printTee;

	@Column(nullable = false)
	private Instant firstSeenAt;

	@Column(nullable = false)
	private Instant lastSeenAt;

	private Instant firstSeenInTopAt;

	@Column(precision = 18, scale = 9)
	private BigDecimal lastScore;

	private Instant lastScoredAt;

	private Integer views;

	private Integer quantity;

	private Instant originalCreatedAt;

	private Instant lastReviewAt;

	@Column(name = "reviews_30d")
	private Integer reviews30d;

	@Column(length = 32)
	private String whoMade;

	@Column(length = 32)
	private String whenMade;

	@Column(nullable = false)
	private boolean etsyBestseller;

	private Instant etsyBestsellerSince;

	private Instant etsyBestsellerEndedAt;

	@Column(nullable = false)
	private boolean pmBestseller;

	@Column(name = "delta_favorers_7d")
	private Integer deltaFavorers7d;

	@Column(name = "delta_views_7d")
	private Integer deltaViews7d;

	private Instant pmBestsellerSince;

	private Instant pmBestsellerEndedAt;

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

	public BigDecimal getLastScore() {
		return lastScore;
	}

	public void setLastScore(BigDecimal lastScore) {
		this.lastScore = lastScore;
	}

	public Instant getLastScoredAt() {
		return lastScoredAt;
	}

	public void setLastScoredAt(Instant lastScoredAt) {
		this.lastScoredAt = lastScoredAt;
	}

	public Integer getViews() {
		return views;
	}

	public void setViews(Integer views) {
		this.views = views;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Instant getOriginalCreatedAt() {
		return originalCreatedAt;
	}

	public void setOriginalCreatedAt(Instant originalCreatedAt) {
		this.originalCreatedAt = originalCreatedAt;
	}

	public Instant getLastReviewAt() {
		return lastReviewAt;
	}

	public void setLastReviewAt(Instant lastReviewAt) {
		this.lastReviewAt = lastReviewAt;
	}

	public Integer getReviews30d() {
		return reviews30d;
	}

	public void setReviews30d(Integer reviews30d) {
		this.reviews30d = reviews30d;
	}

	public String getWhoMade() {
		return whoMade;
	}

	public void setWhoMade(String whoMade) {
		this.whoMade = whoMade;
	}

	public String getWhenMade() {
		return whenMade;
	}

	public void setWhenMade(String whenMade) {
		this.whenMade = whenMade;
	}

	public boolean isEtsyBestseller() {
		return etsyBestseller;
	}

	public void setEtsyBestseller(boolean etsyBestseller) {
		this.etsyBestseller = etsyBestseller;
	}

	public Instant getEtsyBestsellerSince() {
		return etsyBestsellerSince;
	}

	public void setEtsyBestsellerSince(Instant etsyBestsellerSince) {
		this.etsyBestsellerSince = etsyBestsellerSince;
	}

	public Instant getEtsyBestsellerEndedAt() {
		return etsyBestsellerEndedAt;
	}

	public void setEtsyBestsellerEndedAt(Instant etsyBestsellerEndedAt) {
		this.etsyBestsellerEndedAt = etsyBestsellerEndedAt;
	}

	public boolean isPmBestseller() {
		return pmBestseller;
	}

	public void setPmBestseller(boolean pmBestseller) {
		this.pmBestseller = pmBestseller;
	}

	public Instant getPmBestsellerSince() {
		return pmBestsellerSince;
	}

	public void setPmBestsellerSince(Instant pmBestsellerSince) {
		this.pmBestsellerSince = pmBestsellerSince;
	}

	public Instant getPmBestsellerEndedAt() {
		return pmBestsellerEndedAt;
	}

	public void setPmBestsellerEndedAt(Instant pmBestsellerEndedAt) {
		this.pmBestsellerEndedAt = pmBestsellerEndedAt;
	}

	public Integer getDeltaFavorers7d() {
		return deltaFavorers7d;
	}

	public void setDeltaFavorers7d(Integer deltaFavorers7d) {
		this.deltaFavorers7d = deltaFavorers7d;
	}

	public Integer getDeltaViews7d() {
		return deltaViews7d;
	}

	public void setDeltaViews7d(Integer deltaViews7d) {
		this.deltaViews7d = deltaViews7d;
	}

	public List<ListingImage> getImages() {
		return images;
	}
}
