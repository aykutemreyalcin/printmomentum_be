package com.printmomentum.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "printmomentum.ingest")
public record IngestProperties(
		@DefaultValue("false") boolean enabled,
		@DefaultValue("-") String cron,
		@DefaultValue("25") int limit,
		List<Query> queries) {

	public IngestProperties {
		queries = queries == null ? List.of() : List.copyOf(queries);
	}

	public record Query(String keywords, Long taxonomyId) {
	}
}
