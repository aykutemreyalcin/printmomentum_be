package com.printmomentum.config;

import com.printmomentum.web.ApiKeyAuthenticator;
import com.printmomentum.web.ApiKeyInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnBean(ApiKeyAuthenticator.class)
public class AuthConfig implements WebMvcConfigurer {

	private final ApiKeyAuthenticator apiKeyAuthenticator;

	public AuthConfig(ApiKeyAuthenticator apiKeyAuthenticator) {
		this.apiKeyAuthenticator = apiKeyAuthenticator;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ApiKeyInterceptor(apiKeyAuthenticator)).addPathPatterns("/api/**");
	}
}
