package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "listing_niche_term")
@IdClass(ListingNicheTerm.ListingNicheTermId.class)
public class ListingNicheTerm {

	@Id
	@Column(name = "listing_id")
	private Long listingId;

	@Id
	@Column(name = "niche_term_id")
	private Long nicheTermId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "listing_id", insertable = false, updatable = false)
	private Listing listing;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "niche_term_id", insertable = false, updatable = false)
	private NicheTerm nicheTerm;

	@Column(nullable = false, precision = 4, scale = 3)
	private BigDecimal weight;

	@Column(nullable = false, length = 16)
	private String source;

	protected ListingNicheTerm() {
	}

	public ListingNicheTerm(long listingId, NicheTerm nicheTerm, BigDecimal weight, String source) {
		this.listingId = listingId;
		this.nicheTermId = nicheTerm.getId();
		this.nicheTerm = nicheTerm;
		this.weight = weight;
		this.source = source;
	}

	public Long getListingId() {
		return listingId;
	}

	public Long getNicheTermId() {
		return nicheTermId;
	}

	public NicheTerm getNicheTerm() {
		return nicheTerm;
	}

	public BigDecimal getWeight() {
		return weight;
	}

	public String getSource() {
		return source;
	}

	public static class ListingNicheTermId implements Serializable {
		private Long listingId;
		private Long nicheTermId;

		public ListingNicheTermId() {
		}

		public ListingNicheTermId(Long listingId, Long nicheTermId) {
			this.listingId = listingId;
			this.nicheTermId = nicheTermId;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof ListingNicheTermId that)) {
				return false;
			}
			return Objects.equals(listingId, that.listingId) && Objects.equals(nicheTermId, that.nicheTermId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(listingId, nicheTermId);
		}
	}
}
