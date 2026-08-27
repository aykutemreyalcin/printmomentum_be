package com.printmomentum.web;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ListingsController {

	private static final int MAX_PAGE_SIZE = 200;

	private final ListingFeedService listingFeedService;
	private final FavoriteService favoriteService;

	public ListingsController(ListingFeedService listingFeedService, FavoriteService favoriteService) {
		this.listingFeedService = listingFeedService;
		this.favoriteService = favoriteService;
	}

	@GetMapping("/listings/top-chart")
	public TopChartResponse topChart(
			@RequestParam(defaultValue = "30") int limit,
			@RequestParam(defaultValue = "90") int snapshotLimit) {
		if (limit < 1 || limit > 50) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..50");
		}
		if (snapshotLimit < 1 || snapshotLimit > 200) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshotLimit must be 1..200");
		}
		return listingFeedService.topChart(limit, snapshotLimit);
	}

	@GetMapping("/listings")
	public ListingPageResponse list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) Integer maxDaysToTop,
			@RequestParam(required = false) BigDecimal minScore,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Long shopId,
			@RequestParam(required = false) String preset,
			@RequestParam(required = false) Boolean bestseller) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be 1..200");
		}
		return listingFeedService.list(page, size, maxDaysToTop, minScore, q, shopId, preset, bestseller);
	}

	@GetMapping("/favorites")
	public ListingPageResponse favorites(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be 1..200");
		}
		return listingFeedService.favorites(page, size);
	}

	@GetMapping("/listings/{id}")
	public ListingDetailResponse detail(
			@PathVariable long id,
			@RequestParam(defaultValue = "20") int snapshotLimit,
			@RequestParam(defaultValue = "false") boolean debug) {
		if (snapshotLimit < 1 || snapshotLimit > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "snapshotLimit must be 1..200");
		}
		ListingDetailResponse detail = listingFeedService.detail(id, snapshotLimit, debug);
		if (detail == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "listing not found");
		}
		return detail;
	}

	@PutMapping("/listings/{id}/favorite")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void addFavorite(@PathVariable long id) {
		favoriteService.add(id);
	}

	@DeleteMapping("/listings/{id}/favorite")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeFavorite(@PathVariable long id) {
		favoriteService.remove(id);
	}
}
