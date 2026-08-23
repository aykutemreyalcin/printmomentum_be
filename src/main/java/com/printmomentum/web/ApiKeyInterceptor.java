package com.printmomentum.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

public final class ApiKeyInterceptor implements HandlerInterceptor {

	public static final String HEADER = "X-Api-Key";

	private final ApiKeyAuthenticator authenticator;

	public ApiKeyInterceptor(ApiKeyAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isPublic(request)) {
			return true;
		}
		if (authenticator.isValid(request.getHeader(HEADER))) {
			return true;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid or missing API key");
	}

	private static boolean isPublic(HttpServletRequest request) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String path = request.getRequestURI();
		return "/api/v1/health".equals(path);
	}
}
