package com.printmomentum.auth;

import com.printmomentum.config.JwtService;
import com.printmomentum.domain.User;
import com.printmomentum.domain.UserRepository;
import com.printmomentum.domain.UserSession;
import com.printmomentum.domain.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class V2AuthenticationService {

	private static final String AUTH_VERSION = "v2";
	private static final String REFRESH_REVOKE_LOGOUT = "logout";
	private static final String REFRESH_REVOKE_NEW_LOGIN = "new_login";
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final AuthenticationManager authenticationManager;
	private final UserRepository userRepository;
	private final UserSessionRepository userSessionRepository;
	private final JwtService jwtService;
	private final SecureRandom secureRandom = new SecureRandom();

	@Value("${auth.v2.access-token-expiration-ms:900000}")
	private long accessTokenExpirationMs;

	@Value("${auth.v2.refresh-token-expiration-ms:1209600000}")
	private long refreshTokenExpirationMs;

	@Value("${auth.v2.refresh-cookie-name:printmomentum_refresh_token}")
	private String refreshCookieName;

	@Value("${auth.v2.refresh-cookie-secure:true}")
	private boolean refreshCookieSecure;

	@Value("${auth.v2.refresh-cookie-same-site:Lax}")
	private String refreshCookieSameSite;

	@Value("${auth.v2.refresh-cookie-path:/}")
	private String refreshCookiePath;

	@Value("${auth.v2.refresh-cookie-domain:}")
	private String refreshCookieDomain;

	public V2AuthenticationService(
			AuthenticationManager authenticationManager,
			UserRepository userRepository,
			UserSessionRepository userSessionRepository,
			JwtService jwtService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.userSessionRepository = userSessionRepository;
		this.jwtService = jwtService;
	}

	@Transactional
	public V2AuthenticationResponse authenticate(
			V2AuthenticationRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
		} catch (DisabledException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "This account is deactivated");
		} catch (AuthenticationException exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		}

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		if (!Boolean.TRUE.equals(user.getActive())) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "User is waiting for approval");
		}

		return issueTokensForUser(user, request.getDeviceId(), httpRequest, response);
	}

	@Transactional
	public V2AuthenticationResponse refresh(
			V2RefreshRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
		long startedAt = System.nanoTime();
		String clientIp = resolveClientIp(httpRequest);
		String deviceId = request != null ? trimToLength(request.getDeviceId(), 128) : null;
		String refreshToken = extractCookieValue(httpRequest, refreshCookieName);
		String requestId = trimToLength(String.valueOf(httpRequest.getAttribute("requestId")), 64);
		logger.info(
				"auth_reason=REFRESH_ATTEMPT requestId={} request={} method={} ip={} deviceId={} cookiePresent={}",
				requestId,
				httpRequest.getRequestURI(),
				httpRequest.getMethod(),
				clientIp,
				deviceId,
				refreshToken != null && !refreshToken.isBlank());

		if (refreshToken == null || refreshToken.isBlank()) {
			logger.warn(
					"auth_reason=REFRESH_FAILED reason=MISSING_COOKIE requestId={} request={} method={} ip={} durationMs={}",
					requestId,
					httpRequest.getRequestURI(),
					httpRequest.getMethod(),
					clientIp,
					elapsedMs(startedAt));
			clearRefreshCookie(response);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is required");
		}

		String refreshTokenHash = hashToken(refreshToken);
		UserSession session = userSessionRepository.findByRefreshTokenHash(refreshTokenHash).orElse(null);
		if (session == null) {
			logger.warn(
					"auth_reason=REFRESH_FAILED reason=INVALID_TOKEN requestId={} request={} method={} ip={} durationMs={} tokenHashPrefix={}",
					requestId,
					httpRequest.getRequestURI(),
					httpRequest.getMethod(),
					clientIp,
					elapsedMs(startedAt),
					maskHash(refreshTokenHash));
			clearRefreshCookie(response);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid");
		}

		LocalDateTime now = LocalDateTime.now();
		if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
			logger.warn(
					"auth_reason=REFRESH_FAILED reason=REVOKED_OR_EXPIRED requestId={} request={} method={} ip={} sessionId={} userId={}",
					requestId,
					httpRequest.getRequestURI(),
					httpRequest.getMethod(),
					clientIp,
					session.getId(),
					session.getUser() != null ? session.getUser().getId() : null);
			clearRefreshCookie(response);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired or revoked");
		}

		User user = userRepository.findById(session.getUser().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (!Boolean.TRUE.equals(user.getActive())) {
			logger.warn(
					"auth_reason=REFRESH_FAILED reason=INACTIVE_USER requestId={} request={} method={} ip={} sessionId={} userId={}",
					requestId,
					httpRequest.getRequestURI(),
					httpRequest.getMethod(),
					clientIp,
					session.getId(),
					user.getId());
			revokeSession(session, "inactive_user");
			clearRefreshCookie(response);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is inactive");
		}

		String rotatedRefresh = generateOpaqueToken();
		session.setRefreshTokenHash(hashToken(rotatedRefresh));
		session.setExpiresAt(now.plus(Duration.ofMillis(refreshTokenExpirationMs)));
		session.setLastUsedAt(now);
		session.setRevokedAt(null);
		session.setRevokeReason(null);

		if (request != null && request.getDeviceId() != null && !request.getDeviceId().isBlank()) {
			session.setDeviceId(request.getDeviceId());
		}
		session.setIpAddress(resolveClientIp(httpRequest));
		session.setUserAgent(trimToLength(httpRequest.getHeader("User-Agent"), 512));
		userSessionRepository.save(session);

		String accessToken = jwtService.generateV2AccessToken(user, session.getId(), accessTokenExpirationMs);
		setRefreshCookie(response, rotatedRefresh);
		logger.info(
				"auth_reason=REFRESH_SUCCESS requestId={} request={} method={} ip={} sessionId={} userId={} durationMs={}",
				requestId,
				httpRequest.getRequestURI(),
				httpRequest.getMethod(),
				clientIp,
				session.getId(),
				user.getId(),
				elapsedMs(startedAt));

		return new V2AuthenticationResponse(accessToken, accessTokenExpirationMs / 1000L, AUTH_VERSION);
	}

	@Transactional
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractCookieValue(request, refreshCookieName);
		if (refreshToken != null && !refreshToken.isBlank()) {
			String hash = hashToken(refreshToken);
			userSessionRepository.findByRefreshTokenHash(hash)
					.ifPresent(session -> revokeSession(session, REFRESH_REVOKE_LOGOUT));
		}
		clearRefreshCookie(response);
	}

	private V2AuthenticationResponse issueTokensForUser(
			User user, String deviceId, HttpServletRequest request, HttpServletResponse response) {
		revokeAllActiveSessionsForUser(user, REFRESH_REVOKE_NEW_LOGIN);

		String refreshToken = generateOpaqueToken();
		LocalDateTime now = LocalDateTime.now();
		UserSession session = new UserSession(user, hashToken(refreshToken), now.plus(Duration.ofMillis(refreshTokenExpirationMs)));
		session.setDeviceId(trimToLength(deviceId, 128));
		session.setIpAddress(resolveClientIp(request));
		session.setUserAgent(trimToLength(request.getHeader("User-Agent"), 512));
		session.setLastUsedAt(now);
		UserSession savedSession = userSessionRepository.save(session);

		String accessToken = jwtService.generateV2AccessToken(user, savedSession.getId(), accessTokenExpirationMs);
		setRefreshCookie(response, refreshToken);

		return new V2AuthenticationResponse(accessToken, accessTokenExpirationMs / 1000L, AUTH_VERSION);
	}

	private void revokeAllActiveSessionsForUser(User user, String reason) {
		if (user == null || user.getId() == null) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		var activeSessions = userSessionRepository.findByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(user.getId(), now);
		if (activeSessions.isEmpty()) {
			return;
		}
		activeSessions.forEach(session -> {
			session.setRevokedAt(now);
			session.setRevokeReason(reason);
		});
		userSessionRepository.saveAll(activeSessions);
	}

	private void revokeSession(UserSession session, String reason) {
		session.setRevokedAt(LocalDateTime.now());
		session.setRevokeReason(reason);
		userSessionRepository.save(session);
	}

	private String generateOpaqueToken() {
		byte[] randomBytes = new byte[48];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 algorithm not available", exception);
		}
	}

	private String extractCookieValue(HttpServletRequest request, String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length == 0) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookieName.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private void setRefreshCookie(HttpServletResponse response, String value) {
		ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(refreshCookieName, value)
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.path(refreshCookiePath)
				.sameSite(refreshCookieSameSite)
				.maxAge(Duration.ofMillis(refreshTokenExpirationMs));
		if (refreshCookieDomain != null && !refreshCookieDomain.isBlank()) {
			cookieBuilder.domain(refreshCookieDomain);
		}
		response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
	}

	private void clearRefreshCookie(HttpServletResponse response) {
		ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(refreshCookieName, "")
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.path(refreshCookiePath)
				.sameSite(refreshCookieSameSite)
				.maxAge(Duration.ZERO);
		if (refreshCookieDomain != null && !refreshCookieDomain.isBlank()) {
			cookieBuilder.domain(refreshCookieDomain);
		}
		response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			String[] ips = forwardedFor.split(",");
			if (ips.length > 0) {
				return trimToLength(ips[0].trim(), 64);
			}
		}
		return trimToLength(request.getRemoteAddr(), 64);
	}

	private String trimToLength(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private long elapsedMs(long startedAtNanos) {
		return (System.nanoTime() - startedAtNanos) / 1_000_000L;
	}

	private String maskHash(String hash) {
		if (hash == null || hash.isBlank()) {
			return "none";
		}
		int prefixLength = Math.min(10, hash.length());
		return hash.substring(0, prefixLength);
	}
}
