package com.printmomentum.web;

import java.util.List;

public record ListingPageResponse(List<ListingFeedItem> items, int page, int size, long total) {
}
