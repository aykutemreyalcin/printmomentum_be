package com.printmomentum.ingest;

import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class EtsyTaxonomyCache {

	private final EtsyClient etsyClient;

	public EtsyTaxonomyCache(EtsyClient etsyClient) {
		this.etsyClient = etsyClient;
	}

	@Cacheable("etsyTaxonomy")
	public List<EtsyTaxonomyNode> nodes() {
		return etsyClient.getTaxonomy();
	}
}
