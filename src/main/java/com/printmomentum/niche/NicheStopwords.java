package com.printmomentum.niche;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

public final class NicheStopwords {

	private final Set<String> stopwords;
	private final Set<String> productSuffixes;
	private final Set<String> singleWordAllowlist;

	private NicheStopwords(Set<String> stopwords, Set<String> productSuffixes, Set<String> singleWordAllowlist) {
		this.stopwords = stopwords;
		this.productSuffixes = productSuffixes;
		this.singleWordAllowlist = singleWordAllowlist;
	}

	public static NicheStopwords loadDefault() {
		try (InputStream input = NicheStopwords.class.getResourceAsStream("/printmomentum/niche-stopwords.yml")) {
			if (input == null) {
				throw new IllegalStateException("missing niche-stopwords.yml");
			}
			@SuppressWarnings("unchecked")
			var root = (java.util.Map<String, Object>) new Yaml().load(input);
			return new NicheStopwords(
					toSet(root.get("stopwords")),
					toSet(root.get("productSuffixes")),
					toSet(root.get("singleWordAllowlist")));
		} catch (Exception ex) {
			throw new IllegalStateException("failed to load niche-stopwords.yml", ex);
		}
	}

	public boolean isStopword(String token) {
		return stopwords.contains(normalizeToken(token));
	}

	public boolean isProductSuffix(String token) {
		return productSuffixes.contains(normalizeToken(token));
	}

	public boolean allowsSingleWord(String token) {
		return singleWordAllowlist.contains(normalizeToken(token));
	}

	public String stripProductSuffixes(String phrase) {
		if (phrase == null || phrase.isBlank()) {
			return "";
		}
		String[] parts = phrase.trim().toLowerCase(Locale.ROOT).split("\\s+");
		int end = parts.length;
		while (end > 0 && productSuffixes.contains(parts[end - 1])) {
			end--;
		}
		if (end <= 0) {
			return "";
		}
		return String.join(" ", java.util.Arrays.copyOf(parts, end));
	}

	public Set<String> tokenize(String text) {
		if (text == null || text.isBlank()) {
			return Set.of();
		}
		String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s'-]", " ");
		String[] raw = cleaned.split("\\s+");
		Set<String> tokens = new HashSet<>();
		for (String token : raw) {
			String normalized = normalizeToken(token);
			if (!normalized.isBlank() && !isStopword(normalized) && !isNumericOrSize(normalized)) {
				tokens.add(normalized);
			}
		}
		return tokens;
	}

	private static boolean isNumericOrSize(String token) {
		return token.matches("\\d+") || token.matches("x+[sl]?") || token.matches("size\\d*");
	}

	private static String normalizeToken(String token) {
		if (token == null) {
			return "";
		}
		return token.trim().toLowerCase(Locale.ROOT).replaceAll("^'+|'+$", "");
	}

	private static Set<String> toSet(Object value) {
		if (!(value instanceof List<?> list)) {
			return Set.of();
		}
		Set<String> out = new HashSet<>();
		for (Object item : list) {
			if (item != null) {
				out.add(item.toString().trim().toLowerCase(Locale.ROOT));
			}
		}
		return Collections.unmodifiableSet(out);
	}
}
