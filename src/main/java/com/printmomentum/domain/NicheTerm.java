package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "niche_term")
public class NicheTerm {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String slug;

	@Column(nullable = false, unique = true, length = 191)
	private String label;

	@Column(name = "listing_count", nullable = false)
	private int listingCount;

	@Column(name = "first_seen_at", nullable = false)
	private Instant firstSeenAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "etsy_count")
	private Integer etsyCount;

	@Column(name = "etsy_checked_at")
	private Instant etsyCheckedAt;

	@Column(name = "window_state", nullable = false, length = 16)
	private String windowState = "LOW_DATA";

	@Column(name = "new_entrants_14d", nullable = false)
	private int newEntrants14d;

	@Column(name = "clone_density_7d", nullable = false, precision = 5, scale = 4)
	private BigDecimal cloneDensity7d = BigDecimal.ZERO;

	@Column(name = "break_in_rate", nullable = false, precision = 5, scale = 4)
	private BigDecimal breakInRate = BigDecimal.ZERO;

	@Column(name = "incumbent_age_days", precision = 8, scale = 2)
	private BigDecimal incumbentAgeDays;

	@Column(name = "entrant_momentum", precision = 10, scale = 6)
	private BigDecimal entrantMomentum;

	@Column(name = "window_computed_at")
	private Instant windowComputedAt;

	protected NicheTerm() {
	}

	public NicheTerm(String slug, String label, Instant seenAt) {
		this.slug = slug;
		this.label = label;
		this.listingCount = 0;
		this.firstSeenAt = seenAt;
		this.lastSeenAt = seenAt;
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public String getLabel() {
		return label;
	}

	public int getListingCount() {
		return listingCount;
	}

	public void setListingCount(int listingCount) {
		this.listingCount = listingCount;
	}

	public Instant getFirstSeenAt() {
		return firstSeenAt;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void touch(Instant seenAt) {
		this.lastSeenAt = seenAt;
	}

	public Integer getEtsyCount() {
		return etsyCount;
	}

	public void setEtsyCount(Integer etsyCount) {
		this.etsyCount = etsyCount;
	}

	public Instant getEtsyCheckedAt() {
		return etsyCheckedAt;
	}

	public void setEtsyCheckedAt(Instant etsyCheckedAt) {
		this.etsyCheckedAt = etsyCheckedAt;
	}

	public String getWindowState() {
		return windowState;
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

	public BigDecimal getIncumbentAgeDays() {
		return incumbentAgeDays;
	}

	public BigDecimal getEntrantMomentum() {
		return entrantMomentum;
	}

	public Instant getWindowComputedAt() {
		return windowComputedAt;
	}

	public void applyWindow(
			String windowState,
			int newEntrants14d,
			BigDecimal cloneDensity7d,
			BigDecimal breakInRate,
			BigDecimal incumbentAgeDays,
			BigDecimal entrantMomentum,
			Instant computedAt) {
		this.windowState = windowState;
		this.newEntrants14d = newEntrants14d;
		this.cloneDensity7d = cloneDensity7d;
		this.breakInRate = breakInRate;
		this.incumbentAgeDays = incumbentAgeDays;
		this.entrantMomentum = entrantMomentum;
		this.windowComputedAt = computedAt;
	}
}
