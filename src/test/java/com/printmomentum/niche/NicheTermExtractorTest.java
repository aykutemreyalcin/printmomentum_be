package com.printmomentum.niche;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NicheTermExtractorTest {

	private NicheTermExtractor extractor;

	@BeforeEach
	void setUp() {
		extractor = new NicheTermExtractor(NicheStopwords.loadDefault());
	}

	@Test
	void extractsDollyPartonFromTagsAndTitle() {
		List<NicheTermExtractor.ExtractedTerm> terms = extractor.extract(
				"Dolly Parton Graphic Tee Shirt Gift",
				List.of("dolly parton", "country music", "graphic tee"));

		assertThat(terms).extracting(NicheTermExtractor.ExtractedTerm::label).contains("dolly parton");
		assertThat(terms).extracting(NicheTermExtractor.ExtractedTerm::label).doesNotContain("tshirt", "tee", "shirt");
	}

	@Test
	void genericGraphicTeeYieldsNoBuyerNiche() {
		List<NicheTermExtractor.ExtractedTerm> terms = extractor.extract(
				"Graphic Tee Shirt Unisex Cotton T-Shirt",
				List.of("graphic tee", "tshirt", "gift"));

		assertThat(terms).isEmpty();
	}

	@Test
	void retiredTeacherPhraseFromTitle() {
		List<NicheTermExtractor.ExtractedTerm> terms = extractor.extract(
				"Retired Teacher Shirt Funny Retirement Gift",
				List.of("retired teacher", "teacher gift", "retirement"));

		assertThat(terms).extracting(NicheTermExtractor.ExtractedTerm::label).contains("retired teacher");
	}
}
