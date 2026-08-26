package com.printmomentum.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EtsyBestsellerSearchTest {

	@Test
	void parseListingIdsFromHtmlAndJson() {
		String html = """
				<div data-listing-id="111">a</div>
				<a href="https://www.etsy.com/listing/222/graphic-tee">tee</a>
				<script>{"listing_id":333}</script>
				""";

		assertThat(EtsyBestsellerSearch.parseListingIds(html)).containsExactlyInAnyOrder(111L, 222L, 333L);
	}

	@Test
	void parseListingIdsIgnoresBlankHtml() {
		assertThat(EtsyBestsellerSearch.parseListingIds("")).isEmpty();
	}
}
