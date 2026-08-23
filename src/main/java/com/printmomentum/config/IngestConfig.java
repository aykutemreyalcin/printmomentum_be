package com.printmomentum.config;

import com.printmomentum.domain.PrintTeeClassifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(IngestProperties.class)
public class IngestConfig {

	@Bean
	PrintTeeClassifier printTeeClassifier() {
		return PrintTeeClassifier.loadDefault();
	}

	@Configuration
	@EnableScheduling
	@ConditionalOnProperty(prefix = "printmomentum.ingest", name = "enabled", havingValue = "true")
	static class IngestScheduleConfig {
	}
}
