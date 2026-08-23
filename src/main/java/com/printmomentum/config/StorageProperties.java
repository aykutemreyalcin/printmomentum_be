package com.printmomentum.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "printmomentum.storage.s3")
public record StorageProperties(
		@DefaultValue("false") boolean enabled,
		String bucket,
		@DefaultValue("eu-central-1") String region) {
}
