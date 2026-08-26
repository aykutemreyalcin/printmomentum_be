package com.printmomentum.ingest;

import com.printmomentum.config.BestsellerProperties;
import com.printmomentum.config.IngestProperties;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingEvent;
import com.printmomentum.domain.ListingEventRepository;
import com.printmomentum.domain.ListingRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BestsellerMarker {

	private static final Logger log = LoggerFactory.getLogger(BestsellerMarker.class);

	private final BestsellerProperties properties;
	private final IngestProperties ingestProperties;
	private final EtsyBestsellerSearch bestsellerSearch;
	private final ListingRepository listingRepository;
	private final ListingEventRepository listingEventRepository;

	public BestsellerMarker(
			BestsellerProperties properties,
			IngestProperties ingestProperties,
			EtsyBestsellerSearch bestsellerSearch,
			ListingRepository listingRepository,
			ListingEventRepository listingEventRepository) {
		this.properties = properties;
		this.ingestProperties = ingestProperties;
		this.bestsellerSearch = bestsellerSearch;
		this.listingRepository = listingRepository;
		this.listingEventRepository = listingEventRepository;
	}

	@Transactional
	public void refresh(Instant observedAt) {
		if (properties.siteSearchEnabled()) {
			refreshFromSiteSearch(observedAt);
			clearPmFlags(observedAt);
			return;
		}
		refreshPmFallback(observedAt);
	}

	private void refreshFromSiteSearch(Instant observedAt) {
		Set<Long> found = new HashSet<>();
		for (IngestProperties.Query query : ingestProperties.queries()) {
			Optional<Set<Long>> pageIds = bestsellerSearch.listingIds(query.keywords());
			if (pageIds.isEmpty()) {
				log.warn("bestseller site search failed; leaving etsy_bestseller unchanged");
				return;
			}
			found.addAll(pageIds.get());
		}
		if (found.isEmpty()) {
			log.warn("bestseller site search parsed no listing ids; leaving etsy_bestseller unchanged");
			return;
		}
		applyEtsySet(found, observedAt);
	}

	private void applyEtsySet(Set<Long> found, Instant observedAt) {
		List<Listing> current = listingRepository.findByPrintTeeTrueAndEtsyBestsellerTrue();
		for (Listing listing : current) {
			if (!found.contains(listing.getListingId())) {
				listing.setEtsyBestseller(false);
				listing.setEtsyBestsellerEndedAt(observedAt);
				listingEventRepository.save(new ListingEvent(listing, ListingEvent.ETSY_BESTSELLER_OFF, observedAt));
			}
		}
		for (Listing listing : listingRepository.findAllById(found)) {
			if (!listing.isPrintTee()) {
				continue;
			}
			if (!listing.isEtsyBestseller()) {
				listing.setEtsyBestseller(true);
				if (listing.getEtsyBestsellerSince() == null) {
					listing.setEtsyBestsellerSince(observedAt);
				}
				listing.setEtsyBestsellerEndedAt(null);
				listingEventRepository.save(new ListingEvent(listing, ListingEvent.ETSY_BESTSELLER_ON, observedAt));
			}
		}
	}

	private void refreshPmFallback(Instant observedAt) {
		List<Listing> printTees = listingRepository.findByPrintTeeTrue();
		for (Listing listing : printTees) {
			boolean likely = listing.getReviews30d() != null && listing.getReviews30d() >= 8;
			if (likely && !listing.isPmBestseller()) {
				listing.setPmBestseller(true);
				if (listing.getPmBestsellerSince() == null) {
					listing.setPmBestsellerSince(observedAt);
				}
				listing.setPmBestsellerEndedAt(null);
				listingEventRepository.save(new ListingEvent(listing, ListingEvent.PM_BESTSELLER_ON, observedAt));
			} else if (!likely && listing.isPmBestseller()) {
				listing.setPmBestseller(false);
				listing.setPmBestsellerEndedAt(observedAt);
				listingEventRepository.save(new ListingEvent(listing, ListingEvent.PM_BESTSELLER_OFF, observedAt));
			}
		}
	}

	private void clearPmFlags(Instant observedAt) {
		for (Listing listing : listingRepository.findByPrintTeeTrueAndPmBestsellerTrue()) {
			listing.setPmBestseller(false);
			listing.setPmBestsellerEndedAt(observedAt);
			listingEventRepository.save(new ListingEvent(listing, ListingEvent.PM_BESTSELLER_OFF, observedAt));
		}
	}
}
