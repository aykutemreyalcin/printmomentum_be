package com.printmomentum.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "printmomentum.ingest")
public record IngestProperties(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("-") String cron,
		@DefaultValue("100") int limit,
		@DefaultValue("4") int pagesPerSweep,
		@DefaultValue("2") int pagesPerQuery,
		@DefaultValue("1603") Long taxonomyId,
		@DefaultValue("false") boolean createdPage,
		@DefaultValue("100") int reviewLimit,
		@DefaultValue("90") int queryHitRetentionDays,
		@DefaultValue("100") int topN,
		@DefaultValue("100") int minRemainingToday,
		@DefaultValue("30") int maxShopsPerRun,
		@DefaultValue("4") int maxPagesPerShop,
		@DefaultValue("5") int pmBestsellerMinFavorersDelta7d,
		@DefaultValue("25") int staleRefreshLimit,
		List<Query> benchmarkQueries,
		List<Query> queries) {

	public IngestProperties {
		benchmarkQueries = normalizeBenchmarkQueries(benchmarkQueries, queries);
		queries = queries == null ? List.of() : List.copyOf(queries);
	}

	public List<Query> benchmarkQueries() {
		return benchmarkQueries;
	}

	public int effectivePagesPerSweep() {
		int sweep = pagesPerSweep > 0 ? pagesPerSweep : pagesPerQuery;
		return Math.min(Math.max(sweep, 1), 4);
	}

	public record Query(String keywords, Long taxonomyId) {
	}

	private static List<Query> normalizeBenchmarkQueries(List<Query> benchmarkQueries, List<Query> legacyQueries) {
		if (benchmarkQueries != null && !benchmarkQueries.isEmpty()) {
			return List.copyOf(benchmarkQueries);
		}
		if (legacyQueries != null && !legacyQueries.isEmpty()) {
			return List.copyOf(legacyQueries);
		}
		return List.of();
	}
}
