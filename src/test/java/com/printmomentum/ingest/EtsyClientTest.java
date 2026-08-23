package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import tools.jackson.databind.ObjectMapper;
import com.printmomentum.config.EtsyProperties;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EtsyClientTest {

	private static final String BASE_URL = "https://openapi.etsy.com/v3/application";

	private MockRestServiceServer server;
	private EtsyClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		EtsyProperties properties = new EtsyProperties(
				"test-api-key",
				BASE_URL,
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				2,
				Duration.ofMillis(1),
				0);
		RestClient restClient = builder
				.defaultHeader("x-api-key", "test-api-key")
				.requestInterceptor(new EtsyRetryInterceptor(properties))
				.build();
		client = new EtsyClient(restClient, new EtsyListingMapper(new ObjectMapper()));
	}

	@Test
	void searchActiveMapsListingIdTitleAndFavorers() {
		server.expect(requestTo("https://openapi.etsy.com/v3/application/listings/active?keywords=graphic%20tee&limit=25&offset=0&sort_on=score&sort_order=desc"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("x-api-key", "test-api-key"))
				.andRespond(withSuccess(new ClassPathResource("etsy/search-active.json"), MediaType.APPLICATION_JSON));

		EtsySearchPage page = client.searchActive("graphic tee", null, 25, 0);

		assertThat(page.results()).hasSize(1);
		assertThat(page.results().get(0).listingId()).isEqualTo(1147645830L);
		assertThat(page.results().get(0).title()).isEqualTo("Graphic DTG Print Tee");
		assertThat(page.results().get(0).numFavorers()).isEqualTo(42);
		server.verify();
	}

	@Test
	void getListingMapsDocumentedFields() {
		server.expect(requestTo("https://openapi.etsy.com/v3/application/listings/1147645830?includes=Images"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(new ClassPathResource("etsy/listing.json"), MediaType.APPLICATION_JSON));

		EtsyListing listing = client.getListing(1147645830L);

		assertThat(listing.listingId()).isEqualTo(1147645830L);
		assertThat(listing.title()).isEqualTo("Graphic DTG Print Tee");
		assertThat(listing.numFavorers()).isEqualTo(42);
		server.verify();
	}

	@Test
	void getTaxonomyMapsNodes() {
		server.expect(requestTo("https://openapi.etsy.com/v3/application/buyer-taxonomy/nodes"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(
						"""
								{"count":1,"results":[{"id":1,"name":"Clothing","children":[{"id":1603,"name":"T-shirts","children":[]}]}]}
								""",
						MediaType.APPLICATION_JSON));

		List<EtsyTaxonomyNode> nodes = client.getTaxonomy();

		assertThat(nodes).hasSize(1);
		assertThat(nodes.get(0).id()).isEqualTo(1L);
		assertThat(nodes.get(0).name()).isEqualTo("Clothing");
		assertThat(nodes.get(0).children()).hasSize(1);
		assertThat(nodes.get(0).children().get(0).id()).isEqualTo(1603L);
		server.verify();
	}

	@Test
	void retriesOn429ThenFails() {
		server.expect(times(3), requestTo("https://openapi.etsy.com/v3/application/listings/active?keywords=graphic%20tee&limit=25&offset=0&sort_on=score&sort_order=desc"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
						.header("retry-after", "0")
						.header("x-remaining-today", "0")
						.header("x-remaining-this-second", "0"));

		assertThatThrownBy(() -> client.searchActive("graphic tee", null, 25, 0))
				.isInstanceOf(EtsyUnavailableException.class)
				.hasMessageContaining("429");
		server.verify();
	}
}
