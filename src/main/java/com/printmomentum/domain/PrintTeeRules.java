package com.printmomentum.domain;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public record PrintTeeRules(
		double threshold,
		double apparelWeight,
		List<Long> tshirtTaxonomyIds,
		Map<String, Double> includeWeights,
		Map<String, List<String>> includeKeywords,
		Map<String, List<String>> excludeKeywords) {

	public PrintTeeRules {
		tshirtTaxonomyIds = List.copyOf(tshirtTaxonomyIds);
		includeWeights = Map.copyOf(includeWeights);
		includeKeywords = copyKeywordMap(includeKeywords);
		excludeKeywords = copyKeywordMap(excludeKeywords);
	}

	public static PrintTeeRules fromClasspath() {
		String path = "/printmomentum/print-tee-classifier.yml";
		try (InputStream in = PrintTeeRules.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("Missing classifier rules on classpath: " + path);
			}
			return fromYaml(in);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to load classifier rules from " + path, ex);
		}
	}

	static PrintTeeRules fromYaml(InputStream in) {
		Map<String, Object> root = new Yaml().load(in);
		return new PrintTeeRules(
				asDouble(root.get("threshold")),
				asDouble(root.get("apparelWeight")),
				asLongList(root.get("tshirtTaxonomyIds")),
				asDoubleMap(root.get("includeWeights")),
				asKeywordMap(root.get("includeKeywords")),
				asKeywordMap(root.get("excludeKeywords")));
	}

	private static Map<String, List<String>> copyKeywordMap(Map<String, List<String>> source) {
		Map<String, List<String>> copy = new LinkedHashMap<>();
		source.forEach((key, value) -> copy.put(key, List.copyOf(value)));
		return Map.copyOf(copy);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, List<String>> asKeywordMap(Object value) {
		Map<String, Object> raw = (Map<String, Object>) value;
		Map<String, List<String>> mapped = new LinkedHashMap<>();
		raw.forEach((key, keywords) -> {
			List<String> list = new ArrayList<>();
			for (Object keyword : (List<Object>) keywords) {
				list.add(String.valueOf(keyword));
			}
			mapped.put(key, list);
		});
		return mapped;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Double> asDoubleMap(Object value) {
		Map<String, Object> raw = (Map<String, Object>) value;
		Map<String, Double> mapped = new LinkedHashMap<>();
		raw.forEach((key, number) -> mapped.put(key, asDouble(number)));
		return mapped;
	}

	@SuppressWarnings("unchecked")
	private static List<Long> asLongList(Object value) {
		List<Long> ids = new ArrayList<>();
		for (Object item : (List<Object>) value) {
			ids.add(((Number) item).longValue());
		}
		return ids;
	}

	private static double asDouble(Object value) {
		return ((Number) value).doubleValue();
	}
}
