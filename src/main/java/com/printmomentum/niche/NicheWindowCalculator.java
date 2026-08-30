package com.printmomentum.niche;

import com.printmomentum.config.NicheProperties;
import com.printmomentum.domain.Listing;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NicheWindowCalculator {

	public record Metrics(
			NicheWindowState state,
			int listingCount,
			int newEntrants14d,
			double cloneDensity7d,
			double breakInRate,
			Double incumbentAgeDays,
			Double entrantMomentum) {
	}

	private final NicheProperties properties;
	private final NicheStopwords stopwords;

	public NicheWindowCalculator(NicheProperties properties, NicheStopwords stopwords) {
		this.properties = properties;
		this.stopwords = stopwords;
	}

	public Metrics compute(List<Listing> cohort, Instant now) {
		if (cohort == null || cohort.isEmpty()) {
			return new Metrics(NicheWindowState.LOW_DATA, 0, 0, 0, 0, null, null);
		}
		List<Listing> ranked = cohort.stream()
				.sorted(Comparator.comparing(
						(Listing listing) -> listing.getLastScoreWeekly() == null
								? 0.0
								: listing.getLastScoreWeekly().doubleValue(),
						Comparator.reverseOrder()))
				.limit(30)
				.toList();
		int listingCount = ranked.size();
		if (listingCount <= properties.lowDataListingCount()) {
			return new Metrics(NicheWindowState.LOW_DATA, listingCount, 0, 0, 0, null, null);
		}
		Instant entrants14dCutoff = now.minus(Duration.ofDays(14));
		Instant entrants30dCutoff = now.minus(Duration.ofDays(30));
		int newEntrants14d = 0;
		int breakInCandidates = 0;
		int breakInHits = 0;
		double incumbentAgeSum = 0;
		int incumbentCount = 0;
		double entrantMomentumSum = 0;
		int entrantMomentumCount = 0;
		for (Listing listing : ranked) {
			Instant firstTop = listing.getFirstSeenInTopAt();
			if (firstTop != null && !firstTop.isBefore(entrants14dCutoff)) {
				newEntrants14d++;
			}
			if (firstTop != null && !firstTop.isBefore(entrants30dCutoff)) {
				breakInCandidates++;
				if (listing.getDeltaFavorers7d() != null && listing.getDeltaFavorers7d() > 0) {
					breakInHits++;
				}
				if (listing.getLastScoreWeekly() != null) {
					entrantMomentumSum += listing.getLastScoreWeekly().doubleValue();
					entrantMomentumCount++;
				}
			}
			if (firstTop != null) {
				incumbentAgeSum += Duration.between(firstTop, now).toMillis() / 86_400_000.0;
				incumbentCount++;
			}
		}
		double breakInRate = breakInCandidates == 0 ? 0 : breakInHits / (double) breakInCandidates;
		Double incumbentAgeDays = incumbentCount == 0 ? null : incumbentAgeSum / incumbentCount;
		Double entrantMomentum = entrantMomentumCount == 0 ? null : entrantMomentumSum / entrantMomentumCount;
		double cloneDensity = cloneDensity7d(cohort, now);
		NicheWindowState state = classify(newEntrants14d, breakInRate, cloneDensity, incumbentAgeDays, ranked, now);
		return new Metrics(state, listingCount, newEntrants14d, cloneDensity, breakInRate, incumbentAgeDays, entrantMomentum);
	}

	private NicheWindowState classify(
			int newEntrants14d,
			double breakInRate,
			double cloneDensity,
			Double incumbentAgeDays,
			List<Listing> ranked,
			Instant now) {
		long newEntrants30d = ranked.stream()
				.map(Listing::getFirstSeenInTopAt)
				.filter(firstTop -> firstTop != null && !firstTop.isBefore(now.minus(Duration.ofDays(properties.closedNewEntrantsDays()))))
				.count();
		if (newEntrants14d >= properties.openNewEntrantsMin()
				&& breakInRate >= properties.openBreakInRateMin()
				&& cloneDensity < properties.openCloneDensityMax()) {
			return NicheWindowState.OPEN;
		}
		if ((newEntrants30d == 0
						&& incumbentAgeDays != null
						&& incumbentAgeDays >= properties.closedIncumbentAgeDays())
				|| breakInRate <= properties.closedBreakInRateMax()) {
			return NicheWindowState.CLOSED;
		}
		if (newEntrants14d >= 1
				&& (cloneDensity >= properties.closingCloneDensityMin() || breakInRate < properties.closingBreakInRateMax())) {
			return NicheWindowState.CLOSING;
		}
		if (newEntrants14d >= properties.openNewEntrantsMin()) {
			return NicheWindowState.OPEN;
		}
		return NicheWindowState.CLOSING;
	}

	private double cloneDensity7d(List<Listing> cohort, Instant now) {
		Instant cutoff = now.minus(Duration.ofDays(7));
		List<Listing> recent = cohort.stream()
				.filter(listing -> listing.getFirstSeenAt() != null && !listing.getFirstSeenAt().isBefore(cutoff))
				.toList();
		if (recent.size() < 2) {
			return 0;
		}
		int similarPairs = 0;
		int comparisons = 0;
		for (int i = 0; i < recent.size(); i++) {
			Set<String> left = titleTokens(recent.get(i).getTitle());
			if (left.isEmpty()) {
				continue;
			}
			for (int j = i + 1; j < recent.size(); j++) {
				Set<String> right = titleTokens(recent.get(j).getTitle());
				if (right.isEmpty()) {
					continue;
				}
				comparisons++;
				if (jaccard(left, right) >= properties.cloneSimilarityThreshold()) {
					similarPairs++;
				}
			}
		}
		return comparisons == 0 ? 0 : similarPairs / (double) comparisons;
	}

	private Set<String> titleTokens(String title) {
		return stopwords.tokenize(title == null ? "" : title);
	}

	private static double jaccard(Set<String> left, Set<String> right) {
		Set<String> intersection = new HashSet<>(left);
		intersection.retainAll(right);
		Set<String> union = new HashSet<>(left);
		union.addAll(right);
		return union.isEmpty() ? 0 : intersection.size() / (double) union.size();
	}
}
