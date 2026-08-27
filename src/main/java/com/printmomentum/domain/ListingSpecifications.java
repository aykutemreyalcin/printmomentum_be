package com.printmomentum.domain;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class ListingSpecifications {

	private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

	private ListingSpecifications() {
	}

	public static Specification<Listing> printTeeFeed(
			BigDecimal minScore, String q, Long shopId, String preset, Boolean bestseller, Instant now, MomentumPeriod period) {
		MomentumPeriod active = period == null ? MomentumPeriod.WEEKLY : period;
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.isTrue(root.get("printTee")));
			if (minScore != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get(active.sortField()), minScore));
			}
			if (q != null && !q.isBlank()) {
				String pattern = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
				predicates.add(cb.like(cb.lower(root.get("title")), pattern));
			}
			if (shopId != null) {
				Join<Listing, Shop> shop = root.join("shop");
				predicates.add(cb.equal(shop.get("shopId"), shopId));
			}
			Instant moment = now == null ? Instant.now() : now;
			addPreset(predicates, root, cb, preset, moment);
			if (Boolean.TRUE.equals(bestseller)) {
				predicates.add(cb.or(cb.isTrue(root.get("etsyBestseller")), cb.isTrue(root.get("pmBestseller"))));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static void addPreset(
			List<Predicate> predicates,
			jakarta.persistence.criteria.Root<Listing> root,
			jakarta.persistence.criteria.CriteriaBuilder cb,
			String preset,
			Instant now) {
		if (preset == null || preset.isBlank()) {
			return;
		}
		LocalDate today = now.atZone(ISTANBUL).toLocalDate();
		Instant startOfToday = today.atStartOfDay(ISTANBUL).toInstant();
		Instant startOfTomorrow = today.plusDays(1).atStartOfDay(ISTANBUL).toInstant();
		switch (preset) {
			case "seen-today" -> {
				predicates.add(cb.greaterThanOrEqualTo(root.get("firstSeenAt"), startOfToday));
				predicates.add(cb.lessThan(root.get("firstSeenAt"), startOfTomorrow));
			}
			case "created-today" -> {
				predicates.add(cb.greaterThanOrEqualTo(createdAt(root, cb), startOfToday));
				predicates.add(cb.lessThan(createdAt(root, cb), startOfTomorrow));
			}
			case "created-7d" -> predicates.add(cb.greaterThanOrEqualTo(createdAt(root, cb), now.minusSeconds(7L * 24 * 3600)));
			case "reviewed-24h" -> predicates.add(cb.greaterThanOrEqualTo(root.get("lastReviewAt"), now.minusSeconds(24L * 3600)));
			case "bestseller" -> predicates.add(cb.or(cb.isTrue(root.get("etsyBestseller")), cb.isTrue(root.get("pmBestseller"))));
			default -> {
			}
		}
	}

	private static jakarta.persistence.criteria.Expression<Instant> createdAt(
			jakarta.persistence.criteria.Root<Listing> root, jakarta.persistence.criteria.CriteriaBuilder cb) {
		return cb.coalesce(root.get("originalCreatedAt"), root.get("etsyCreatedAt"));
	}
}
