package com.printmomentum.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Five-line Printify briefing. Facts only: days-to-top, estimated sales, bestseller
 * date, tag pack, price vs the listing's primary query median.
 */
public final class ListingTakeaway {

	private static final int MAX_LINES = 5;
	private static final int TAG_PACK = 5;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)
			.withZone(ZoneOffset.UTC);

	public record Input(
			Double daysToTop,
			Double estSales30d,
			boolean etsyBestseller,
			Instant etsyBestsellerSince,
			boolean pmBestseller,
			List<String> tags,
			BigDecimal price,
			BigDecimal medianPrice,
			String query) {
	}

	public List<String> lines(Input input) {
		if (input == null) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		if (input.daysToTop() != null) {
			lines.add("Entered our top-N in " + formatDays(input.daysToTop()) + " days.");
		}
		if (input.estSales30d() != null) {
			lines.add("Est. " + formatCount(input.estSales30d()) + " sales / 30d (reviews30d / 0.10).");
		}
		if (input.etsyBestseller() && input.etsyBestsellerSince() != null) {
			lines.add("Bestseller since " + DATE.format(input.etsyBestsellerSince()) + ".");
		} else if (input.pmBestseller()) {
			lines.add("Likely bestseller (PM signal from reviews).");
		}
		List<String> tags = input.tags() == null ? List.of() : input.tags().stream().filter(tag -> tag != null && !tag.isBlank()).toList();
		if (!tags.isEmpty()) {
			List<String> pack = tags.size() > TAG_PACK ? tags.subList(0, TAG_PACK) : tags;
			lines.add("Tag pack: " + String.join(", ", pack) + ".");
		}
		String vsMedian = priceVsMedian(input.price(), input.medianPrice(), input.query());
		if (vsMedian != null) {
			lines.add(vsMedian);
		}
		return List.copyOf(lines.size() > MAX_LINES ? lines.subList(0, MAX_LINES) : lines);
	}

	private static String priceVsMedian(BigDecimal price, BigDecimal median, String query) {
		if (price == null || median == null || median.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		BigDecimal pct = price.subtract(median)
				.divide(median, 4, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.setScale(0, RoundingMode.HALF_UP);
		int n = pct.intValue();
		String where = query == null || query.isBlank() ? "the query median" : "the “" + query + "” median";
		if (n < 0) {
			return "Price is " + Math.abs(n) + "% below " + where + ".";
		}
		if (n > 0) {
			return "Price is " + n + "% above " + where + ".";
		}
		return "Price matches " + where + ".";
	}

	private static String formatDays(double days) {
		return BigDecimal.valueOf(days).setScale(1, RoundingMode.HALF_UP).toPlainString();
	}

	private static String formatCount(double value) {
		return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
	}
}
