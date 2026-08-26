package com.printmomentum.config;

import com.printmomentum.domain.HistoryRetention;
import com.printmomentum.domain.ListingEstimator;
import com.printmomentum.domain.ListingRanker;
import com.printmomentum.domain.ListingTakeaway;
import com.printmomentum.domain.ListingTimeline;
import com.printmomentum.domain.PrintTeeClassifier;
import com.printmomentum.domain.QueryStatsCalculator;
import com.printmomentum.domain.ReviewWindow;
import com.printmomentum.domain.SnapshotDeltas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(IngestProperties.class)
public class IngestConfig {

	@Bean
	PrintTeeClassifier printTeeClassifier() {
		return PrintTeeClassifier.loadDefault();
	}

	@Bean
	ListingRanker listingRanker() {
		return new ListingRanker();
	}

	@Bean
	ListingEstimator listingEstimator() {
		return new ListingEstimator();
	}

	@Bean
	SnapshotDeltas snapshotDeltas() {
		return new SnapshotDeltas();
	}

	@Bean
	QueryStatsCalculator queryStatsCalculator() {
		return new QueryStatsCalculator();
	}

	@Bean
	ReviewWindow reviewWindow() {
		return new ReviewWindow();
	}

	@Bean
	HistoryRetention historyRetention() {
		return new HistoryRetention();
	}

	@Bean
	ListingTakeaway listingTakeaway() {
		return new ListingTakeaway();
	}

	@Bean
	ListingTimeline listingTimeline() {
		return new ListingTimeline();
	}

	@Configuration
	@EnableScheduling
	@ConditionalOnProperty(prefix = "printmomentum.ingest", name = "enabled", havingValue = "true")
	static class IngestScheduleConfig {
	}
}
