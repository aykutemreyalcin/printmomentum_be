package com.printmomentum.web;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingRanker;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSpecifications;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListingFeedService {

	private final ListingRepository listingRepository;
	private final ListingRanker listingRanker;

	public ListingFeedService(ListingRepository listingRepository, ListingRanker listingRanker) {
		this.listingRepository = listingRepository;
		this.listingRanker = listingRanker;
	}

	@Transactional(readOnly = true)
	public ListingPageResponse list(int page, int size, Integer maxDaysToTop, BigDecimal minScore, String q) {
		List<Listing> matches = listingRepository
				.findAll(ListingSpecifications.printTeeFeed(minScore, q), scoreSort())
				.stream()
				.filter(listing -> withinMaxDaysToTop(listing, maxDaysToTop))
				.toList();
		int from = Math.min(page * size, matches.size());
		int to = Math.min(from + size, matches.size());
		List<ListingFeedItem> items = matches.subList(from, to).stream().map(this::toItem).toList();
		return new ListingPageResponse(items, page, size, matches.size());
	}

	private boolean withinMaxDaysToTop(Listing listing, Integer maxDaysToTop) {
		if (maxDaysToTop == null) {
			return true;
		}
		Double days = listingRanker.daysToTop(listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt());
		return days != null && days <= maxDaysToTop;
	}

	private ListingFeedItem toItem(Listing listing) {
		return new ListingFeedItem(
				listing.getListingId(),
				listing.getTitle(),
				listing.getPriceAmount(),
				listing.getCurrency(),
				imageUrl(listing),
				listing.getUrl(),
				listingRanker.daysToTop(listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt()),
				listing.getLastScore(),
				listing.getNumFavorers(),
				listing.getShop().getName());
	}

	private static String imageUrl(Listing listing) {
		return listing.getImages().stream()
				.min(Comparator.comparingInt(ListingImage::getRank))
				.map(ListingImage::getUrl)
				.orElse(null);
	}

	private static Sort scoreSort() {
		return Sort.by(Sort.Order.desc("lastScore").nullsLast(), Sort.Order.desc("listingId"));
	}
}
