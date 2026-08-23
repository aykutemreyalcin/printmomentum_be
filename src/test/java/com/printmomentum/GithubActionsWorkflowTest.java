package com.printmomentum;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GithubActionsWorkflowTest {

	@Test
	void ciWorkflowUsesJava21AndMavenCacheWithoutEtsySecrets() throws Exception {
		String yaml = Files.readString(Path.of(".github/workflows/ci.yml"));
		assertThat(yaml).contains("java-version: \"21\"");
		assertThat(yaml).contains("cache: maven");
		assertThat(yaml).contains("./mvnw -B test");
		assertThat(yaml).contains("./mvnw -B -DskipTests package");
		assertThat(yaml).doesNotContain("ETSY_API_KEY");
		assertThat(yaml).doesNotContain("ETSY_SHARED_SECRET");
	}
}
