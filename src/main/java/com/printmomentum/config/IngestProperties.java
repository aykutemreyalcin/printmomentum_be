package com.printmomentum.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "printmomentum.ingest")
public record IngestProperties(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("-") String cron,
		@DefaultValue("25") int limit,
		@DefaultValue("2") int pagesPerQuery,
		@DefaultValue("true") boolean createdPage,
		@DefaultValue("100") int reviewLimit,
		@DefaultValue("90") int queryHitRetentionDays,
		@DefaultValue("100") int topN,
		@DefaultValue("100") int minRemainingToday,
		List<Query> queries) {

	public IngestProperties {
		queries = queries == null ? List.of() : List.copyOf(queries);
	}

	public record Query(String keywords, Long taxonomyId) {
	}
}
