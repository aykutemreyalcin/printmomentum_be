package com.printmomentum.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/query-stats", produces = MediaType.APPLICATION_JSON_VALUE)
public class QueryStatsController {

	private final ListingFeedService listingFeedService;

	public QueryStatsController(ListingFeedService listingFeedService) {
		this.listingFeedService = listingFeedService;
	}

	@GetMapping
	public List<QueryStatsItem> latest() {
		return listingFeedService.latestQueryStats();
	}
}
