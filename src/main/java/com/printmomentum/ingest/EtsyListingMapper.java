package com.printmomentum.ingest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EtsyListingMapper {

	private final ObjectMapper objectMapper;

	public EtsyListingMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public EtsySearchPage readSearch(InputStream body) {
		return readSearch(readTree(body));
	}

	public EtsySearchPage readSearch(String json) {
		return readSearch(readTree(json));
	}

	public EtsyListing readListing(InputStream body) {
		return readListing(readTree(body));
	}

	public EtsyListing readListing(String json) {
		return readListing(readTree(json));
	}

	public List<EtsyTaxonomyNode> readTaxonomy(InputStream body) {
		return readTaxonomy(readTree(body));
	}

	EtsySearchPage readSearch(JsonNode root) {
		int count = root.path("count").asInt(0);
		List<EtsyListing> results = new ArrayList<>();
		for (JsonNode item : root.path("results")) {
			results.add(readListing(item));
		}
		return new EtsySearchPage(count, List.copyOf(results));
	}

	EtsyListing readListing(JsonNode node) {
		if (node.has("results") && node.get("results").isArray() && node.get("results").size() > 0) {
			return readListing(node.get("results").get(0));
		}
		return new EtsyListing(
				node.path("listing_id").asLong(),
				longOrNull(node, "shop_id"),
				textOrNull(node, "title"),
				textOrNull(node, "description"),
				stringList(node.path("tags")),
				longOrNull(node, "taxonomy_id"),
				textOrNull(node, "url"),
				readMoney(node.path("price")),
				intOrNull(node, "quantity"),
				intOrNull(node, "num_favorers"),
				instantOrNull(node, "created_timestamp"),
				instantOrNull(node, "original_creation_timestamp"),
				instantOrNull(node, "updated_timestamp"),
				textOrNull(node, "state"),
				readImages(node.path("images")));
	}

	List<EtsyTaxonomyNode> readTaxonomy(JsonNode root) {
		List<EtsyTaxonomyNode> nodes = new ArrayList<>();
		for (JsonNode item : root.path("results")) {
			nodes.add(readTaxonomyNode(item));
		}
		return List.copyOf(nodes);
	}

	private EtsyTaxonomyNode readTaxonomyNode(JsonNode node) {
		List<EtsyTaxonomyNode> children = new ArrayList<>();
		for (JsonNode child : node.path("children")) {
			children.add(readTaxonomyNode(child));
		}
		return new EtsyTaxonomyNode(node.path("id").asLong(), textOrNull(node, "name"), List.copyOf(children));
	}

	private EtsyMoney readMoney(JsonNode price) {
		if (price == null || price.isMissingNode() || price.isNull()) {
			return null;
		}
		if (!price.has("amount")) {
			return null;
		}
		return new EtsyMoney(
				price.path("amount").asLong(),
				price.path("divisor").asInt(100),
				textOrNull(price, "currency_code"));
	}

	private List<EtsyImage> readImages(JsonNode images) {
		List<EtsyImage> result = new ArrayList<>();
		for (JsonNode image : images) {
			String url = firstText(image, "url_fullxfull", "url");
			if (url == null) {
				continue;
			}
			result.add(new EtsyImage(url, image.path("rank").asInt(0)));
		}
		return List.copyOf(result);
	}

	private JsonNode readTree(InputStream body) {
		return objectMapper.readTree(body);
	}

	private JsonNode readTree(String json) {
		return objectMapper.readTree(json);
	}

	private static Long longOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asLong();
	}

	private static Integer intOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asInt();
	}

	private static String textOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || value.asString().isBlank()) {
			return null;
		}
		return value.asString();
	}

	private static String firstText(JsonNode node, String... fields) {
		for (String field : fields) {
			String value = textOrNull(node, field);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private static Instant instantOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || !value.canConvertToLong()) {
			return null;
		}
		return Instant.ofEpochSecond(value.asLong());
	}

	private static List<String> stringList(JsonNode array) {
		if (array == null || !array.isArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>();
		for (JsonNode item : array) {
			if (!item.isNull()) {
				values.add(item.asString());
			}
		}
		return List.copyOf(values);
	}
}
