package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "query_stats")
public class QueryStats {

	@EmbeddedId
	private QueryStatsId id = new QueryStatsId();

	@Column(name = "listing_count", nullable = false)
	private int listingCount;

	@Column(name = "etsy_count")
	private Integer etsyCount;

	@Column(name = "median_price", precision = 12, scale = 2)
	private BigDecimal medianPrice;

	@Column(name = "median_favorers")
	private Integer medianFavorers;

	@Column(name = "median_views")
	private Integer medianViews;

	protected QueryStats() {
	}

	public QueryStats(String query, LocalDate observedDay) {
		this.id = new QueryStatsId(query, observedDay);
	}

	public void apply(QueryStatsCalculator.Row row) {
		this.listingCount = row.listingCount();
		this.etsyCount = row.etsyCount();
		this.medianPrice = row.medianPrice();
		this.medianFavorers = row.medianFavorers();
		this.medianViews = row.medianViews();
	}

	public String getQuery() {
		return id.getQuery();
	}

	public LocalDate getObservedDay() {
		return id.getObservedDay();
	}

	public int getListingCount() {
		return listingCount;
	}

	public Integer getEtsyCount() {
		return etsyCount;
	}

	public BigDecimal getMedianPrice() {
		return medianPrice;
	}

	public Integer getMedianFavorers() {
		return medianFavorers;
	}

	public Integer getMedianViews() {
		return medianViews;
	}
}
