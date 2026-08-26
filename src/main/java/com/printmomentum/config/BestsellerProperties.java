package com.printmomentum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "printmomentum.bestseller")
public record BestsellerProperties(
		@DefaultValue("false") boolean siteSearchEnabled,
		@DefaultValue("4") int maxPagesPerQuery,
		@DefaultValue("https://www.etsy.com") String siteBaseUrl) {
}
