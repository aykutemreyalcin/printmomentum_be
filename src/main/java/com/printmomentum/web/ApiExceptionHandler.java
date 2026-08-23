package com.printmomentum.web;

import com.printmomentum.ingest.EtsyUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
		return problem(ex.getStatusCode(), ex.getReason());
	}

	@ExceptionHandler(EtsyUnavailableException.class)
	public ProblemDetail handleEtsyUnavailable(EtsyUnavailableException ex) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Etsy Open API is unavailable");
	}

	private static ProblemDetail problem(HttpStatusCode status, String detail) {
		ProblemDetail body = ProblemDetail.forStatus(status);
		body.setTitle(title(status));
		if (detail != null && !detail.isBlank()) {
			body.setDetail(detail);
		}
		return body;
	}

	private static String title(HttpStatusCode status) {
		HttpStatus resolved = HttpStatus.resolve(status.value());
		return resolved != null ? resolved.getReasonPhrase() : "Error";
	}
}
