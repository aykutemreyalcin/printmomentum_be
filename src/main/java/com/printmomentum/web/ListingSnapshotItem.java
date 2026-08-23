package com.printmomentum.web;

import java.time.Instant;

public record ListingSnapshotItem(Instant observedAt, int position, int numFavorers) {
}
