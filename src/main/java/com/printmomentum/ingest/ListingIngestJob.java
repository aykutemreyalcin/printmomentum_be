package com.printmomentum.ingest;

import com.printmomentum.config.IngestProperties;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingQueryHit;
import com.printmomentum.domain.ListingQueryHitId;
import com.printmomentum.domain.ListingQueryHitRepository;
import com.printmomentum.domain.ListingRanker;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import com.printmomentum.domain.PrintTeeClassification;
import com.printmomentum.domain.PrintTeeClassifier;
import com.printmomentum.domain.QueryStats;
import com.printmomentum.domain.QueryStatsCalculator;
import com.printmomentum.domain.QueryStatsId;
import com.printmomentum.domain.QueryStatsRepository;
import com.printmomentum.domain.ReviewWindow;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopCrawlQueue;
import com.printmomentum.domain.ShopCrawlQueueRepository;
import com.printmomentum.domain.ShopRepository;
import com.printmomentum.domain.SnapshotDeltas;
import com.printmomentum.domain.SnapshotTrendSignals;
import com.printmomentum.storage.ListingImageCache;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

@Component
public class ListingIngestJob {

	private static final Logger log = LoggerFactory.getLogger(ListingIngestJob.class);
	private static final int TITLE_MAX = 255;
	private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

	private record PersistResult(boolean accepted, boolean newToIndex) {
	}

	private record IngestRunCounts(int matchedPrintTees, int rejectedNonPrintTees, int newListings) {
	}

	private final EtsyClient etsyClient;
	private final PrintTeeClassifier classifier;
	private final IngestProperties properties;
	private final ShopRepository shopRepository;
	private final ShopCrawlQueueRepository shopCrawlQueueRepository;
	private final ListingRepository listingRepository;
	private final ListingSnapshotRepository listingSnapshotRepository;
	private final ListingQueryHitRepository listingQueryHitRepository;
	private final ListingRanker listingRanker;
	private final SnapshotDeltas snapshotDeltas;
	private final SnapshotTrendSignals snapshotTrendSignals = new SnapshotTrendSignals();
	private final QueryStatsCalculator queryStatsCalculator;
	private final QueryStatsRepository queryStatsRepository;
	private final ReviewWindow reviewWindow;
	private final ListingImageCache listingImageCache;
	private final EtsyQuotaTracker quotaTracker;
	private final BestsellerMarker bestsellerMarker;
	private final IngestStatusStore ingestStatusStore;
	private final TransactionTemplate transactionTemplate;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final com.printmomentum.niche.NicheTermService nicheTermService;

	public ListingIngestJob(
			EtsyClient etsyClient,
			PrintTeeClassifier classifier,
			IngestProperties properties,
			ShopRepository shopRepository,
			ShopCrawlQueueRepository shopCrawlQueueRepository,
		 ListingRepository listingRepository,
			ListingSnapshotRepository listingSnapshotRepository,
			ListingQueryHitRepository listingQueryHitRepository,
			ListingRanker listingRanker,
			SnapshotDeltas snapshotDeltas,
			QueryStatsCalculator queryStatsCalculator,
			QueryStatsRepository queryStatsRepository,
			ReviewWindow reviewWindow,
			ListingImageCache listingImageCache,
			EtsyQuotaTracker quotaTracker,
			BestsellerMarker bestsellerMarker,
			IngestStatusStore ingestStatusStore,
			PlatformTransactionManager transactionManager,
			ObjectMapper objectMapper,
			com.printmomentum.niche.NicheTermService nicheTermService,
			@Autowired(required = false) Clock clock) {
		this.etsyClient = etsyClient;
		this.classifier = classifier;
		this.properties = properties;
		this.shopRepository = shopRepository;
		this.shopCrawlQueueRepository = shopCrawlQueueRepository;
		this.listingRepository = listingRepository;
		this.listingSnapshotRepository = listingSnapshotRepository;
		this.listingQueryHitRepository = listingQueryHitRepository;
		this.listingRanker = listingRanker;
		this.snapshotDeltas = snapshotDeltas;
		this.queryStatsCalculator = queryStatsCalculator;
		this.queryStatsRepository = queryStatsRepository;
		this.reviewWindow = reviewWindow;
		this.listingImageCache = listingImageCache;
		this.quotaTracker = quotaTracker;
		this.bestsellerMarker = bestsellerMarker;
		this.ingestStatusStore = ingestStatusStore;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.objectMapper = objectMapper;
		this.nicheTermService = nicheTermService;
		this.clock = clock != null ? clock : Clock.systemUTC();
	}

	@Scheduled(cron = "${printmomentum.ingest.cron:-}", zone = "Europe/Istanbul")
	public void ingestOnSchedule() {
		run();
	}

	public IngestResult run() {
		Instant startedAt = clock.instant();
		ingestStatusStore.markStarted(startedAt);
		try {
			return runInternal(startedAt);
		} catch (RuntimeException exception) {
			ingestStatusStore.markError(Instant.now(), exception.getMessage());
			throw exception;
		}
	}

	private IngestResult runInternal(Instant startedAt) {
		Integer remaining = quotaTracker.remainingToday();
		if (remaining != null && remaining < properties.minRemainingToday()) {
			log.warn(
					"ingest skip remaining-today={} threshold={}",
					remaining,
					properties.minRemainingToday());
			ingestStatusStore.markSkippedQuota(startedAt);
			return new IngestResult(null, 0, 0, 0);
		}

		UUID crawlRunId = UUID.randomUUID();
		Instant observedAt = startedAt;
		int matchedPrintTees = 0;
		int rejectedNonPrintTees = 0;
		int newListings = 0;
		Set<Long> seenListingIds = new HashSet<>();
		DiscoveryMode mode = DiscoveryMode.forInstant(observedAt);
		log.info("ingest start crawl_run_id={} mode={}", crawlRunId, mode);

		switch (mode) {
			case SHOP_EXPANSION -> {
				IngestRunCounts counts = runShopExpansion(crawlRunId, observedAt, seenListingIds);
				matchedPrintTees += counts.matchedPrintTees();
				rejectedNonPrintTees += counts.rejectedNonPrintTees();
				newListings += counts.newListings();
			}
			case BENCHMARK -> {
				IngestRunCounts counts = runBenchmarkQueries(crawlRunId, observedAt, seenListingIds);
				matchedPrintTees += counts.matchedPrintTees();
				rejectedNonPrintTees += counts.rejectedNonPrintTees();
				newListings += counts.newListings();
			}
			default -> {
				IngestRunCounts counts = runTaxonomySweep(mode, crawlRunId, observedAt, seenListingIds);
				matchedPrintTees += counts.matchedPrintTees();
				rejectedNonPrintTees += counts.rejectedNonPrintTees();
				newListings += counts.newListings();
			}
		}

		int staleRefreshed = refreshStaleListings(crawlRunId, observedAt, seenListingIds);
		refreshReviewsIfNeeded(observedAt);
		bestsellerMarker.refresh(observedAt, seenListingIds);
		log.info(
				"ingest done crawl_run_id={} mode={} matched={} rejected={} new={} staleRefreshed={}",
				crawlRunId,
				mode,
				matchedPrintTees,
				rejectedNonPrintTees,
				newListings,
				staleRefreshed);
		ingestStatusStore.markOk(Instant.now(), matchedPrintTees, rejectedNonPrintTees, newListings);
		return new IngestResult(crawlRunId, matchedPrintTees, rejectedNonPrintTees, newListings);
	}

	private IngestRunCounts runTaxonomySweep(
			DiscoveryMode mode, UUID crawlRunId, Instant observedAt, Set<Long> seenListingIds) {
		int matchedPrintTees = 0;
		int rejectedNonPrintTees = 0;
		int newListings = 0;
		int pages = properties.effectivePagesPerSweep();
		Long taxonomyId = properties.taxonomyId();
		String source = mode.sourceKey();
		int etsyCount = 0;
		List<QueryStatsCalculator.Signals> querySignals = new ArrayList<>();
		Set<Long> seenInSource = new HashSet<>();
		int position = 0;
		for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
			EtsySearchPage page = etsyClient.searchActive(
					null,
					taxonomyId,
					properties.limit(),
					pageIndex * properties.limit(),
					mode.sortOn(),
					mode.sortOrder());
			etsyCount = Math.max(etsyCount, page.count());
			if (page.results().isEmpty()) {
				break;
			}
			for (EtsyListing listing : page.results()) {
				position++;
				boolean firstSighting = seenListingIds.add(listing.listingId());
				PersistResult result = upsertIfPrintTee(
						listing, crawlRunId, observedAt, position, source, firstSighting, true);
				if (result != null && result.accepted()) {
					matchedPrintTees++;
					if (result.newToIndex()) {
						newListings++;
					}
					if (seenInSource.add(listing.listingId())) {
						querySignals.add(signalsOf(listing));
					}
				} else if (firstSighting) {
					rejectedNonPrintTees++;
				}
			}
		}
		upsertQueryStats(source, observedAt, etsyCount, querySignals);
		return new IngestRunCounts(matchedPrintTees, rejectedNonPrintTees, newListings);
	}

	private IngestRunCounts runBenchmarkQueries(UUID crawlRunId, Instant observedAt, Set<Long> seenListingIds) {
		int matchedPrintTees = 0;
		int rejectedNonPrintTees = 0;
		int newListings = 0;
		int pages = properties.effectivePagesPerSweep();
		for (IngestProperties.Query query : properties.benchmarkQueries()) {
			int etsyCount = 0;
			List<QueryStatsCalculator.Signals> querySignals = new ArrayList<>();
			Set<Long> seenInQuery = new HashSet<>();
			int position = 0;
			String source = DiscoveryMode.benchmarkSource(query.keywords());
			for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
				EtsySearchPage page = etsyClient.searchActive(
						query.keywords(),
						query.taxonomyId() != null ? query.taxonomyId() : properties.taxonomyId(),
						properties.limit(),
						pageIndex * properties.limit(),
						"score",
						"desc");
				etsyCount = Math.max(etsyCount, page.count());
				if (page.results().isEmpty()) {
					break;
				}
				for (EtsyListing listing : page.results()) {
					position++;
					boolean firstSighting = seenListingIds.add(listing.listingId());
					PersistResult result = upsertIfPrintTee(
							listing, crawlRunId, observedAt, position, source, firstSighting, true);
					if (result != null && result.accepted()) {
						matchedPrintTees++;
						if (result.newToIndex()) {
							newListings++;
						}
						if (seenInQuery.add(listing.listingId())) {
							querySignals.add(signalsOf(listing));
						}
					} else if (firstSighting) {
						rejectedNonPrintTees++;
					}
				}
			}
			if (properties.createdPage()) {
				EtsySearchPage created = etsyClient.searchActive(
						query.keywords(),
						query.taxonomyId() != null ? query.taxonomyId() : properties.taxonomyId(),
						properties.limit(),
						0,
						"created",
						"desc");
				if (created != null && created.results() != null) {
					etsyCount = Math.max(etsyCount, created.count());
					int createdPosition = 0;
					for (EtsyListing listing : created.results()) {
						createdPosition++;
						boolean firstSighting = seenListingIds.add(listing.listingId());
						PersistResult result = upsertIfPrintTee(
								listing,
								crawlRunId,
								observedAt,
								createdPosition,
								source,
								firstSighting,
								false);
						if (result != null && result.accepted()) {
							matchedPrintTees++;
							if (result.newToIndex()) {
								newListings++;
							}
						} else if (firstSighting) {
							rejectedNonPrintTees++;
						}
					}
				}
			}
			upsertQueryStats(source, observedAt, etsyCount, querySignals);
		}
		return new IngestRunCounts(matchedPrintTees, rejectedNonPrintTees, newListings);
	}

	private IngestRunCounts runShopExpansion(UUID crawlRunId, Instant observedAt, Set<Long> seenListingIds) {
		int matchedPrintTees = 0;
		int rejectedNonPrintTees = 0;
		int newListings = 0;
		List<ShopCrawlQueue> pending =
				shopCrawlQueueRepository.findPendingOrderByEnqueuedAtAsc().stream()
						.limit(Math.max(properties.maxShopsPerRun(), 1))
						.toList();
		if (pending.isEmpty()) {
			log.info("shop expansion: queue empty");
			return new IngestRunCounts(0, 0, 0);
		}
		int maxPages = Math.min(Math.max(properties.maxPagesPerShop(), 1), 4);
		for (ShopCrawlQueue entry : pending) {
			long shopId = entry.getShopId();
			String source = DiscoveryMode.shopSource(shopId);
			int position = 0;
			for (int pageIndex = 0; pageIndex < maxPages; pageIndex++) {
				EtsySearchPage page = etsyClient.searchActiveByShop(
						shopId, properties.limit(), pageIndex * properties.limit(), "created", "desc");
				if (page.results().isEmpty()) {
					break;
				}
				for (EtsyListing listing : page.results()) {
					position++;
					boolean firstSighting = seenListingIds.add(listing.listingId());
					PersistResult result = upsertIfPrintTee(
							listing, crawlRunId, observedAt, position, source, firstSighting, false);
					if (result != null && result.accepted()) {
						matchedPrintTees++;
						if (result.newToIndex()) {
							newListings++;
						}
					} else if (firstSighting) {
						rejectedNonPrintTees++;
					}
				}
			}
			entry.setLastCrawledAt(observedAt);
			entry.setStatus("done");
			shopCrawlQueueRepository.save(entry);
		}
		return new IngestRunCounts(matchedPrintTees, rejectedNonPrintTees, newListings);
	}

	private int refreshStaleListings(UUID crawlRunId, Instant observedAt, Set<Long> seenListingIds) {
		int limit = properties.staleRefreshLimit();
		if (limit <= 0) {
			return 0;
		}
		List<Listing> stale = listingRepository.findByPrintTeeTrueOrderByLastSeenAtAsc(PageRequest.of(0, limit));
		int refreshed = 0;
		for (Listing listing : stale) {
			if (seenListingIds.contains(listing.getListingId())) {
				continue;
			}
			try {
				EtsyListing etsyListing = etsyClient.getListing(listing.getListingId());
				PersistResult result = upsertIfPrintTee(
						etsyListing, crawlRunId, observedAt, 0, "refresh:stale", false, false);
				if (result != null && result.accepted()) {
					refreshed++;
					seenListingIds.add(listing.getListingId());
				}
			} catch (RuntimeException ex) {
				log.warn("stale refresh skipped listing_id={}: {}", listing.getListingId(), ex.toString());
			}
		}
		if (refreshed > 0) {
			log.info("stale refresh updated {} listings", refreshed);
		}
		return refreshed;
	}

	private PersistResult upsertIfPrintTee(
			EtsyListing etsyListing,
			UUID crawlRunId,
			Instant observedAt,
			int position,
			String source,
			boolean firstSighting,
			boolean rankable) {
		return transactionTemplate.execute(
				status -> persistPrintTee(etsyListing, crawlRunId, observedAt, position, source, rankable));
	}

	private PersistResult persistPrintTee(
			EtsyListing etsyListing,
			UUID crawlRunId,
			Instant observedAt,
			int position,
			String source,
			boolean rankable) {
		if (etsyListing.listingId() == 0 || etsyListing.shopId() == null) {
			return new PersistResult(false, false);
		}
		String title = etsyListing.title();
		if (title == null || title.isBlank()) {
			return new PersistResult(false, false);
		}

		PrintTeeClassification classification = classifier.classify(
				etsyListing.title(), etsyListing.description(), etsyListing.tags(), etsyListing.taxonomyId());
		if (!classification.printTee()) {
			log.debug(
					"skip listing_id={} reasons={}", etsyListing.listingId(), classification.rejectReasons());
			return new PersistResult(false, false);
		}

		Instant now = observedAt;
		Shop shop = resolveShop(etsyListing, now);
		enqueueShopIfNew(etsyListing.shopId(), now);
		Listing listing = listingRepository
				.findById(etsyListing.listingId())
				.orElseGet(() -> new Listing(etsyListing.listingId(), shop, truncateTitle(title), listingUrl(etsyListing)));
		boolean newToIndex = listing.getFirstSeenAt() == null;
		listing.setShop(shop);
		listing.setTitle(truncateTitle(title));
		listing.setDescription(etsyListing.description());
		listing.setUrl(listingUrl(etsyListing));
		listing.setTaxonomyId(etsyListing.taxonomyId());
		if (etsyListing.price() != null) {
			listing.setPriceAmount(etsyListing.price().toDecimal());
			listing.setCurrency(truncate(etsyListing.price().currencyCode(), 3));
		}
		listing.setTags(tagsJson(etsyListing.tags()));
		listing.setNumFavorers(etsyListing.numFavorers() == null ? 0 : etsyListing.numFavorers());
		listing.setViews(etsyListing.views());
		listing.setQuantity(etsyListing.quantity());
		listing.setWhoMade(truncate(etsyListing.whoMade(), 32));
		listing.setWhenMade(truncate(etsyListing.whenMade(), 32));
		listing.setOriginalCreatedAt(
				etsyListing.originalCreatedAt() != null ? etsyListing.originalCreatedAt() : etsyListing.createdAt());
		listing.setEtsyCreatedAt(etsyListing.createdAt() != null ? etsyListing.createdAt() : etsyListing.originalCreatedAt());
		listing.setEtsyUpdatedAt(etsyListing.updatedAt());
		listing.setPrintTeeScore(BigDecimal.valueOf(classification.score()).setScale(3, RoundingMode.HALF_UP));
		listing.setPrintTee(true);
		listing.setLastSeenAt(now);
		if (listing.getFirstSeenAt() == null) {
			listing.setFirstSeenAt(now);
		}
		if (rankable && listing.getFirstSeenInTopAt() == null && position <= properties.topN()) {
			listing.setFirstSeenInTopAt(now);
		}
		addImagesIfMissing(listing, imagesFor(etsyListing, listing));
		listingRepository.save(listing);
		maybeSnapshot(listing, crawlRunId, observedAt, position);
		applyWindowDeltas(listing, observedAt);
		applyMomentumScores(listing, observedAt, rankable, position);
		listingRepository.save(listing);
		if (source != null && !source.isBlank() && !source.startsWith("refresh:")) {
			upsertQueryHit(listing, source, position, crawlRunId, observedAt);
		}
		nicheTermService.assignListing(listing);
		return new PersistResult(true, newToIndex);
	}

	private void enqueueShopIfNew(long shopId, Instant now) {
		if (shopCrawlQueueRepository.existsById(shopId)) {
			return;
		}
		shopCrawlQueueRepository.save(new ShopCrawlQueue(shopId, now));
	}

	private void maybeSnapshot(Listing listing, UUID crawlRunId, Instant observedAt, int position) {
		List<ListingSnapshot> recent = listingSnapshotRepository.findTop2ByListingListingIdOrderByIdDesc(listing.getListingId());
		ListingSnapshot last = recent.isEmpty() ? null : recent.get(0);
		Integer views = listing.getViews();
		Integer quantity = listing.getQuantity();
		Integer reviewCount = listing.getReviews30d();
		if (last != null
				&& last.sameSignals(position, listing.getNumFavorers(), views, quantity, reviewCount)) {
			LocalDate lastDay = last.getObservedAt().atZone(ISTANBUL).toLocalDate();
			LocalDate runDay = observedAt.atZone(ISTANBUL).toLocalDate();
			if (lastDay.equals(runDay)) {
				return;
			}
		}
		ListingSnapshot snapshot = new ListingSnapshot(
				listing, crawlRunId.toString(), observedAt, position, listing.getNumFavorers());
		snapshot.setViews(views);
		snapshot.setQuantity(quantity);
		snapshot.setReviewCount(reviewCount);
		listingSnapshotRepository.saveAndFlush(snapshot);
	}

	private void upsertQueryHit(Listing listing, String source, int position, UUID crawlRunId, Instant observedAt) {
		if (source == null || source.isBlank()) {
			return;
		}
		LocalDate day = observedAt.atZone(ISTANBUL).toLocalDate();
		ListingQueryHitId id = new ListingQueryHitId(listing.getListingId(), source, day);
		ListingQueryHit hit = listingQueryHitRepository
				.findById(id)
				.orElseGet(() -> new ListingQueryHit(listing, source, day, position, crawlRunId.toString(), observedAt));
		hit.setPosition(position);
		hit.setCrawlRunId(crawlRunId.toString());
		hit.setObservedAt(observedAt);
		listingQueryHitRepository.save(hit);
	}

	private Shop resolveShop(EtsyListing etsyListing, Instant now) {
		Shop shop = shopRepository
				.findById(etsyListing.shopId())
				.orElseGet(() -> shopRepository.save(newShop(etsyListing.shopId())));
		EtsyShop etsyShop = etsyListing.shop();
		if (etsyShop == null) {
			return shop;
		}
		boolean stale = shop.getLastRefreshedAt() == null
				|| shop.getLastRefreshedAt().isBefore(now.minus(Duration.ofHours(24)));
		boolean placeholder = shop.getName() != null && shop.getName().startsWith("Etsy shop ");
		if (stale || placeholder) {
			applyShop(shop, etsyShop);
			shop.setLastRefreshedAt(now);
			shopRepository.save(shop);
		}
		return shop;
	}

	private static void applyShop(Shop shop, EtsyShop etsyShop) {
		if (etsyShop.name() != null && !etsyShop.name().isBlank()) {
			shop.setName(truncate(etsyShop.name(), 255));
		}
		if (etsyShop.url() != null && !etsyShop.url().isBlank()) {
			shop.setUrl(etsyShop.url());
		}
		shop.setIconUrl(etsyShop.iconUrl());
		shop.setTransactionSoldCount(etsyShop.transactionSoldCount());
		shop.setListingActiveCount(etsyShop.listingActiveCount());
		shop.setReviewCount(etsyShop.reviewCount());
		shop.setReviewAverage(etsyShop.reviewAverage());
		if (etsyShop.createdAt() != null) {
			shop.setEtsyCreatedAt(etsyShop.createdAt());
		}
	}

	private int favorersDelta(long listingId) {
		List<ListingSnapshot> recent = listingSnapshotRepository.findTop2ByListingListingIdOrderByIdDesc(listingId);
		if (recent.size() < 2) {
			return 0;
		}
		return recent.get(0).getNumFavorers() - recent.get(1).getNumFavorers();
	}

	private static Shop newShop(long shopId) {
		return new Shop(shopId, "Etsy shop " + shopId, "https://www.etsy.com/shop/" + shopId);
	}

	private static String listingUrl(EtsyListing listing) {
		if (listing.url() != null && !listing.url().isBlank()) {
			return listing.url();
		}
		return "https://www.etsy.com/listing/" + listing.listingId();
	}

	private static String truncateTitle(String title) {
		return truncate(title, TITLE_MAX);
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		return value.length() <= max ? value : value.substring(0, max);
	}

	private List<EtsyImage> imagesFor(EtsyListing etsyListing, Listing listing) {
		if (!listing.getImages().isEmpty()) {
			return List.of();
		}
		if (etsyListing.images() != null && !etsyListing.images().isEmpty()) {
			return etsyListing.images();
		}
		try {
			List<EtsyImage> fetched = etsyClient.getListing(etsyListing.listingId()).images();
			return fetched == null ? List.of() : fetched;
		} catch (RuntimeException ex) {
			log.warn("images fetch skipped listing_id={}: {}", etsyListing.listingId(), ex.toString());
			return List.of();
		}
	}

	private void addImagesIfMissing(Listing listing, List<EtsyImage> images) {
		if (listing.getImages().isEmpty() && images != null) {
			for (EtsyImage image : images) {
				listing.addImage(new ListingImage(image.url(), image.rank()));
			}
		}
		listing.getImages().stream()
				.min(Comparator.comparingInt(ListingImage::getRank))
				.filter(image -> image.getStorageKey() == null)
				.ifPresent(image -> listingImageCache
						.cache(listing.getListingId(), 1, image.getUrl())
						.ifPresent(image::setStorageKey));
	}

	private String tagsJson(List<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return null;
		}
		return objectMapper.writeValueAsString(tags);
	}

	private void applyWindowDeltas(Listing listing, Instant observedAt) {
		List<ListingSnapshot> history =
				listingSnapshotRepository.findByListingListingIdOrderByObservedAtAscIdAsc(listing.getListingId());
		SnapshotDeltas.Delta delta = snapshotDeltas.delta7d(
				history.stream()
						.map(row -> new SnapshotDeltas.Point(row.getObservedAt(), row.getNumFavorers(), row.getViews()))
						.toList(),
				observedAt);
		listing.setDeltaFavorers7d(delta.favorers());
		listing.setDeltaViews7d(delta.views());
	}

	private void applyMomentumScores(Listing listing, Instant observedAt, boolean rankable, int position) {
		if (rankable && position > 0) {
			int favorersDelta = favorersDelta(listing.getListingId());
			double daily = listingRanker.scoreDaily(
					listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt(), observedAt, favorersDelta);
			listing.setLastScore(BigDecimal.valueOf(daily).setScale(9, RoundingMode.HALF_UP));
		}
		List<ListingSnapshot> history =
				listingSnapshotRepository.findByListingListingIdOrderByObservedAtAscIdAsc(listing.getListingId());
		List<SnapshotTrendSignals.TrendPoint> trendPoints = history.stream()
				.map(row -> new SnapshotTrendSignals.TrendPoint(
						row.getObservedAt(), row.getNumFavorers(), row.getViews(), row.getPosition()))
				.toList();
		double weekly = listingRanker.scoreTrend(
				snapshotTrendSignals.trendInput(trendPoints, observedAt, Duration.ofDays(7)));
		double monthly = listingRanker.scoreTrend(
				snapshotTrendSignals.trendInput(trendPoints, observedAt, Duration.ofDays(30)));
		listing.setLastScoreWeekly(BigDecimal.valueOf(weekly).setScale(9, RoundingMode.HALF_UP));
		listing.setLastScoreMonthly(BigDecimal.valueOf(monthly).setScale(9, RoundingMode.HALF_UP));
		listing.setLastScoredAt(observedAt);
	}

	private void upsertQueryStats(
			String source, Instant observedAt, int etsyCount, List<QueryStatsCalculator.Signals> signals) {
		if (source == null || source.isBlank()) {
			return;
		}
		LocalDate day = observedAt.atZone(ISTANBUL).toLocalDate();
		QueryStatsCalculator.Row row = queryStatsCalculator.summarize(etsyCount, signals);
		QueryStats stats = queryStatsRepository
				.findById(new QueryStatsId(source, day))
				.orElseGet(() -> new QueryStats(source, day));
		stats.apply(row);
		queryStatsRepository.save(stats);
	}

	private void refreshReviewsIfNeeded(Instant observedAt) {
		if (properties.reviewLimit() <= 0) {
			return;
		}
		boolean scheduledRun = DiscoveryMode.isReviewRun(observedAt);
		boolean bootstrap = listingRepository.countWithReviews30d() == 0;
		if (!scheduledRun && !bootstrap) {
			return;
		}
		List<Listing> top = listingRepository.findByPrintTeeTrueOrderByLastScoreWeeklyDesc(
				org.springframework.data.domain.PageRequest.of(0, Math.max(properties.reviewLimit(), 1)));
		for (Listing listing : top) {
			try {
				List<Instant> created = etsyClient.getListingReviews(listing.getListingId(), 100);
				ReviewWindow.Summary summary = reviewWindow.summarize(created, observedAt);
				listing.setReviews30d(summary.reviews30d());
				listing.setLastReviewAt(summary.lastReviewAt());
				listingRepository.save(listing);
			} catch (RuntimeException ex) {
				log.warn("reviews skipped listing_id={}: {}", listing.getListingId(), ex.toString());
			}
		}
	}

	private static QueryStatsCalculator.Signals signalsOf(EtsyListing listing) {
		return new QueryStatsCalculator.Signals(
				listing.price() == null ? null : listing.price().toDecimal(),
				listing.numFavorers() == null ? 0 : listing.numFavorers(),
				listing.views());
	}
}
