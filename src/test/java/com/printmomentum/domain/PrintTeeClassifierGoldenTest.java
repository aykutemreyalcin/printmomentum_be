package com.printmomentum.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

class PrintTeeClassifierGoldenTest {

	private final PrintTeeClassifier classifier = PrintTeeClassifier.loadDefault();

	@Test
	void goldenFixturesMatchThresholdAndDocumentedPrecision() throws Exception {
		String json = new String(
				new ClassPathResource("printtee/golden.json").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		GoldenSuite suite = new ObjectMapper().readValue(json, GoldenSuite.class);

		assertThat(suite.cases()).hasSize(17);
		assertThat(suite.includeCount()).isEqualTo(8);
		assertThat(suite.excludeCount()).isEqualTo(9);
		assertThat(suite.threshold()).isEqualTo(0.7);
		assertThat(suite.documentedPrecision()).isEqualTo(1.0);

		long includes = suite.cases().stream().filter(GoldenCase::expectPrintTee).count();
		long excludes = suite.cases().size() - includes;
		assertThat(includes).isEqualTo(suite.includeCount());
		assertThat(excludes).isEqualTo(suite.excludeCount());

		int correct = 0;
		for (GoldenCase fixture : suite.cases()) {
			PrintTeeClassification result = classifier.classify(
					fixture.title(), fixture.description(), fixture.tags(), fixture.taxonomyId());

			assertThat(result.score())
					.as(fixture.id())
					.isBetween(0.0, 1.0);
			if (fixture.expectPrintTee()) {
				assertThat(result.printTee()).as(fixture.id()).isTrue();
				assertThat(result.score()).as(fixture.id()).isGreaterThanOrEqualTo(suite.threshold());
				assertThat(result.rejectReasons()).as(fixture.id()).isEmpty();
			}
			else {
				assertThat(result.printTee()).as(fixture.id()).isFalse();
				assertThat(result.score()).as(fixture.id()).isLessThan(suite.threshold());
				assertThat(result.rejectReasons()).as(fixture.id()).isNotEmpty();
			}
			if (result.printTee() == fixture.expectPrintTee()) {
				correct++;
			}
		}

		double precision = (double) correct / suite.cases().size();
		assertThat(precision).isEqualTo(suite.documentedPrecision());
	}

	@Test
	void yamlThresholdIsPointSeven() {
		assertThat(PrintTeeRules.fromClasspath().threshold()).isEqualTo(0.7);
	}

	record GoldenSuite(
			double documentedPrecision,
			int includeCount,
			int excludeCount,
			double threshold,
			List<GoldenCase> cases) {
	}

	record GoldenCase(
			String id,
			boolean expectPrintTee,
			String title,
			String description,
			List<String> tags,
			Long taxonomyId) {
	}
}
