package com.printmomentum.config;

import com.printmomentum.ingest.EtsyRetryInterceptor;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(EtsyProperties.class)
public class EtsyConfig {

	@Bean
	RestClient etsyRestClient(EtsyProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.defaultHeader("x-api-key", properties.apiKey() == null ? "" : properties.apiKey())
				.requestFactory(requestFactory)
				.requestInterceptor(new EtsyRetryInterceptor(properties))
				.build();
	}
}
