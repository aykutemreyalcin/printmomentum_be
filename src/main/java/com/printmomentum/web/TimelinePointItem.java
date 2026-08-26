package com.printmomentum.web;

import java.time.Instant;

public record TimelinePointItem(String kind, Instant at, String label) {
}
