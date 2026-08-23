package com.printmomentum.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "printmomentum.auth")
public record AuthProperties(List<String> additionalKeys) {

	public AuthProperties {
		additionalKeys = additionalKeys == null ? List.of() : List.copyOf(additionalKeys);
	}
}
