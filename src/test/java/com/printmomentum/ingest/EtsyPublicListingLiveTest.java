package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

class EtsyPublicListingLiveTest {

	private final EtsyPublicListingClient client = new EtsyPublicListingClient(
			RestClient.builder()
					.baseUrl("https://www.etsy.com")
					.defaultHeader(
							"User-Agent",
							"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
					.defaultHeader("Accept", "application/json")
					.requestFactory(new JdkClientHttpRequestFactory(
							HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()))
					.build(),
			new ObjectMapper());

	@Test
	@EnabledIfEnvironmentVariable(named = "ETSY_LIVE_TEST", matches = "true")
	void knownBestsellerListingReturnsTrue() {
		assertThat(client.isBestseller(4525213805L)).contains(true);
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "ETSY_LIVE_TEST", matches = "true")
	void knownNonBestsellerListingReturnsFalse() {
		assertThat(client.isBestseller(4490045725L)).contains(false);
	}
}
