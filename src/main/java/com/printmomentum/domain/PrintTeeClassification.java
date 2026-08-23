package com.printmomentum.domain;

import java.util.List;

public record PrintTeeClassification(double score, boolean printTee, List<String> rejectReasons) {

	public PrintTeeClassification {
		rejectReasons = List.copyOf(rejectReasons);
	}
}
