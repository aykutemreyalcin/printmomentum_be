package com.printmomentum.web;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ListingsController {

	private static final int MAX_PAGE_SIZE = 100;

	private final ListingFeedService listingFeedService;

	public ListingsController(ListingFeedService listingFeedService) {
		this.listingFeedService = listingFeedService;
	}

	@GetMapping("/listings")
	public ListingPageResponse list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) Integer maxDaysToTop,
			@RequestParam(required = false) BigDecimal minScore,
			@RequestParam(required = false) String q) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be 1..100");
		}
		return listingFeedService.list(page, size, maxDaysToTop, minScore, q);
	}

	@GetMapping("/listings/{id}")
	public ListingDetailResponse detail(
			@PathVariable long id,
			@RequestParam(defaultValue = "20") int snapshotLimit,
			@RequestParam(defaultValue = "false") boolean debug) {
		if (snapshotLimit < 1 || snapshotLimit > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshotLimit must be 1..100");
		}
		ListingDetailResponse detail = listingFeedService.detail(id, snapshotLimit, debug);
		if (detail == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "listing not found");
		}
		return detail;
	}
}
