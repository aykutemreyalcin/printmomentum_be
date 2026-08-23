package com.printmomentum.web;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingRanker;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import com.printmomentum.domain.ListingSpecifications;
import com.printmomentum.domain.PrintTeeClassifier;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ListingFeedService {

	private final ListingRepository listingRepository;
	private final ListingSnapshotRepository listingSnapshotRepository;
	private final ListingRanker listingRanker;
	private final PrintTeeClassifier printTeeClassifier;
	private final ObjectMapper objectMapper;

	public ListingFeedService(
			ListingRepository listingRepository,
			ListingSnapshotRepository listingSnapshotRepository,
			ListingRanker listingRanker,
			PrintTeeClassifier printTeeClassifier,
			ObjectMapper objectMapper) {
		this.listingRepository = listingRepository;
		this.listingSnapshotRepository = listingSnapshotRepository;
		this.listingRanker = listingRanker;
		this.printTeeClassifier = printTeeClassifier;
		this.objectMapper = objectMapper;
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

	@Transactional(readOnly = true)
	public ListingDetailResponse detail(long listingId, int snapshotLimit, boolean debug) {
		Listing listing = listingRepository.findById(listingId).orElse(null);
		if (listing == null) {
			return null;
		}
		List<ListingSnapshot> snapshots = new ArrayList<>(listingSnapshotRepository
				.findByListingListingIdOrderByObservedAtDescIdDesc(listingId, PageRequest.of(0, snapshotLimit)));
		Collections.reverse(snapshots);
		List<String> rejectReasons = debug
				? printTeeClassifier
						.classify(
								listing.getTitle(),
								listing.getDescription(),
								readTags(listing.getTags()),
								listing.getTaxonomyId())
						.rejectReasons()
				: null;
		return new ListingDetailResponse(
				listing.getListingId(),
				listing.getTitle(),
				listing.getPriceAmount(),
				listing.getCurrency(),
				imageUrl(listing),
				listing.getUrl(),
				listingRanker.daysToTop(listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt()),
				listing.getLastScore(),
				listing.getNumFavorers(),
				listing.getShop().getName(),
				snapshots.stream().map(ListingFeedService::toSnapshotItem).toList(),
				rejectReasons);
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

	private List<String> readTags(String tagsJson) {
		if (tagsJson == null || tagsJson.isBlank()) {
			return List.of();
		}
		JsonNode tags = objectMapper.readTree(tagsJson);
		if (!tags.isArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>();
		for (JsonNode tag : tags) {
			if (!tag.isNull()) {
				values.add(tag.asString());
			}
		}
		return List.copyOf(values);
	}

	private static ListingSnapshotItem toSnapshotItem(ListingSnapshot snapshot) {
		return new ListingSnapshotItem(
				snapshot.getObservedAt(), snapshot.getPosition(), snapshot.getNumFavorers());
	}

	private static Sort scoreSort() {
		return Sort.by(Sort.Order.desc("lastScore").nullsLast(), Sort.Order.desc("listingId"));
	}
}
