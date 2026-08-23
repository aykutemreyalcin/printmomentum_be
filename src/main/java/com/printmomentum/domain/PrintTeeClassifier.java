package com.printmomentum.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Scored print-tee filter (not a single keyword). Apparel (tee language or t-shirt taxonomy)
 * plus print-family signals (print, graphic, DTG, sublimation, mockup) raise the score;
 * vintage / used / embroidery / blank wholesale / hoodie / non-apparel zero it.
 * <p>
 * Formula: {@code min(1, apparelWeight + sum(matched includeWeights))}, then 0 if any exclude
 * category matches. {@code is_print_tee} at {@code score >= threshold} (default 0.7).
 * <p>
 * Golden fixture precision: 8 include + 7 exclude = 15/15 correct at 0.7 (precision 1.0 on
 * {@code printtee/golden.json}).
 */
public class PrintTeeClassifier {

	private final PrintTeeRules rules;

	public PrintTeeClassifier(PrintTeeRules rules) {
		this.rules = rules;
	}

	public static PrintTeeClassifier loadDefault() {
		return new PrintTeeClassifier(PrintTeeRules.fromClasspath());
	}

	public PrintTeeClassification classify(String title, String description, List<String> tags, Long taxonomyId) {
		String haystack = normalize(join(title, description, tags));
		List<String> rejectReasons = new ArrayList<>();

		for (var entry : rules.excludeKeywords().entrySet()) {
			if (matchesAny(haystack, entry.getValue())) {
				rejectReasons.add(entry.getKey());
			}
		}
		if (!rejectReasons.isEmpty()) {
			return new PrintTeeClassification(0.0, false, rejectReasons);
		}

		double score = 0.0;
		if (isApparel(haystack, taxonomyId)) {
			score += rules.apparelWeight();
		}
		for (var entry : rules.includeKeywords().entrySet()) {
			String category = entry.getKey();
			if ("tee".equals(category)) {
				continue;
			}
			Double weight = rules.includeWeights().get(category);
			if (weight != null && matchesAny(haystack, entry.getValue())) {
				score += weight;
			}
		}

		double rounded = roundScore(Math.min(1.0, Math.max(0.0, score)));
		boolean printTee = rounded >= rules.threshold();
		if (!printTee) {
			rejectReasons.add("below_threshold");
		}
		return new PrintTeeClassification(rounded, printTee, rejectReasons);
	}

	private boolean isApparel(String haystack, Long taxonomyId) {
		if (taxonomyId != null && rules.tshirtTaxonomyIds().contains(taxonomyId)) {
			return true;
		}
		List<String> teeKeywords = rules.includeKeywords().getOrDefault("tee", List.of());
		return matchesAny(haystack, teeKeywords);
	}

	private static boolean matchesAny(String haystack, List<String> keywords) {
		for (String keyword : keywords) {
			if (keyword == null || keyword.isBlank()) {
				continue;
			}
			if (containsPhrase(haystack, normalize(keyword))) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsPhrase(String haystack, String phrase) {
		if (phrase.isEmpty()) {
			return false;
		}
		String regex = "\\b" + Pattern.quote(phrase) + "\\b";
		return Pattern.compile(regex).matcher(haystack).find();
	}

	private static String join(String title, String description, List<String> tags) {
		StringBuilder text = new StringBuilder();
		append(text, title);
		append(text, description);
		if (tags != null) {
			for (String tag : tags) {
				append(text, tag);
			}
		}
		return text.toString();
	}

	private static void append(StringBuilder text, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		if (!text.isEmpty()) {
			text.append(' ');
		}
		text.append(value);
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ").trim();
	}

	private static double roundScore(double score) {
		return BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP).doubleValue();
	}
}
