package com.printmomentum.ingest;

import com.printmomentum.config.TaxonomyProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaxonomyIdResolver {

	private static final Logger log = LoggerFactory.getLogger(TaxonomyIdResolver.class);

	private final EtsyTaxonomyCache taxonomyCache;
	private final TaxonomyProperties properties;

	public TaxonomyIdResolver(EtsyTaxonomyCache taxonomyCache, TaxonomyProperties properties) {
		this.taxonomyCache = taxonomyCache;
		this.properties = properties;
	}

	public List<Long> tshirtTaxonomyIds() {
		try {
			List<Long> ids = findTshirtIds(taxonomyCache.nodes());
			if (!ids.isEmpty()) {
				return ids;
			}
			log.warn("etsy taxonomy had no t-shirt nodes; using yaml fallback");
			return properties.fallbackIds();
		} catch (RuntimeException ex) {
			log.warn("etsy taxonomy failed; using yaml fallback: {}", ex.toString());
			return properties.fallbackIds();
		}
	}

	static List<Long> findTshirtIds(List<EtsyTaxonomyNode> roots) {
		List<Long> ids = new ArrayList<>();
		walk(roots, ids);
		return List.copyOf(ids);
	}

	private static void walk(List<EtsyTaxonomyNode> nodes, List<Long> ids) {
		if (nodes == null) {
			return;
		}
		for (EtsyTaxonomyNode node : nodes) {
			if (node == null) {
				continue;
			}
			if (isTshirtName(node.name())) {
				ids.add(node.id());
			}
			walk(node.children(), ids);
		}
	}

	private static boolean isTshirtName(String name) {
		if (name == null || name.isBlank()) {
			return false;
		}
		String normalized = name.toLowerCase(Locale.ROOT);
		return normalized.contains("t-shirt")
				|| normalized.contains("t shirt")
				|| normalized.contains("tshirts")
				|| normalized.contains("tees");
	}
}
