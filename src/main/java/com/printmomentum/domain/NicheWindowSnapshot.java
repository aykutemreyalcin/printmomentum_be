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
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "niche_window_snapshot")
@IdClass(NicheWindowSnapshot.NicheWindowSnapshotId.class)
public class NicheWindowSnapshot {

	@Id
	@Column(name = "niche_term_id")
	private Long nicheTermId;

	@Id
	@Column(name = "observed_day")
	private LocalDate observedDay;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "niche_term_id", insertable = false, updatable = false)
	private NicheTerm nicheTerm;

	@Column(name = "window_state", nullable = false, length = 16)
	private String windowState;

	@Column(name = "listing_count", nullable = false)
	private int listingCount;

	@Column(name = "new_entrants_14d", nullable = false)
	private int newEntrants14d;

	@Column(name = "clone_density_7d", nullable = false, precision = 5, scale = 4)
	private BigDecimal cloneDensity7d;

	@Column(name = "break_in_rate", nullable = false, precision = 5, scale = 4)
	private BigDecimal breakInRate;

	@Column(name = "incumbent_age_days", precision = 8, scale = 2)
	private BigDecimal incumbentAgeDays;

	@Column(name = "entrant_momentum", precision = 10, scale = 6)
	private BigDecimal entrantMomentum;

	@Column(name = "etsy_count")
	private Integer etsyCount;

	protected NicheWindowSnapshot() {
	}

	public NicheWindowSnapshot(
			NicheTerm nicheTerm,
			LocalDate observedDay,
			String windowState,
			int listingCount,
			int newEntrants14d,
			BigDecimal cloneDensity7d,
			BigDecimal breakInRate,
			BigDecimal incumbentAgeDays,
			BigDecimal entrantMomentum,
			Integer etsyCount) {
		this.nicheTerm = nicheTerm;
		this.nicheTermId = nicheTerm.getId();
		this.observedDay = observedDay;
		this.windowState = windowState;
		this.listingCount = listingCount;
		this.newEntrants14d = newEntrants14d;
		this.cloneDensity7d = cloneDensity7d;
		this.breakInRate = breakInRate;
		this.incumbentAgeDays = incumbentAgeDays;
		this.entrantMomentum = entrantMomentum;
		this.etsyCount = etsyCount;
	}

	public Long getNicheTermId() {
		return nicheTermId;
	}

	public LocalDate getObservedDay() {
		return observedDay;
	}

	public String getWindowState() {
		return windowState;
	}

	public int getListingCount() {
		return listingCount;
	}

	public int getNewEntrants14d() {
		return newEntrants14d;
	}

	public BigDecimal getCloneDensity7d() {
		return cloneDensity7d;
	}

	public BigDecimal getBreakInRate() {
		return breakInRate;
	}

	public static class NicheWindowSnapshotId implements Serializable {
		private Long nicheTermId;
		private LocalDate observedDay;

		public NicheWindowSnapshotId() {
		}

		public NicheWindowSnapshotId(Long nicheTermId, LocalDate observedDay) {
			this.nicheTermId = nicheTermId;
			this.observedDay = observedDay;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof NicheWindowSnapshotId that)) {
				return false;
			}
			return Objects.equals(nicheTermId, that.nicheTermId) && Objects.equals(observedDay, that.observedDay);
		}

		@Override
		public int hashCode() {
			return Objects.hash(nicheTermId, observedDay);
		}
	}
}
