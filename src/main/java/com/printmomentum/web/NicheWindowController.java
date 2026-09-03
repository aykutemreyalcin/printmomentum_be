package com.printmomentum.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1/niches", produces = MediaType.APPLICATION_JSON_VALUE)
public class NicheWindowController {

	private static final int MAX_PAGE_SIZE = 100;

	private final NicheWindowService nicheWindowService;
	private final com.printmomentum.niche.NicheReindexJob nicheReindexJob;

	public NicheWindowController(
			NicheWindowService nicheWindowService,
			com.printmomentum.niche.NicheReindexJob nicheReindexJob) {
		this.nicheWindowService = nicheWindowService;
		this.nicheReindexJob = nicheReindexJob;
	}

	@GetMapping
	public NichePageResponse list(
			@RequestParam(required = false) String window,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "momentum") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "30") int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0 and size must be 1..100");
		}
		return nicheWindowService.list(window, q, sort, page, size);
	}

	@GetMapping("/stats")
	public NicheStatsResponse stats() {
		return nicheWindowService.stats();
	}

	@GetMapping("/{slug}")
	public NicheDetailResponse detail(@PathVariable String slug) {
		NicheDetailResponse detail = nicheWindowService.detail(slug);
		if (detail == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "niche not found");
		}
		return detail;
	}

	@GetMapping("/{slug}/listings")
	public java.util.List<NicheTopListingItem> listings(
			@PathVariable String slug,
			@RequestParam(defaultValue = "30") int limit) {
		if (limit < 1 || limit > MAX_PAGE_SIZE) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..100");
		}
		return nicheWindowService.listings(slug, limit);
	}

	@PostMapping("/reindex")
	@PreAuthorize("hasRole('admin')")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void reindex() {
		if (!nicheReindexJob.triggerAsync()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "niche reindex already running");
		}
	}
}
