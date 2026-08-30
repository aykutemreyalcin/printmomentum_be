package com.printmomentum.web;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingEvent;
import com.printmomentum.domain.ListingEventRepository;
import com.printmomentum.domain.ListingEstimator;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingQueryHit;
import com.printmomentum.domain.ListingQueryHitRepository;
import com.printmomentum.domain.ListingRanker;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import com.printmomentum.domain.ListingSpecifications;
import com.printmomentum.domain.ListingTakeaway;
import com.printmomentum.domain.ListingTimeline;
import com.printmomentum.domain.MomentumPeriod;
import com.printmomentum.domain.PrintTeeClassifier;
import com.printmomentum.domain.QueryStats;
import com.printmomentum.domain.QueryStatsRepository;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopRepository;
import com.printmomentum.domain.User;
import com.printmomentum.domain.UserFavorite;
import com.printmomentum.domain.UserFavoriteRepository;
import com.printmomentum.util.CurrentUserHolder;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ListingFeedService {

	private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

	private final ListingRepository listingRepository;
	private final ListingSnapshotRepository listingSnapshotRepository;
	private final ListingQueryHitRepository listingQueryHitRepository;
	private final ListingEventRepository listingEventRepository;
	private final QueryStatsRepository queryStatsRepository;
	private final ShopRepository shopRepository;
	private final ListingRanker listingRanker;
	private final ListingEstimator listingEstimator;
	private final ListingTakeaway listingTakeaway;
	private final ListingTimeline listingTimeline;
	private final PrintTeeClassifier printTeeClassifier;
	private final UserFavoriteRepository userFavoriteRepository;
	private final CurrentUserHolder currentUserHolder;
	private final ObjectMapper objectMapper;

	public ListingFeedService(
			ListingRepository listingRepository,
			ListingSnapshotRepository listingSnapshotRepository,
			ListingQueryHitRepository listingQueryHitRepository,
			ListingEventRepository listingEventRepository,
			QueryStatsRepository queryStatsRepository,
			ShopRepository shopRepository,
			ListingRanker listingRanker,
			ListingEstimator listingEstimator,
			ListingTakeaway listingTakeaway,
			ListingTimeline listingTimeline,
			PrintTeeClassifier printTeeClassifier,
			UserFavoriteRepository userFavoriteRepository,
			CurrentUserHolder currentUserHolder,
			ObjectMapper objectMapper) {
		this.listingRepository = listingRepository;
		this.listingSnapshotRepository = listingSnapshotRepository;
		this.listingQueryHitRepository = listingQueryHitRepository;
		this.listingEventRepository = listingEventRepository;
		this.queryStatsRepository = queryStatsRepository;
		this.shopRepository = shopRepository;
		this.listingRanker = listingRanker;
		this.listingEstimator = listingEstimator;
		this.listingTakeaway = listingTakeaway;
		this.listingTimeline = listingTimeline;
		this.printTeeClassifier = printTeeClassifier;
		this.userFavoriteRepository = userFavoriteRepository;
		this.currentUserHolder = currentUserHolder;
		this.objectMapper = objectMapper;
	}

	@Transactional(readOnly = true)
	public ListingPageResponse list(
			int page,
			int size,
			Integer maxDaysToTop,
			BigDecimal minScore,
			String q,
			Long shopId,
			String preset,
			Boolean bestseller,
			String nicheSlug,
			String nicheWindow,
			MomentumPeriod momentumPeriod) {
		MomentumPeriod period = momentumPeriod == null ? MomentumPeriod.WEEKLY : momentumPeriod;
		List<Listing> matches = listingRepository
				.findAll(
						ListingSpecifications.printTeeFeed(
								minScore, q, shopId, preset, bestseller, nicheSlug, nicheWindow, Instant.now(), period),
						scoreSort(period))
				.stream()
				.filter(listing -> withinMaxDaysToTop(listing, maxDaysToTop))
				.filter(listing -> matchesClimbing(listing, preset))
				.toList();
		int from = Math.min(page * size, matches.size());
		int to = Math.min(from + size, matches.size());
		Set<Long> favoriteIds = favoriteIdsForCurrentUser();
		Map<Long, List<QueryHitItem>> hits = queryHitsFor(matches.subList(from, to));
		List<ListingFeedItem> items = matches.subList(from, to).stream()
				.map(listing -> toItem(listing, favoriteIds.contains(listing.getListingId()), hits.getOrDefault(listing.getListingId(), List.of()), period))
				.toList();
		return new ListingPageResponse(items, page, size, matches.size());
	}

	@Transactional(readOnly = true)
	public TopChartResponse topChart(int limit, int snapshotLimit, MomentumPeriod momentumPeriod) {
		MomentumPeriod period = momentumPeriod == null ? MomentumPeriod.WEEKLY : momentumPeriod;
		int cappedLimit = Math.min(Math.max(limit, 1), 50);
		int cappedSnapshots = Math.min(Math.max(snapshotLimit, 1), 200);
		List<Listing> top = listingRepository
				.findAll(
						ListingSpecifications.printTeeFeed(null, null, null, null, null, null, null, Instant.now(), period),
						scoreSort(period))
				.stream()
				.limit(cappedLimit)
				.toList();
		List<TopChartItem> items =
				top.stream().map(listing -> toTopChartItem(listing, cappedSnapshots, period)).toList();
		return new TopChartResponse(cappedLimit, cappedSnapshots, items);
	}

	@Transactional(readOnly = true)
	public ListingPageResponse favorites(int page, int size) {
		User user = currentUserHolder.getCurrentUser();
		if (user == null) {
			return new ListingPageResponse(List.of(), page, size, 0);
		}
		List<Listing> listings = userFavoriteRepository.findWithListingByUserId(user.getId()).stream()
				.map(UserFavorite::getListing)
				.toList();
		int from = Math.min(page * size, listings.size());
		int to = Math.min(from + size, listings.size());
		List<ListingFeedItem> items = listings.subList(from, to).stream()
				.map(listing -> toItem(
						listing,
						true,
						queryHitsFor(listings.subList(from, to)).getOrDefault(listing.getListingId(), List.of()),
						MomentumPeriod.WEEKLY))
				.toList();
		return new ListingPageResponse(items, page, size, listings.size());
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
		List<String> tags = readTags(listing.getTags());
		List<String> rejectReasons = debug
				? printTeeClassifier
						.classify(listing.getTitle(), listing.getDescription(), tags, listing.getTaxonomyId())
						.rejectReasons()
				: null;
		List<QueryHitItem> hits = queryHitsForListing(listing);
		List<QueryPeerItem> peers = queryPeers(hits);
		QueryPeerItem primary = peers.stream().min(Comparator.comparingInt(QueryPeerItem::position)).orElse(null);
		ListingFeedItem item = toItem(listing, favoriteIdsForCurrentUser().contains(listingId), hits, MomentumPeriod.WEEKLY);
		List<String> takeaway = listingTakeaway.lines(new ListingTakeaway.Input(
				item.daysToTop(),
				item.estSales30d(),
				item.etsyBestseller(),
				item.etsyBestsellerSince(),
				item.pmBestseller(),
				tags,
				item.price(),
				primary == null ? null : primary.medianPrice(),
				primary == null ? null : primary.query()));
		List<TimelinePointItem> timeline = listingTimeline
				.points(listing, listingEventRepository.findByListingListingIdOrderByObservedAtAscIdAsc(listingId))
				.stream()
				.map(point -> new TimelinePointItem(point.kind(), point.at(), point.label()))
				.toList();
		return new ListingDetailResponse(
				item.listingId(),
				item.title(),
				item.price(),
				item.currency(),
				item.imageUrl(),
				item.etsyUrl(),
				item.daysToTop(),
				item.momentumScore(),
				item.numFavorers(),
				item.shopName(),
				item.shopId(),
				item.shopSales(),
				item.shopRating(),
				item.shopReviewCount(),
				item.shopUrl(),
				item.shopAgeDays(),
				item.listingActiveCount(),
				item.views(),
				item.quantity(),
				quantityDelta(snapshots),
				item.ageDays(),
				item.originalCreatedAt(),
				item.firstSeenAt(),
				item.firstSeenInTopAt(),
				item.lastSeenAt(),
				item.lastReviewAt(),
				item.whoMade(),
				item.whenMade(),
				item.etsyBestseller(),
				item.etsyBestsellerSince(),
				item.etsyBestsellerEndedAt(),
				item.pmBestseller(),
				item.favorite(),
				item.reviews30d(),
				item.estSales30d(),
				item.estRevenue30d(),
				item.deltaFavorers7d(),
				item.deltaViews7d(),
				item.viewsPerDay(),
				item.queryHits(),
				tags,
				takeaway,
				peers,
				timeline,
				snapshots.stream().map(ListingFeedService::toSnapshotItem).toList(),
				rejectReasons);
	}

	@Transactional(readOnly = true)
	public ShopResponse shop(long shopId) {
		Shop shop = shopRepository.findById(shopId).orElse(null);
		if (shop == null) {
			return null;
		}
		return new ShopResponse(
				shop.getShopId(),
				shop.getName(),
				shop.getUrl(),
				shop.getIconUrl(),
				shop.getTransactionSoldCount(),
				shop.getListingActiveCount(),
				shop.getReviewCount(),
				shop.getReviewAverage(),
				shop.getEtsyCreatedAt(),
				ageDays(shop.getEtsyCreatedAt(), Instant.now()),
				listingRepository.countByShopShopIdAndPrintTeeTrue(shopId));
	}

	@Transactional(readOnly = true)
	public List<QueryStatsItem> latestQueryStats() {
		LocalDate day = queryStatsRepository.findLatestObservedDay().orElse(null);
		if (day == null) {
			return List.of();
		}
		return queryStatsRepository.findByIdObservedDayOrderByIdQueryAsc(day).stream()
				.map(stats -> new QueryStatsItem(
						stats.getQuery(),
						stats.getObservedDay(),
						stats.getListingCount(),
						stats.getEtsyCount(),
						stats.getMedianPrice(),
						stats.getMedianFavorers(),
						stats.getMedianViews()))
				.toList();
	}

	private boolean withinMaxDaysToTop(Listing listing, Integer maxDaysToTop) {
		if (maxDaysToTop == null) {
			return true;
		}
		Double days = listingRanker.daysToTop(listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt());
		return days != null && days <= maxDaysToTop;
	}

	private boolean matchesClimbing(Listing listing, String preset) {
		if (!"climbing".equals(preset)) {
			return true;
		}
		Double days = listingRanker.daysToTop(listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt());
		return days != null && days <= 7;
	}

	private Set<Long> favoriteIdsForCurrentUser() {
		User user = currentUserHolder.getCurrentUser();
		if (user == null || user.getId() == null) {
			return Set.of();
		}
		return new HashSet<>(userFavoriteRepository.findListingIdsByUserId(user.getId()));
	}

	private TopChartItem toTopChartItem(Listing listing, int snapshotLimit, MomentumPeriod period) {
		List<ListingSnapshot> snapshots = new ArrayList<>(listingSnapshotRepository.findByListingListingIdOrderByObservedAtDescIdDesc(
				listing.getListingId(), PageRequest.of(0, snapshotLimit)));
		Collections.reverse(snapshots);
		return new TopChartItem(
				listing.getListingId(),
				listing.getTitle(),
				imageUrl(listing),
				listing.getUrl(),
				momentumScore(listing, period),
				listingRanker.daysToTop(listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt()),
				listing.getNumFavorers(),
				listing.getViews(),
				snapshots.stream().map(ListingFeedService::toSnapshotItem).toList());
	}

	private ListingFeedItem toItem(Listing listing, boolean favorite, List<QueryHitItem> queryHits, MomentumPeriod period) {
		Instant now = Instant.now();
		Instant created = listing.getOriginalCreatedAt() != null
				? listing.getOriginalCreatedAt()
				: listing.getEtsyCreatedAt();
		Shop shop = listing.getShop();
		ListingEstimator.Estimate estimate = listingEstimator.estimate(
				listing.getReviews30d(), listing.getPriceAmount(), listing.getViews(), created, now);
		return new ListingFeedItem(
				listing.getListingId(),
				listing.getTitle(),
				listing.getPriceAmount(),
				listing.getCurrency(),
				imageUrl(listing),
				listing.getUrl(),
				listingRanker.daysToTop(listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt()),
				momentumScore(listing, period),
				listing.getNumFavorers(),
				shop.getName(),
				shop.getShopId(),
				shop.getTransactionSoldCount(),
				shop.getReviewAverage(),
				shop.getReviewCount(),
				shop.getUrl(),
				ageDays(shop.getEtsyCreatedAt(), now),
				shop.getListingActiveCount(),
				listing.getViews(),
				listing.getQuantity(),
				ageDays(created, now),
				listing.getOriginalCreatedAt(),
				listing.getFirstSeenAt(),
				listing.getFirstSeenInTopAt(),
				listing.getLastSeenAt(),
				listing.getLastReviewAt(),
				listing.getWhoMade(),
				listing.getWhenMade(),
				listing.isEtsyBestseller(),
				listing.getEtsyBestsellerSince(),
				listing.getEtsyBestsellerEndedAt(),
				listing.isPmBestseller(),
				favorite,
				listing.getReviews30d(),
				estimate.estSales30d(),
				estimate.estRevenue30d(),
				listing.getDeltaFavorers7d(),
				listing.getDeltaViews7d(),
				estimate.viewsPerDay(),
				queryHits);
	}

	private Map<Long, List<QueryHitItem>> queryHitsFor(List<Listing> listings) {
		if (listings.isEmpty()) {
			return Map.of();
		}
		LocalDate day = Instant.now().atZone(ISTANBUL).toLocalDate();
		List<Long> ids = listings.stream().map(Listing::getListingId).toList();
		Map<Long, List<QueryHitItem>> hits = new LinkedHashMap<>();
		for (ListingQueryHit hit : listingQueryHitRepository.findByIdObservedDayAndListingListingIdIn(day, ids)) {
			hits.computeIfAbsent(hit.getListingId(), key -> new ArrayList<>())
					.add(new QueryHitItem(hit.getQuery(), hit.getPosition()));
		}
		return hits;
	}

	private List<QueryHitItem> queryHitsForListing(Listing listing) {
		LocalDate day = listingQueryHitRepository.findLatestDayForListing(listing.getListingId()).orElse(null);
		if (day == null) {
			return List.of();
		}
		return listingQueryHitRepository.findByListingListingIdAndIdObservedDay(listing.getListingId(), day).stream()
				.map(hit -> new QueryHitItem(hit.getQuery(), hit.getPosition()))
				.toList();
	}

	private List<QueryPeerItem> queryPeers(List<QueryHitItem> hits) {
		if (hits.isEmpty()) {
			return List.of();
		}
		LocalDate day = queryStatsRepository.findLatestObservedDay().orElse(null);
		if (day == null) {
			return hits.stream()
					.map(hit -> new QueryPeerItem(hit.query(), hit.position(), 0, null, null, null, null))
					.toList();
		}
		List<String> queries = hits.stream().map(QueryHitItem::query).toList();
		Map<String, QueryStats> byQuery = queryStatsRepository.findByIdQueryInAndIdObservedDay(queries, day).stream()
				.collect(Collectors.toMap(QueryStats::getQuery, stats -> stats, (left, right) -> left));
		List<QueryPeerItem> peers = new ArrayList<>();
		for (QueryHitItem hit : hits) {
			QueryStats stats = byQuery.get(hit.query());
			if (stats == null) {
				peers.add(new QueryPeerItem(hit.query(), hit.position(), 0, null, null, null, null));
			} else {
				peers.add(new QueryPeerItem(
						hit.query(),
						hit.position(),
						stats.getListingCount(),
						stats.getEtsyCount(),
						stats.getMedianPrice(),
						stats.getMedianFavorers(),
						stats.getMedianViews()));
			}
		}
		return List.copyOf(peers);
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
			if (!tag.isNull() && values.size() < 13) {
				values.add(tag.asString());
			}
		}
		return List.copyOf(values);
	}

	private static ListingSnapshotItem toSnapshotItem(ListingSnapshot snapshot) {
		return new ListingSnapshotItem(
				snapshot.getObservedAt(),
				snapshot.getPosition(),
				snapshot.getNumFavorers(),
				snapshot.getViews(),
				snapshot.getQuantity());
	}

	private static Integer quantityDelta(List<ListingSnapshot> snapshots) {
		if (snapshots.size() < 2) {
			return null;
		}
		Integer previous = snapshots.get(snapshots.size() - 2).getQuantity();
		Integer current = snapshots.get(snapshots.size() - 1).getQuantity();
		if (previous == null || current == null) {
			return null;
		}
		return current - previous;
	}

	private static Double ageDays(Instant created, Instant now) {
		if (created == null || now == null) {
			return null;
		}
		return Duration.between(created, now).toMinutes() / (60.0 * 24.0);
	}

	private static BigDecimal momentumScore(Listing listing, MomentumPeriod period) {
		return switch (period == null ? MomentumPeriod.WEEKLY : period) {
			case DAILY -> listing.getLastScore();
			case WEEKLY -> listing.getLastScoreWeekly();
			case MONTHLY -> listing.getLastScoreMonthly();
		};
	}

	private static Sort scoreSort(MomentumPeriod period) {
		MomentumPeriod active = period == null ? MomentumPeriod.WEEKLY : period;
		return Sort.by(Sort.Order.desc(active.sortField()).nullsLast(), Sort.Order.desc("listingId"));
	}
}
