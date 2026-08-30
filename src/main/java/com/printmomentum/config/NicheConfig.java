package com.printmomentum.config;

import com.printmomentum.niche.NicheStopwords;
import com.printmomentum.niche.NicheTermExtractor;
import com.printmomentum.niche.NicheWindowCalculator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NicheProperties.class)
public class NicheConfig {

	@Bean
	NicheStopwords nicheStopwords() {
		return NicheStopwords.loadDefault();
	}

	@Bean
	NicheTermExtractor nicheTermExtractor(NicheStopwords nicheStopwords) {
		return new NicheTermExtractor(nicheStopwords);
	}

	@Bean
	NicheWindowCalculator nicheWindowCalculator(NicheProperties properties, NicheStopwords nicheStopwords) {
		return new NicheWindowCalculator(properties, nicheStopwords);
	}
}
