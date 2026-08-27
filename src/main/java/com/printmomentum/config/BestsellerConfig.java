package com.printmomentum.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BestsellerProperties.class)
public class BestsellerConfig {

	@Bean
	RestClient etsySiteRestClient(BestsellerProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofSeconds(8));
		String baseUrl = properties.siteBaseUrl() == null || properties.siteBaseUrl().isBlank()
				? "https://www.etsy.com"
				: properties.siteBaseUrl();
		return RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader(
						"User-Agent",
						"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
				.defaultHeader("Accept", "application/json")
				.requestFactory(requestFactory)
				.build();
	}
}
