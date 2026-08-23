package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class TaxonomyIdResolverTest {

	@Autowired
	private TaxonomyIdResolver taxonomyIdResolver;

	@Autowired
	private CacheManager cacheManager;

	@MockitoBean
	private EtsyClient etsyClient;

	@Test
	void secondLookupHitsCacheAndCallsClientOnce() {
		when(etsyClient.getTaxonomy()).thenReturn(List.of(clothingWithTshirts()));
		cacheManager.getCache("etsyTaxonomy").clear();

		List<Long> first = taxonomyIdResolver.tshirtTaxonomyIds();
		List<Long> second = taxonomyIdResolver.tshirtTaxonomyIds();

		assertThat(first).containsExactly(1603L);
		assertThat(second).containsExactly(1603L);
		verify(etsyClient, times(1)).getTaxonomy();
	}

	@Test
	void etsyFailureUsesYamlFallback() {
		when(etsyClient.getTaxonomy()).thenThrow(new EtsyUnavailableException("Etsy unavailable: HTTP 503"));
		cacheManager.getCache("etsyTaxonomy").clear();

		assertThat(taxonomyIdResolver.tshirtTaxonomyIds()).containsExactly(1603L);
	}

	private static EtsyTaxonomyNode clothingWithTshirts() {
		return new EtsyTaxonomyNode(
				1L,
				"Clothing",
				List.of(new EtsyTaxonomyNode(1603L, "T-shirts", List.of())));
	}
}
