package com.printmomentum.niche;

public enum NicheWindowState {
	OPEN,
	CLOSING,
	CLOSED,
	LOW_DATA;

	public static NicheWindowState parse(String value) {
		if (value == null || value.isBlank()) {
			return LOW_DATA;
		}
		return NicheWindowState.valueOf(value.trim().toUpperCase());
	}
}
