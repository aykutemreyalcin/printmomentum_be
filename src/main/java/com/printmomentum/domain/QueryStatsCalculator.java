package com.printmomentum.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-query peer stats for a crawl day. {@code etsyCount} is Etsy search {@code count}
 * (competition). Medians are over print-tees we kept for that query, not the raw page.
 */
public final class QueryStatsCalculator {

	public record Row(
			int listingCount,
			int etsyCount,
			BigDecimal medianPrice,
			Integer medianFavorers,
			Integer medianViews) {
	}

	public record Signals(BigDecimal price, int numFavorers, Integer views) {
	}

	public Row summarize(int etsyCount, List<Signals> rows) {
		List<Signals> values = rows == null ? List.of() : rows.stream().filter(row -> row != null).toList();
		return new Row(
				values.size(),
				Math.max(etsyCount, 0),
				medianDecimal(values.stream().map(Signals::price).filter(price -> price != null).toList()),
				medianInt(values.stream().map(Signals::numFavorers).toList()),
				medianInt(values.stream().map(Signals::views).filter(views -> views != null).toList()));
	}

	private static Integer medianInt(List<Integer> values) {
		if (values.isEmpty()) {
			return null;
		}
		List<Integer> sorted = new ArrayList<>(values);
		Collections.sort(sorted);
		int mid = sorted.size() / 2;
		if (sorted.size() % 2 == 1) {
			return sorted.get(mid);
		}
		return (int) Math.round((sorted.get(mid - 1) + sorted.get(mid)) / 2.0);
	}

	private static BigDecimal medianDecimal(List<BigDecimal> values) {
		if (values.isEmpty()) {
			return null;
		}
		List<BigDecimal> sorted = new ArrayList<>(values);
		Collections.sort(sorted);
		int mid = sorted.size() / 2;
		if (sorted.size() % 2 == 1) {
			return sorted.get(mid).setScale(2, RoundingMode.HALF_UP);
		}
		return sorted.get(mid - 1).add(sorted.get(mid)).divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP);
	}
}
