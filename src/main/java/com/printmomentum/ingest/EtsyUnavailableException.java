package com.printmomentum.ingest;

public class EtsyUnavailableException extends RuntimeException {

	public EtsyUnavailableException(String message) {
		super(message);
	}

	public EtsyUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
