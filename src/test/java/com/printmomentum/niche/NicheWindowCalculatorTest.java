package com.printmomentum.niche;

import static org.assertj.core.api.Assertions.assertThat;

import com.printmomentum.config.NicheProperties;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.Shop;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NicheWindowCalculatorTest {

	private NicheWindowCalculator calculator;
	private Instant now;

	@BeforeEach
	void setUp() {
		calculator = new NicheWindowCalculator(defaultProperties(), NicheStopwords.loadDefault());
		now = Instant.parse("2026-08-30T12:00:00Z");
	}

	@Test
	void lowDataWhenCohortTooSmall() {
		Listing listing = listing(1L, "Niche tee one", now.minus(3, ChronoUnit.DAYS));
		NicheWindowCalculator.Metrics metrics = calculator.compute(List.of(listing), now);
		assertThat(metrics.state()).isEqualTo(NicheWindowState.LOW_DATA);
	}

	@Test
	void openWhenFreshEntrantsBreakInWithLowCloneDensity() {
		List<Listing> cohort = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Listing listing = listing(100L + i, "Unique niche alpha listing " + i, now.minus(5, ChronoUnit.DAYS));
			listing.setFirstSeenInTopAt(now.minus(10, ChronoUnit.DAYS));
			listing.setDeltaFavorers7d(4);
			listing.setLastScoreWeekly(BigDecimal.valueOf(0.8));
			cohort.add(listing);
		}
		cohort.get(0).setFirstSeenInTopAt(now.minus(5, ChronoUnit.DAYS));
		cohort.get(1).setFirstSeenInTopAt(now.minus(7, ChronoUnit.DAYS));
		cohort.get(2).setFirstSeenInTopAt(now.minus(12, ChronoUnit.DAYS));
		cohort.get(3).setFirstSeenInTopAt(now.minus(20, ChronoUnit.DAYS));
		cohort.get(4).setFirstSeenInTopAt(now.minus(25, ChronoUnit.DAYS));

		NicheWindowCalculator.Metrics metrics = calculator.compute(cohort, now);
		assertThat(metrics.state()).isIn(NicheWindowState.OPEN, NicheWindowState.CLOSING);
		assertThat(metrics.newEntrants14d()).isGreaterThanOrEqualTo(2);
	}

	private static Listing listing(long id, String title, Instant firstSeenAt) {
		Shop shop = new Shop(1L, "Shop", "https://etsy.com/shop/shop");
		Listing listing = new Listing(id, shop, title, "https://etsy.com/listing/" + id);
		listing.setPrintTee(true);
		listing.setFirstSeenAt(firstSeenAt);
		listing.setLastSeenAt(firstSeenAt);
		return listing;
	}

	private static NicheProperties defaultProperties() {
		return new NicheProperties(
				0.25,
				3,
				2,
				0.65,
				5,
				20,
				2,
				0.40,
				0.35,
				0.35,
				0.40,
				30,
				21,
				0.15);
	}
}
