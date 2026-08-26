package com.printmomentum.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1/shops", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShopsController {

	private final ListingFeedService listingFeedService;

	public ShopsController(ListingFeedService listingFeedService) {
		this.listingFeedService = listingFeedService;
	}

	@GetMapping("/{id}")
	public ShopResponse shop(@PathVariable long id) {
		ShopResponse shop = listingFeedService.shop(id);
		if (shop == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "shop not found");
		}
		return shop;
	}
}
