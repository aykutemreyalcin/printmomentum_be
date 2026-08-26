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
@Table(name = "listing_event")
public class ListingEvent {

	public static final String ETSY_BESTSELLER_ON = "ETSY_BESTSELLER_ON";
	public static final String ETSY_BESTSELLER_OFF = "ETSY_BESTSELLER_OFF";
	public static final String PM_BESTSELLER_ON = "PM_BESTSELLER_ON";
	public static final String PM_BESTSELLER_OFF = "PM_BESTSELLER_OFF";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id", nullable = false)
	private Listing listing;

	@Column(nullable = false, length = 32)
	private String kind;

	@Column(nullable = false)
	private Instant observedAt;

	protected ListingEvent() {
	}

	public ListingEvent(Listing listing, String kind, Instant observedAt) {
		this.listing = listing;
		this.kind = kind;
		this.observedAt = observedAt;
	}

	public Long getId() {
		return id;
	}

	public String getKind() {
		return kind;
	}

	public Instant getObservedAt() {
		return observedAt;
	}
}
