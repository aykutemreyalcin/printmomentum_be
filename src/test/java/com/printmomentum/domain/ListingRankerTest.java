package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ListingRankerTest {

	private static final Instant SCORED_AT = Instant.parse("2026-08-23T00:00:00Z");

	private final ListingRanker ranker = new ListingRanker();

	@Test
	void twoDayClimbBeatsThirtyDayOccupant() {
		Instant createdFast = SCORED_AT.minus(Duration.ofDays(10));
		Instant topInTwoDays = createdFast.plus(Duration.ofDays(2));

		Instant createdOld = SCORED_AT.minus(Duration.ofDays(400));
		Instant inTopThirtyDays = SCORED_AT.minus(Duration.ofDays(30));

		double fast = ranker.score(createdFast, topInTwoDays, SCORED_AT, 0);
		double occupant = ranker.score(createdOld, inTopThirtyDays, SCORED_AT, 10_000);

		assertThat(fast).isGreaterThan(occupant);
		assertFinite(fast);
		assertFinite(occupant);
	}

	@Test
	void largerFavorersDeltaBreaksTies() {
		Instant created = SCORED_AT.minus(Duration.ofDays(10));
		Instant firstTop = created.plus(Duration.ofDays(2));

		double smallerDelta = ranker.score(created, firstTop, SCORED_AT, 3);
		double largerDelta = ranker.score(created, firstTop, SCORED_AT, 30);

		assertThat(largerDelta).isGreaterThan(smallerDelta);
		assertFinite(largerDelta);
		assertFinite(smallerDelta);
	}

	@Test
	void zeroDayDurationsDoNotDivideByZero() {
		double score = ranker.score(SCORED_AT, SCORED_AT, SCORED_AT, 0);

		assertFinite(score);
		assertThat(score).isEqualTo(1.0 / ListingRanker.MIN_DAYS + 1.0 / ListingRanker.MIN_DAYS);
	}

	@Test
	void missingClockFieldsScoreZero() {
		assertThat(ranker.score(null, SCORED_AT, SCORED_AT, 1)).isZero();
		assertThat(ranker.score(SCORED_AT, null, SCORED_AT, 1)).isZero();
		assertThat(ranker.score(SCORED_AT, SCORED_AT, null, 1)).isZero();
	}

	@Test
	void trendScoreRewardsEngagementAndPositionClimb() {
		double quiet = ranker.scoreTrend(new ListingRanker.TrendInput(0, 0, 0, 0, 7));
		double growing = ranker.scoreTrend(new ListingRanker.TrendInput(20, 200, 30, 5, 7));
		assertThat(growing).isGreaterThan(quiet);
		assertFinite(growing);
	}

	private static void assertFinite(double score) {
		assertThat(score).isFinite();
	}
}
