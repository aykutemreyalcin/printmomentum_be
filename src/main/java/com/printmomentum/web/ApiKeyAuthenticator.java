package com.printmomentum.web;

import com.printmomentum.config.AuthProperties;
import com.printmomentum.domain.ApiClientRepository;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyAuthenticator {

	private final AuthProperties authProperties;
	private final ApiClientRepository apiClientRepository;

	public ApiKeyAuthenticator(AuthProperties authProperties, ApiClientRepository apiClientRepository) {
		this.authProperties = authProperties;
		this.apiClientRepository = apiClientRepository;
	}

	public boolean isValid(String apiKey) {
		if (apiKey == null || apiKey.isBlank()) {
			return false;
		}
		if (authProperties.additionalKeys().contains(apiKey)) {
			return true;
		}
		return apiClientRepository.findByApiKeyAndActiveTrue(apiKey).isPresent();
	}
}
