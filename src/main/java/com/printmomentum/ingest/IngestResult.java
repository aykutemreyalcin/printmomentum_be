package com.printmomentum.ingest;

import java.util.UUID;

public record IngestResult(UUID crawlRunId, int stored, int skipped) {
}
