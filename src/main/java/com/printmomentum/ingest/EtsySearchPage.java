package com.printmomentum.ingest;

import java.util.List;

public record EtsySearchPage(int count, List<EtsyListing> results) {
}
