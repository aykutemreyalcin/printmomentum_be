package com.printmomentum.domain;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ListingSpecifications {

	private ListingSpecifications() {
	}

	public static Specification<Listing> printTeeFeed(BigDecimal minScore, String q) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.isTrue(root.get("printTee")));
			if (minScore != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("lastScore"), minScore));
			}
			if (q != null && !q.isBlank()) {
				String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.like(cb.lower(root.get("title")), pattern));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}
}
