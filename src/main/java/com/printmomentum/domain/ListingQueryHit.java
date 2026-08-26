package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "listing_query")
public class ListingQueryHit {

	@EmbeddedId
	private ListingQueryHitId id = new ListingQueryHitId();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("listingId")
	@JoinColumn(name = "listing_id", nullable = false)
	private Listing listing;

	@Column(nullable = false)
	private int position;

	@Column(name = "crawl_run_id", nullable = false, length = 36)
	private String crawlRunId;

	@Column(nullable = false)
	private Instant observedAt;

	protected ListingQueryHit() {
	}

	public ListingQueryHit(Listing listing, String query, LocalDate observedDay, int position, String crawlRunId, Instant observedAt) {
		this.listing = listing;
		this.id = new ListingQueryHitId(listing.getListingId(), query, observedDay);
		this.position = position;
		this.crawlRunId = crawlRunId;
		this.observedAt = observedAt;
	}

	public Listing getListing() {
		return listing;
	}

	public Long getListingId() {
		return id.getListingId();
	}

	public ListingQueryHitId getId() {
		return id;
	}

	public String getQuery() {
		return id.getQuery();
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public void setCrawlRunId(String crawlRunId) {
		this.crawlRunId = crawlRunId;
	}

	public void setObservedAt(Instant observedAt) {
		this.observedAt = observedAt;
	}
}
