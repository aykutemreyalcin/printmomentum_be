package com.printmomentum.storage;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ListingImageCache {

	private static final Logger log = LoggerFactory.getLogger(ListingImageCache.class);

	private final ObjectStorage objectStorage;
	private final RestClient restClient;

	public ListingImageCache(ObjectStorage objectStorage) {
		this.objectStorage = objectStorage;
		this.restClient = RestClient.create();
	}

	public Optional<String> cache(long listingId, int n, String sourceUrl) {
		if (!objectStorage.writable() || sourceUrl == null || sourceUrl.isBlank()) {
			return Optional.empty();
		}
		String key = ObjectStorage.objectKey(listingId, n);
		try {
			byte[] bytes = restClient.get().uri(sourceUrl).retrieve().body(byte[].class);
			if (bytes == null || bytes.length == 0) {
				return Optional.empty();
			}
			objectStorage.put(key, bytes, MediaType.IMAGE_JPEG_VALUE);
			return Optional.of(key);
		} catch (RuntimeException ex) {
			log.warn("image cache skipped listing_id={} n={}: {}", listingId, n, ex.toString());
			return Optional.empty();
		}
	}
}
