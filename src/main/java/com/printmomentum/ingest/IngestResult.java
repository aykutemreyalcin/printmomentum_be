package com.printmomentum.ingest;

import java.util.UUID;

public record IngestResult(UUID crawlRunId, int matchedPrintTees, int rejectedNonPrintTees, int newListings) {

	public int stored() {
		return matchedPrintTees;
	}

	public int skipped() {
		return rejectedNonPrintTees;
	}
}
