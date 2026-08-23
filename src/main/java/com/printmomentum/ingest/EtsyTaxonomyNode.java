package com.printmomentum.ingest;

import java.util.List;

public record EtsyTaxonomyNode(long id, String name, List<EtsyTaxonomyNode> children) {
}
