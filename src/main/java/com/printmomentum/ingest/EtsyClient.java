package com.printmomentum.ingest;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EtsyClient {

	private final RestClient restClient;
	private final EtsyListingMapper mapper;

	public EtsyClient(@Qualifier("etsyRestClient") RestClient restClient, EtsyListingMapper mapper) {
		this.restClient = restClient;
		this.mapper = mapper;
	}

	public EtsySearchPage searchActive(String keywords, Long taxonomyId, int limit, int offset) {
		return searchActive(keywords, taxonomyId, limit, offset, "score", "desc");
	}

	public EtsySearchPage searchActive(
			String keywords, Long taxonomyId, int limit, int offset, String sortOn, String sortOrder) {
		int cappedLimit = Math.min(Math.max(limit, 1), 100);
		return restClient
				.get()
				.uri(uriBuilder -> uriBuilder
						.path("/listings/active")
						.queryParamIfPresent("keywords", Optional.ofNullable(keywords))
						.queryParamIfPresent("taxonomy_id", Optional.ofNullable(taxonomyId))
						.queryParam("limit", cappedLimit)
						.queryParam("offset", Math.max(offset, 0))
						.queryParam("sort_on", sortOn)
						.queryParam("sort_order", sortOrder)
						.build())
				.exchange((request, response) -> readSearchOrThrow(response.getStatusCode(), response));
	}

	public EtsyListing getListing(long listingId) {
		return restClient
				.get()
				.uri(uriBuilder -> uriBuilder
						.path("/listings/{listingId}")
						.queryParam("includes", "Images")
						.build(listingId))
				.exchange((request, response) -> readListingOrThrow(response.getStatusCode(), response));
	}

	public List<EtsyTaxonomyNode> getTaxonomy() {
		return restClient
				.get()
				.uri("/buyer-taxonomy/nodes")
				.exchange((request, response) -> readTaxonomyOrThrow(response.getStatusCode(), response));
	}

	private EtsySearchPage readSearchOrThrow(
			HttpStatusCode status, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
		throwIfUnavailable(status);
		return mapper.readSearch(body(response));
	}

	private EtsyListing readListingOrThrow(
			HttpStatusCode status, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
		throwIfUnavailable(status);
		return mapper.readListing(body(response));
	}

	private List<EtsyTaxonomyNode> readTaxonomyOrThrow(
			HttpStatusCode status, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
		throwIfUnavailable(status);
		return mapper.readTaxonomy(body(response));
	}

	private static void throwIfUnavailable(HttpStatusCode status) {
		if (status.value() == 429 || status.is5xxServerError()) {
			throw new EtsyUnavailableException("Etsy unavailable: HTTP " + status.value());
		}
		if (!status.is2xxSuccessful()) {
			throw new IllegalStateException("Etsy request failed: HTTP " + status.value());
		}
	}

	private static java.io.InputStream body(
			RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) {
		try {
			return response.getBody();
		} catch (java.io.IOException ex) {
			throw new EtsyUnavailableException("Failed to read Etsy response", ex);
		}
	}
}
