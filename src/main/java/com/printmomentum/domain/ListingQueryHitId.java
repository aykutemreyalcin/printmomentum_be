package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class ListingQueryHitId implements Serializable {

	@Column(name = "listing_id", nullable = false)
	private Long listingId;

	@Column(nullable = false, length = 191)
	private String query;

	@Column(name = "observed_day", nullable = false)
	private LocalDate observedDay;

	protected ListingQueryHitId() {
	}

	public ListingQueryHitId(Long listingId, String query, LocalDate observedDay) {
		this.listingId = listingId;
		this.query = query;
		this.observedDay = observedDay;
	}

	public Long getListingId() {
		return listingId;
	}

	public String getQuery() {
		return query;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ListingQueryHitId that)) {
			return false;
		}
		return Objects.equals(listingId, that.listingId)
				&& Objects.equals(query, that.query)
				&& Objects.equals(observedDay, that.observedDay);
	}

	@Override
	public int hashCode() {
		return Objects.hash(listingId, query, observedDay);
	}
}
