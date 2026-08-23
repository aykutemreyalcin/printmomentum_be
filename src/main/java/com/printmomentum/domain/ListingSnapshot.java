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
import java.time.Instant;

@Entity
@Table(name = "listing_snapshot")
public class ListingSnapshot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id", nullable = false)
	private Listing listing;

	@Column(name = "crawl_run_id", nullable = false, length = 36)
	private String crawlRunId;

	@Column(nullable = false)
	private Instant observedAt;

	@Column(nullable = false)
	private int position;

	@Column(nullable = false)
	private int numFavorers;

	protected ListingSnapshot() {
	}

	public ListingSnapshot(Listing listing, String crawlRunId, Instant observedAt, int position, int numFavorers) {
		this.listing = listing;
		this.crawlRunId = crawlRunId;
		this.observedAt = observedAt;
		this.position = position;
		this.numFavorers = numFavorers;
	}

	public Long getId() {
		return id;
	}

	public Listing getListing() {
		return listing;
	}

	public String getCrawlRunId() {
		return crawlRunId;
	}

	public Instant getObservedAt() {
		return observedAt;
	}

	public int getPosition() {
		return position;
	}

	public int getNumFavorers() {
		return numFavorers;
	}
}
