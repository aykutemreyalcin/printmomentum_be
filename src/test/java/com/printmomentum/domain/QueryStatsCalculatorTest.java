package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryStatsCalculatorTest {

	private final QueryStatsCalculator calculator = new QueryStatsCalculator();

	@Test
	void mediansIgnoreNullPriceAndViews() {
		QueryStatsCalculator.Row row = calculator.summarize(
				1200,
				List.of(
						new QueryStatsCalculator.Signals(new BigDecimal("10.00"), 4, 100),
						new QueryStatsCalculator.Signals(new BigDecimal("20.00"), 8, null),
						new QueryStatsCalculator.Signals(new BigDecimal("30.00"), 12, 300)));
		assertThat(row.listingCount()).isEqualTo(3);
		assertThat(row.etsyCount()).isEqualTo(1200);
		assertThat(row.medianPrice()).isEqualByComparingTo("20.00");
		assertThat(row.medianFavorers()).isEqualTo(8);
		assertThat(row.medianViews()).isEqualTo(200);
	}
}
