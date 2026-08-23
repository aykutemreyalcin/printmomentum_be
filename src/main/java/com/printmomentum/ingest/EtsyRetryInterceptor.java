package com.printmomentum.ingest;

import com.printmomentum.config.EtsyProperties;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public final class EtsyRetryInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger log = LoggerFactory.getLogger(EtsyRetryInterceptor.class);

	private final EtsyProperties properties;
	private final EtsyQuotaTracker quotaTracker;
	private final Object rateLock = new Object();
	private long nextPermittedNanos;

	public EtsyRetryInterceptor(EtsyProperties properties, EtsyQuotaTracker quotaTracker) {
		this.properties = properties;
		this.quotaTracker = quotaTracker;
	}

	@Override
	public ClientHttpResponse intercept(
			HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		int attempts = properties.maxRetries() + 1;
		IOException lastIo = null;
		for (int attempt = 1; attempt <= attempts; attempt++) {
			throttle();
			try {
				ClientHttpResponse response = execution.execute(request, body);
				logQuota(response.getStatusCode(), response.getHeaders());
				if (!shouldRetry(response.getStatusCode()) || attempt == attempts) {
					return response;
				}
				response.close();
				park(waitBeforeRetry(attempt, response.getHeaders()));
			} catch (IOException ex) {
				lastIo = ex;
				if (attempt == attempts) {
					throw ex;
				}
				park(waitBeforeRetry(attempt, HttpHeaders.EMPTY));
			}
		}
		throw lastIo != null ? lastIo : new IOException("Etsy request failed after retries");
	}

	private void throttle() {
		int maxRps = properties.maxRequestsPerSecond();
		if (maxRps <= 0) {
			return;
		}
		long intervalNanos = Duration.ofSeconds(1).toNanos() / maxRps;
		synchronized (rateLock) {
			long now = System.nanoTime();
			long wait = nextPermittedNanos - now;
			if (wait > 0) {
				park(Duration.ofNanos(wait));
				now = System.nanoTime();
			}
			nextPermittedNanos = now + intervalNanos;
		}
	}

	private static boolean shouldRetry(HttpStatusCode status) {
		return status.value() == 429 || status.is5xxServerError();
	}

	private Duration waitBeforeRetry(int attempt, HttpHeaders headers) {
		Duration retryAfter = parseRetryAfter(headers);
		if (retryAfter != null) {
			return retryAfter;
		}
		long factor = 1L << Math.min(attempt - 1, 8);
		return properties.retryBackoff().multipliedBy(factor);
	}

	private static Duration parseRetryAfter(HttpHeaders headers) {
		String raw = headers.getFirst("retry-after");
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			long seconds = Long.parseLong(raw.trim());
			if (seconds <= 0) {
				return Duration.ZERO;
			}
			return Duration.ofSeconds(Math.min(seconds, 5));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private void logQuota(HttpStatusCode status, HttpHeaders headers) {
		String remainingToday = headers.getFirst("x-remaining-today");
		quotaTracker.recordRemainingToday(parseRemaining(remainingToday));
		log.info(
				"etsy status={} remainingThisSecond={} remainingToday={} limitPerSecond={} limitPerDay={}",
				status.value(),
				headers.getFirst("x-remaining-this-second"),
				remainingToday,
				headers.getFirst("x-limit-per-second"),
				headers.getFirst("x-limit-per-day"));
	}

	private static Integer parseRemaining(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static void park(Duration wait) {
		if (wait == null || wait.isZero() || wait.isNegative()) {
			return;
		}
		LockSupport.parkNanos(wait.toNanos());
	}
}
