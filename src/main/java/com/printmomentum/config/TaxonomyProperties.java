package com.printmomentum.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "printmomentum.taxonomy")
public record TaxonomyProperties(List<Long> fallbackIds) {

	public TaxonomyProperties {
		fallbackIds = fallbackIds == null ? List.of() : List.copyOf(fallbackIds);
	}
}
