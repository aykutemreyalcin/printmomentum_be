package com.printmomentum.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class EtsyApiKeyVerifierTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(EtsyConfig.class, EtsyApiKeyVerifier.class);

	@Test
	void missingApiKeyFailsFastWhenEtsyProfileIsActive() {
		runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("etsy"))
				.withPropertyValues("printmomentum.etsy.api-key=")
				.run(context -> assertThat(context)
						.hasFailed()
						.getFailure()
						.hasRootCauseInstanceOf(IllegalStateException.class)
						.hasRootCauseMessage("ETSY_API_KEY is required when the etsy profile is active"));
	}

	@Test
	void missingApiKeyDoesNotFailOnDefaultProfile() {
		runner.withPropertyValues("printmomentum.etsy.api-key=")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(EtsyApiKeyVerifier.class);
				});
	}

	@Test
	void etsyProfileStartsWhenApiKeyIsPresent() {
		runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("etsy"))
				.withPropertyValues("printmomentum.etsy.api-key=test-key")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(EtsyApiKeyVerifier.class);
				});
	}
}
