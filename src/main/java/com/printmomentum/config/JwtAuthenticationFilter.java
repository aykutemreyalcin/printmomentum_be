package com.printmomentum.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	private final Cache<String, String> recentlyFailedTokens = Caffeine.newBuilder()
			.expireAfterWrite(10, TimeUnit.SECONDS)
			.maximumSize(1000)
			.build();

	private final Cache<String, UserDetails> userDetailsCache = Caffeine.newBuilder()
			.expireAfterWrite(600, TimeUnit.SECONDS)
			.maximumSize(5000)
			.build();

	public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		Logger logger = LoggerFactory.getLogger(getClass());

		if (request.getDispatcherType() == DispatcherType.ERROR) {
			filterChain.doFilter(request, response);
			return;
		}

		if (request.getServletPath().contains("/api/v2/auth")) {
			filterChain.doFilter(request, response);
			return;
		}

		if (isWhiteListed(request.getServletPath())) {
			filterChain.doFilter(request, response);
			return;
		}

		final String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			String uri = request.getRequestURI();
			if (uri != null && uri.startsWith("/api/v1/")) {
				logger.warn("""
						auth_reason=AUTH_MISSING_HEADER request={} method={} ip={} hostname={} userAgent={} authType={}
						""",
						uri,
						request.getMethod(),
						request.getRemoteAddr(),
						request.getRemoteHost(),
						request.getHeader("User-Agent"),
						request.getAuthType());
			} else {
				logger.debug("auth_reason=AUTH_MISSING_HEADER request={} method={} ip={}",
						uri, request.getMethod(), request.getRemoteAddr());
			}
			filterChain.doFilter(request, response);
			return;
		}

		final String jwt = authHeader.substring(7);
		String userEmail = null;

		try {
			userEmail = jwtService.extractUsername(jwt);
		} catch (ExpiredJwtException e) {
			logger.warn("auth_reason=AUTH_EXPIRED request={} method={}", request.getRequestURI(), request.getMethod());
			markTokenAsFailed(jwt, "expired");
			sendUnauthorizedResponse(response, "Couldn't verify your Identity. Please log out and log in again!");
			return;
		} catch (JwtException | IllegalArgumentException e) {
			logger.warn("auth_reason=AUTH_INVALID_JWT request={} method={} reason={}",
					request.getRequestURI(), request.getMethod(), e.getMessage());
			markTokenAsFailed(jwt, "invalid");
			sendUnauthorizedResponse(response, "Couldn't verify your Identity. Please log out and log in again!");
			return;
		}

		if (isRecentlyFailedToken(jwt)) {
			String cachedIdentifier = recentlyFailedTokens.getIfPresent(jwt);
			logger.warn("auth_reason=AUTH_RECENTLY_FAILED user={} request={} method={}",
					cachedIdentifier, request.getRequestURI(), request.getMethod());
			sendUnauthorizedResponse(response, "Couldn't verify your Identity. Please log out and log in again!");
			return;
		}

		if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails;
			try {
				userDetails = userDetailsCache.get(userEmail, this.userDetailsService::loadUserByUsername);
			} catch (Exception ex) {
				logger.warn("User details cannot be loaded for user={} request={} reason={}",
						userEmail, request.getRequestURI(), ex.getMessage());
				markTokenAsFailed(jwt, userEmail);
				sendUnauthorizedResponse(response, "Couldn't verify your Identity. Please log out and log in again!");
				return;
			}
			boolean isV2AccessToken = false;
			try {
				isV2AccessToken = jwtService.isV2AccessToken(jwt);
			} catch (Exception ex) {
				logger.debug("auth_reason=AUTH_V2_CLAIM_PARSE_FALLBACK request={} reason={}",
						request.getRequestURI(), ex.getMessage());
			}

			if (jwtService.isTokenValid(jwt, userDetails) && isV2AccessToken) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			} else {
				logger.warn("auth_reason=AUTH_INVALID_OR_REVOKED user={} request={} method={}",
						userEmail, request.getRequestURI(), request.getMethod());
				markTokenAsFailed(jwt, userEmail);
				sendUnauthorizedResponse(response, "Couldn't verify your Identity. Please log out and log in again!");
				return;
			}
		} else if (userEmail == null) {
			logger.warn("auth_reason=AUTH_NULL_USER request={} method={}", request.getRequestURI(), request.getMethod());
			markTokenAsFailed(jwt, "null_user");
			sendUnauthorizedResponse(response, "Couldn't verify your Identity. Please log out and log in again!");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private boolean isWhiteListed(String requestPath) {
		return Arrays.stream(SecurityConfiguration.WHITE_LIST_URL)
				.map(pattern -> pattern.replace("**", ".*"))
				.anyMatch(requestPath::matches);
	}

	private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		JSONObject obj = new JSONObject();
		obj.put("detail", message);
		response.getWriter().write(obj.toString());
	}

	private boolean isRecentlyFailedToken(String jwt) {
		return recentlyFailedTokens.getIfPresent(jwt) != null;
	}

	private void markTokenAsFailed(String jwt, String userIdentifier) {
		recentlyFailedTokens.put(jwt, userIdentifier);
	}
}
