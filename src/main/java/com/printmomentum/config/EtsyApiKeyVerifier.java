package com.printmomentum.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("etsy")
public class EtsyApiKeyVerifier implements InitializingBean {

	private final EtsyProperties properties;

	public EtsyApiKeyVerifier(EtsyProperties properties) {
		this.properties = properties;
	}

	@Override
	public void afterPropertiesSet() {
		if (!StringUtils.hasText(properties.apiKey())) {
			throw new IllegalStateException("ETSY_API_KEY is required when the etsy profile is active");
		}
	}
}
