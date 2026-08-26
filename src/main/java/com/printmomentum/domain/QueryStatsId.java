package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class QueryStatsId implements Serializable {

	@Column(nullable = false, length = 191)
	private String query;

	@Column(name = "observed_day", nullable = false)
	private LocalDate observedDay;

	protected QueryStatsId() {
	}

	public QueryStatsId(String query, LocalDate observedDay) {
		this.query = query;
		this.observedDay = observedDay;
	}

	public String getQuery() {
		return query;
	}

	public LocalDate getObservedDay() {
		return observedDay;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof QueryStatsId that)) {
			return false;
		}
		return Objects.equals(query, that.query) && Objects.equals(observedDay, that.observedDay);
	}

	@Override
	public int hashCode() {
		return Objects.hash(query, observedDay);
	}
}
