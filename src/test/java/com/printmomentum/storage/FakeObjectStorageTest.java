package com.printmomentum.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FakeObjectStorageTest {

	@Test
	void objectKeyMatchesListingsIdNJpg() {
		assertThat(ObjectStorage.objectKey(42L, 1)).isEqualTo("listings/42/1.jpg");
		assertThat(ObjectStorage.objectKey(99L, 3)).isEqualTo("listings/99/3.jpg");
	}

	@Test
	void fakeStoresBytesUnderListingKey() {
		FakeObjectStorage fake = new FakeObjectStorage();
		byte[] bytes = {1, 2, 3};
		fake.put(ObjectStorage.objectKey(1147L, 2), bytes, MediaType.IMAGE_JPEG_VALUE);

		assertThat(fake.objects()).containsOnlyKeys("listings/1147/2.jpg");
		assertThat(fake.get("listings/1147/2.jpg")).containsExactly(1, 2, 3);
	}

	@Test
	void noopKeepsEtsyUrlAndDoesNotWrite() {
		ListingImageCache cache = new ListingImageCache(new NoopObjectStorage());
		assertThat(cache.cache(1L, 1, "https://i.etsystatic.com/x.jpg")).isEmpty();
	}
}
