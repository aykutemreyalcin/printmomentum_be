package com.printmomentum.ingest;

import com.printmomentum.config.IngestProperties;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.ListingSnapshot;
import com.printmomentum.domain.ListingSnapshotRepository;
import com.printmomentum.domain.PrintTeeClassification;
import com.printmomentum.domain.PrintTeeClassifier;
import com.printmomentum.domain.Shop;
import com.printmomentum.domain.ShopRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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

	private final EtsyClient etsyClient;
	private final PrintTeeClassifier classifier;
	private final IngestProperties properties;
	private final ShopRepository shopRepository;
	private final ListingRepository listingRepository;
	private final ListingSnapshotRepository listingSnapshotRepository;
	private final TransactionTemplate transactionTemplate;
	private final ObjectMapper objectMapper;

	public ListingIngestJob(
			EtsyClient etsyClient,
			PrintTeeClassifier classifier,
			IngestProperties properties,
			ShopRepository shopRepository,
			ListingRepository listingRepository,
			ListingSnapshotRepository listingSnapshotRepository,
			PlatformTransactionManager transactionManager,
			ObjectMapper objectMapper) {
		this.etsyClient = etsyClient;
		this.classifier = classifier;
		this.properties = properties;
		this.shopRepository = shopRepository;
		this.listingRepository = listingRepository;
		this.listingSnapshotRepository = listingSnapshotRepository;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.objectMapper = objectMapper;
	}

	@Scheduled(cron = "${printmomentum.ingest.cron:-}")
	public void ingestOnSchedule() {
		run();
	}

	public IngestResult run() {
		UUID crawlRunId = UUID.randomUUID();
		Instant observedAt = Instant.now();
		int stored = 0;
		int skipped = 0;
		Set<Long> seenListingIds = new HashSet<>();
		log.info("ingest start crawl_run_id={}", crawlRunId);

		for (IngestProperties.Query query : properties.queries()) {
			EtsySearchPage page = etsyClient.searchActive(
					query.keywords(), query.taxonomyId(), properties.limit(), 0);
			int position = 0;
			for (EtsyListing listing : page.results()) {
				position++;
				if (!seenListingIds.add(listing.listingId())) {
					continue;
				}
				if (upsertIfPrintTee(listing, crawlRunId, observedAt, position)) {
					stored++;
				}
				else {
					skipped++;
				}
			}
		}

		log.info("ingest done crawl_run_id={} stored={} skipped={}", crawlRunId, stored, skipped);
		return new IngestResult(crawlRunId, stored, skipped);
	}

	private boolean upsertIfPrintTee(EtsyListing etsyListing, UUID crawlRunId, Instant observedAt, int position) {
		Boolean stored = transactionTemplate.execute(
				status -> persistPrintTee(etsyListing, crawlRunId, observedAt, position));
		return Boolean.TRUE.equals(stored);
	}

	private boolean persistPrintTee(EtsyListing etsyListing, UUID crawlRunId, Instant observedAt, int position) {
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
		Shop shop = shopRepository
				.findById(etsyListing.shopId())
				.orElseGet(() -> shopRepository.save(newShop(etsyListing.shopId())));
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
		listing.setEtsyCreatedAt(etsyListing.createdAt() != null ? etsyListing.createdAt() : etsyListing.originalCreatedAt());
		listing.setEtsyUpdatedAt(etsyListing.updatedAt());
		listing.setPrintTeeScore(BigDecimal.valueOf(classification.score()).setScale(3, RoundingMode.HALF_UP));
		listing.setPrintTee(true);
		listing.setLastSeenAt(now);
		if (listing.getFirstSeenAt() == null) {
			listing.setFirstSeenAt(now);
		}
		if (listing.getFirstSeenInTopAt() == null && position <= properties.topN()) {
			listing.setFirstSeenInTopAt(now);
		}
		addImagesIfMissing(listing, etsyListing.images());
		listingRepository.save(listing);
		listingSnapshotRepository.save(new ListingSnapshot(
				listing,
				crawlRunId.toString(),
				observedAt,
				position,
				listing.getNumFavorers()));
		return true;
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

	private void addImagesIfMissing(Listing listing, List<EtsyImage> images) {
		if (!listing.getImages().isEmpty() || images == null || images.isEmpty()) {
			return;
		}
		for (EtsyImage image : images) {
			listing.addImage(new ListingImage(image.url(), image.rank()));
		}
	}

	private String tagsJson(List<String> tags) {
		if (tags == null || tags.isEmpty()) {
			return null;
		}
		return objectMapper.writeValueAsString(tags);
	}
}
