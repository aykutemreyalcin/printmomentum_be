package com.printmomentum.ingest;

import com.printmomentum.config.BestsellerProperties;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class EtsyBestsellerSearch {

	private static final Logger log = LoggerFactory.getLogger(EtsyBestsellerSearch.class);
	private static final Pattern LISTING_ID = Pattern.compile(
			"data-listing-id=\"(\\d+)\"|/listing/(\\d+)(?:/|\"|'|\\?|#)|\"listing_id\"\\s*:\\s*(\\d+)");

	private final RestClient restClient;
	private final BestsellerProperties properties;

	public EtsyBestsellerSearch(
			@Qualifier("etsySiteRestClient") RestClient restClient, BestsellerProperties properties) {
		this.restClient = restClient;
		this.properties = properties;
	}

	public Optional<Set<Long>> listingIds(String keywords) {
		if (keywords == null || keywords.isBlank()) {
			return Optional.of(Set.of());
		}
		Set<Long> ids = new LinkedHashSet<>();
		int maxPages = Math.min(Math.max(properties.maxPagesPerQuery(), 1), 4);
		for (int page = 1; page <= maxPages; page++) {
			Optional<String> html = fetchPage(keywords, page);
			if (html.isEmpty()) {
				return Optional.empty();
			}
			Set<Long> pageIds = parseListingIds(html.get());
			if (pageIds.isEmpty()) {
				break;
			}
			int before = ids.size();
			ids.addAll(pageIds);
			if (ids.size() == before) {
				break;
			}
		}
		return Optional.of(Set.copyOf(ids));
	}

	static Set<Long> parseListingIds(String html) {
		if (html == null || html.isBlank()) {
			return Set.of();
		}
		Set<Long> ids = new LinkedHashSet<>();
		Matcher matcher = LISTING_ID.matcher(html);
		while (matcher.find()) {
			for (int group = 1; group <= matcher.groupCount(); group++) {
				String value = matcher.group(group);
				if (value != null) {
					ids.add(Long.parseLong(value));
					break;
				}
			}
		}
		return Set.copyOf(ids);
	}

	private Optional<String> fetchPage(String keywords, int page) {
		try {
			return restClient
					.get()
					.uri(uriBuilder -> uriBuilder
							.path("/search")
							.queryParam("q", keywords)
							.queryParam("is_best_seller", "true")
							.queryParam("page", page)
							.build())
					.exchange((request, response) -> {
						HttpStatusCode status = response.getStatusCode();
						if (status.value() == 403 || status.value() == 429 || status.is5xxServerError()) {
							log.warn("bestseller search skipped keywords={} page={} http={}", keywords, page, status.value());
							return Optional.empty();
						}
						if (!status.is2xxSuccessful()) {
							log.warn("bestseller search skipped keywords={} page={} http={}", keywords, page, status.value());
							return Optional.empty();
						}
						String body;
						try {
							body = new String(response.getBody().readAllBytes());
						} catch (java.io.IOException ex) {
							log.warn("bestseller search read failed keywords={} page={}: {}", keywords, page, ex.toString());
							return Optional.empty();
						}
						return Optional.of(body);
					});
		} catch (RestClientException ex) {
			log.warn("bestseller search failed keywords={} page={}: {}", keywords, page, ex.toString());
			return Optional.empty();
		}
	}
}
