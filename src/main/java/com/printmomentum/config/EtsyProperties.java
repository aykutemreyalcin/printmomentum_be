package com.printmomentum.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "printmomentum.etsy")
public record EtsyProperties(
		String apiKey,
		@DefaultValue("https://openapi.etsy.com/v3/application") String baseUrl,
		@DefaultValue("5s") Duration connectTimeout,
		@DefaultValue("15s") Duration readTimeout,
		@DefaultValue("3") int maxRetries,
		@DefaultValue("200ms") Duration retryBackoff,
		@DefaultValue("8") int maxRequestsPerSecond) {
}
