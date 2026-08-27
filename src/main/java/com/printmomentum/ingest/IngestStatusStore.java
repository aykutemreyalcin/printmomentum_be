package com.printmomentum.ingest;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class IngestStatusStore {

	public enum Outcome {
		never,
		ok,
		skipped_quota,
		error
	}

	private volatile Instant lastAttemptAt;
	private volatile Instant lastFinishedAt;
	private volatile Integer lastMatchedPrintTees;
	private volatile Integer lastRejectedNonPrintTees;
	private volatile Integer lastNewListings;
	private volatile Integer lastStored;
	private volatile Integer lastSkipped;
	private volatile String lastError;
	private volatile Outcome lastOutcome = Outcome.never;

	public void markStarted(Instant at) {
		this.lastAttemptAt = at;
	}

	public void markOk(Instant at, int matchedPrintTees, int rejectedNonPrintTees, int newListings) {
		this.lastFinishedAt = at;
		this.lastMatchedPrintTees = matchedPrintTees;
		this.lastRejectedNonPrintTees = rejectedNonPrintTees;
		this.lastNewListings = newListings;
		this.lastStored = matchedPrintTees;
		this.lastSkipped = rejectedNonPrintTees;
		this.lastError = null;
		this.lastOutcome = Outcome.ok;
	}

	public void markSkippedQuota(Instant at) {
		this.lastAttemptAt = at;
		this.lastError = null;
		this.lastOutcome = Outcome.skipped_quota;
	}

	public void markError(Instant at, String message) {
		this.lastAttemptAt = at;
		this.lastError = message == null ? "ingest failed" : message;
		this.lastOutcome = Outcome.error;
	}

	public Instant lastAttemptAt() {
		return lastAttemptAt;
	}

	public Instant lastFinishedAt() {
		return lastFinishedAt;
	}

	public Integer lastMatchedPrintTees() {
		return lastMatchedPrintTees;
	}

	public Integer lastRejectedNonPrintTees() {
		return lastRejectedNonPrintTees;
	}

	public Integer lastNewListings() {
		return lastNewListings;
	}

	public Integer lastStored() {
		return lastStored;
	}

	public Integer lastSkipped() {
		return lastSkipped;
	}

	public String lastError() {
		return lastError;
	}

	public Outcome lastOutcome() {
		return lastOutcome;
	}
}
