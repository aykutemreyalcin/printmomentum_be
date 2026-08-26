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
import com.printmomentum.domain.ShopRepository;
import com.printmomentum.domain.SnapshotDeltas;
import com.printmomentum.storage.ListingImageCache;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

	private final EtsyClient etsyClient;
	private final PrintTeeClassifier classifier;
	private final IngestProperties properties;
	private final ShopRepository shopRepository;
	private final ListingRepository listingRepository;
	private final ListingSnapshotRepository listingSnapshotRepository;
	private final ListingQueryHitRepository listingQueryHitRepository;
	private final ListingRanker listingRanker;
	private final SnapshotDeltas snapshotDeltas;
	private final QueryStatsCalculator queryStatsCalculator;
	private final QueryStatsRepository queryStatsRepository;
	private final ReviewWindow reviewWindow;
	private final ListingImageCache listingImageCache;
	private final EtsyQuotaTracker quotaTracker;
	private final BestsellerMarker bestsellerMarker;
	private final TransactionTemplate transactionTemplate;
	private final ObjectMapper objectMapper;

	public ListingIngestJob(
			EtsyClient etsyClient,
			PrintTeeClassifier classifier,
			IngestProperties properties,
			ShopRepository shopRepository,
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
			PlatformTransactionManager transactionManager,
			ObjectMapper objectMapper) {
		this.etsyClient = etsyClient;
		this.classifier = classifier;
		this.properties = properties;
		this.shopRepository = shopRepository;
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
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.objectMapper = objectMapper;
	}

	@Scheduled(cron = "${printmomentum.ingest.cron:-}", zone = "Europe/Istanbul")
	public void ingestOnSchedule() {
		run();
	}

	public IngestResult run() {
		Integer remaining = quotaTracker.remainingToday();
		if (remaining != null && remaining < properties.minRemainingToday()) {
			log.warn(
					"ingest skip remaining-today={} threshold={}",
					remaining,
					properties.minRemainingToday());
			return new IngestResult(null, 0, 0);
		}

		UUID crawlRunId = UUID.randomUUID();
		Instant observedAt = Instant.now();
		int stored = 0;
		int skipped = 0;
		Set<Long> seenListingIds = new HashSet<>();
		log.info("ingest start crawl_run_id={}", crawlRunId);

		int pages = Math.min(Math.max(properties.pagesPerQuery(), 1), 4);
		for (IngestProperties.Query query : properties.queries()) {
			int etsyCount = 0;
			List<QueryStatsCalculator.Signals> querySignals = new ArrayList<>();
			Set<Long> seenInQuery = new HashSet<>();
			int position = 0;
			for (int pageIndex = 0; pageIndex < pages; pageIndex++) {
				EtsySearchPage page = etsyClient.searchActive(
						query.keywords(),
						query.taxonomyId(),
						properties.limit(),
						pageIndex * properties.limit());
				etsyCount = Math.max(etsyCount, page.count());
				if (page.results().isEmpty()) {
					break;
				}
				for (EtsyListing listing : page.results()) {
					position++;
					boolean firstSighting = seenListingIds.add(listing.listingId());
					if (upsertIfPrintTee(
							listing, crawlRunId, observedAt, position, query.keywords(), firstSighting, true)) {
						stored += firstSighting ? 1 : 0;
						if (seenInQuery.add(listing.listingId())) {
							querySignals.add(signalsOf(listing));
						}
					} else if (firstSighting) {
						skipped++;
					}
				}
			}
			if (properties.createdPage()) {
				EtsySearchPage created = etsyClient.searchActive(
						query.keywords(), query.taxonomyId(), properties.limit(), 0, "created", "desc");
				if (created != null && created.results() != null) {
					etsyCount = Math.max(etsyCount, created.count());
					int createdPosition = 0;
					for (EtsyListing listing : created.results()) {
						createdPosition++;
						boolean firstSighting = seenListingIds.add(listing.listingId());
						if (upsertIfPrintTee(
								listing,
								crawlRunId,
								observedAt,
								createdPosition,
								query.keywords(),
								firstSighting,
								false)) {
							stored += firstSighting ? 1 : 0;
						} else if (firstSighting) {
							skipped++;
						}
					}
				}
			}
			upsertQueryStats(query.keywords(), observedAt, etsyCount, querySignals);
		}

		refreshReviewsIfNoon(observedAt);
		bestsellerMarker.refresh(observedAt);
		log.info("ingest done crawl_run_id={} stored={} skipped={}", crawlRunId, stored, skipped);
		return new IngestResult(crawlRunId, stored, skipped);
	}

	private boolean upsertIfPrintTee(
			EtsyListing etsyListing,
			UUID crawlRunId,
			Instant observedAt,
			int position,
			String query,
			boolean firstSighting,
			boolean rankable) {
		Boolean stored = transactionTemplate.execute(status -> persistPrintTee(
				etsyListing, crawlRunId, observedAt, position, query, firstSighting, rankable));
		return Boolean.TRUE.equals(stored);
	}

	private boolean persistPrintTee(
			EtsyListing etsyListing,
			UUID crawlRunId,
			Instant observedAt,
			int position,
			String query,
			boolean firstSighting,
			boolean rankable) {
		if (etsyListing.listingId() == 0 || etsyListing.shopId() == null) {
			return false;
		}
		String title = etsyListing.title();
		if (title == null || title.isBlank()) {
			return false;
		}

		PrintTeeClassification classification = classifier.classify(
				etsyListing.title(), etsyListing.description(), etsyListing.tags(), etsyListing.taxonomyId());
		if (!classification.printTee()) {
			log.debug(
					"skip listing_id={} reasons={}", etsyListing.listingId(), classification.rejectReasons());
			return false;
		}

		Instant now = observedAt;
		Shop shop = resolveShop(etsyListing, now);
		Listing listing = listingRepository
				.findById(etsyListing.listingId())
				.orElseGet(() -> new Listing(etsyListing.listingId(), shop, truncateTitle(title), listingUrl(etsyListing)));
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
		if (firstSighting && rankable) {
			maybeSnapshot(listing, crawlRunId, observedAt, position);
			int favorersDelta = favorersDelta(listing.getListingId());
			double momentum = listingRanker.score(
					listing.getEtsyCreatedAt(), listing.getFirstSeenInTopAt(), observedAt, favorersDelta);
			listing.setLastScore(BigDecimal.valueOf(momentum).setScale(9, RoundingMode.HALF_UP));
			listing.setLastScoredAt(observedAt);
			applyWindowDeltas(listing, observedAt);
			listingRepository.save(listing);
		}
		upsertQueryHit(listing, query, position, crawlRunId, observedAt);
		return true;
	}

	private void maybeSnapshot(Listing listing, UUID crawlRunId, Instant observedAt, int position) {
		List<ListingSnapshot> recent = listingSnapshotRepository.findTop2ByListingListingIdOrderByIdDesc(listing.getListingId());
		ListingSnapshot last = recent.isEmpty() ? null : recent.get(0);
		Integer views = listing.getViews();
		Integer quantity = listing.getQuantity();
		Integer reviewCount = listing.getReviews30d();
		if (last != null
				&& last.sameSignals(position, listing.getNumFavorers(), views, quantity, reviewCount)) {
			return;
		}
		ListingSnapshot snapshot = new ListingSnapshot(
				listing, crawlRunId.toString(), observedAt, position, listing.getNumFavorers());
		snapshot.setViews(views);
		snapshot.setQuantity(quantity);
		snapshot.setReviewCount(reviewCount);
		listingSnapshotRepository.saveAndFlush(snapshot);
	}

	private void upsertQueryHit(Listing listing, String query, int position, UUID crawlRunId, Instant observedAt) {
		if (query == null || query.isBlank()) {
			return;
		}
		LocalDate day = observedAt.atZone(ISTANBUL).toLocalDate();
		ListingQueryHitId id = new ListingQueryHitId(listing.getListingId(), query, day);
		ListingQueryHit hit = listingQueryHitRepository
				.findById(id)
				.orElseGet(() -> new ListingQueryHit(listing, query, day, position, crawlRunId.toString(), observedAt));
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

	private void upsertQueryStats(
			String query, Instant observedAt, int etsyCount, List<QueryStatsCalculator.Signals> signals) {
		if (query == null || query.isBlank()) {
			return;
		}
		LocalDate day = observedAt.atZone(ISTANBUL).toLocalDate();
		QueryStatsCalculator.Row row = queryStatsCalculator.summarize(etsyCount, signals);
		QueryStats stats = queryStatsRepository
				.findById(new QueryStatsId(query, day))
				.orElseGet(() -> new QueryStats(query, day));
		stats.apply(row);
		queryStatsRepository.save(stats);
	}

	private void refreshReviewsIfNoon(Instant observedAt) {
		if (observedAt.atZone(ISTANBUL).getHour() != 12) {
			return;
		}
		List<Listing> top = listingRepository.findByPrintTeeTrueOrderByLastScoreDesc(
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
