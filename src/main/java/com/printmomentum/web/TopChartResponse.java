package com.printmomentum.web;

import java.util.List;

public record TopChartResponse(int limit, int snapshotLimit, List<TopChartItem> items) {
}
