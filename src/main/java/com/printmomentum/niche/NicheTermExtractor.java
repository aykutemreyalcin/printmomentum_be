package com.printmomentum.niche;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NicheTermExtractor {

	public record ExtractedTerm(String label, double weight, String source) {
	}

	private final NicheStopwords stopwords;

	public NicheTermExtractor(NicheStopwords stopwords) {
		this.stopwords = stopwords;
	}

	public List<ExtractedTerm> extract(String title, List<String> tags) {
		List<Candidate> candidates = new ArrayList<>();
		if (tags != null) {
			for (String tag : tags) {
				addCandidates(candidates, tag, "tag", 3.0);
			}
		}
		if (title != null && !title.isBlank()) {
			addCandidates(candidates, title, "title", 1.0);
		}
		candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());
		LinkedHashSet<String> seen = new LinkedHashSet<>();
		List<ExtractedTerm> results = new ArrayList<>();
		for (Candidate candidate : candidates) {
			if (seen.add(candidate.label()) && results.size() < 3) {
				results.add(new ExtractedTerm(candidate.label(), candidate.weight(), candidate.source()));
			}
		}
		return results;
	}

	private void addCandidates(List<Candidate> candidates, String raw, String source, double sourceBoost) {
		String normalized = normalizePhrase(raw);
		if (normalized.isBlank()) {
			return;
		}
		for (String phrase : ngrams(normalized)) {
			String canonical = canonicalize(phrase);
			if (canonical.isBlank() || isGenericOnly(canonical)) {
				continue;
			}
			double score = sourceBoost + wordCountBonus(canonical) + tagExactBonus(source, canonical, normalized);
			candidates.add(new Candidate(canonical, Math.min(score / 10.0, 1.0), source, score));
		}
	}

	private double tagExactBonus(String source, String canonical, String normalized) {
		if ("tag".equals(source) && canonical.equals(normalized)) {
			return 2.0;
		}
		return 0.0;
	}

	private double wordCountBonus(String phrase) {
		int words = phrase.split("\\s+").length;
		return switch (words) {
			case 2, 3 -> 2.0;
			case 4 -> 1.0;
			case 1 -> stopwords.allowsSingleWord(phrase) ? 1.0 : -2.0;
			default -> 0.0;
		};
	}

	private String canonicalize(String phrase) {
		String stripped = stopwords.stripProductSuffixes(phrase);
		if (stripped.isBlank()) {
			return "";
		}
		String[] parts = stripped.split("\\s+");
		List<String> tokens = new ArrayList<>();
		for (String part : parts) {
			String normalized = part.trim().toLowerCase(Locale.ROOT);
			if (normalized.isBlank()
					|| stopwords.isStopword(normalized)
					|| stopwords.isProductSuffix(normalized)) {
				continue;
			}
			tokens.add(normalized);
		}
		if (tokens.isEmpty()) {
			return "";
		}
		return String.join(" ", tokens);
	}

	private List<String> ngrams(String phrase) {
		String[] parts = phrase.split("\\s+");
		List<String> out = new ArrayList<>();
		for (int size = Math.min(4, parts.length); size >= 1; size--) {
			for (int i = 0; i + size <= parts.length; i++) {
				out.add(String.join(" ", java.util.Arrays.copyOfRange(parts, i, i + size)));
			}
		}
		return out;
	}

	private boolean isGenericOnly(String phrase) {
		String[] parts = phrase.split("\\s+");
		if (parts.length == 1) {
			return stopwords.isStopword(parts[0])
					|| stopwords.isProductSuffix(parts[0])
					|| (!stopwords.allowsSingleWord(parts[0]) && parts[0].length() < 3);
		}
		for (String part : parts) {
			if (!stopwords.isStopword(part) && !stopwords.isProductSuffix(part)) {
				return false;
			}
		}
		return true;
	}

	private static String normalizePhrase(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.trim()
				.toLowerCase(Locale.ROOT)
				.replace('&', ' ')
				.replaceAll("[^a-z0-9\\s'-]", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private record Candidate(String label, double weight, String source, double score) {
	}
}
