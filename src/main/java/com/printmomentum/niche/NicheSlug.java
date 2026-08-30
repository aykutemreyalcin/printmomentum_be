package com.printmomentum.niche;

import java.util.Locale;

public final class NicheSlug {

	private NicheSlug() {
	}

	public static String fromLabel(String label) {
		if (label == null || label.isBlank()) {
			return "";
		}
		String normalized = label.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
		normalized = normalized.replaceAll("^-+|-+$", "");
		return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
	}
}
