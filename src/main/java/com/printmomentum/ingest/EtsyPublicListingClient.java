package com.printmomentum.ingest;

import com.printmomentum.config.BestsellerProperties;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Etsy exposes {@code is_bestseller} on the public ajax listing endpoint.
 * Unlike HTML search, this JSON endpoint is reachable from our server without DataDome.
 */
@Component
public class EtsyPublicListingClient {

	private static final Logger log = LoggerFactory.getLogger(EtsyPublicListingClient.class);

	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public EtsyPublicListingClient(
			@Qualifier("etsySiteRestClient") RestClient restClient, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
	}

	public Optional<Boolean> isBestseller(long listingId) {
		try {
			return restClient
					.get()
					.uri("/api/v3/ajax/public/listings/{listingId}", listingId)
					.exchange((request, response) -> parse(response.getStatusCode(), response));
		} catch (RestClientException ex) {
			log.warn("etsy public listing skipped listing_id={}: {}", listingId, ex.toString());
			return Optional.empty();
		}
	}

	private Optional<Boolean> parse(
			HttpStatusCode status, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
		if (status.value() == 404) {
			return Optional.empty();
		}
		if (status.value() == 429 || status.is5xxServerError()) {
			log.warn("etsy public listing unavailable http={}", status.value());
			return Optional.empty();
		}
		if (!status.is2xxSuccessful()) {
			log.warn("etsy public listing unexpected http={}", status.value());
			return Optional.empty();
		}
		try {
			JsonNode body = objectMapper.readTree(response.getBody());
			if (!body.has("is_bestseller") || body.get("is_bestseller").isNull()) {
				return Optional.empty();
			}
			return Optional.of(body.get("is_bestseller").asBoolean());
		} catch (java.io.IOException ex) {
			log.warn("etsy public listing parse failed: {}", ex.toString());
			return Optional.empty();
		}
	}
}
