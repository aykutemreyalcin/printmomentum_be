package com.printmomentum.ingest;

import com.printmomentum.config.BestsellerProperties;
import com.printmomentum.config.IngestProperties;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingEstimator;
import com.printmomentum.domain.ListingEvent;
import com.printmomentum.domain.ListingEventRepository;
import com.printmomentum.domain.ListingRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
	private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");
	private static final double PM_MIN_EST_SALES_30D = 25.0;
	private static final int ROTATION_SLOTS = 6;

	private final BestsellerProperties properties;
	private final IngestProperties ingestProperties;
	private final ListingEstimator listingEstimator;
	private final EtsyPublicListingClient publicListingClient;
	private final ListingRepository listingRepository;
	private final ListingEventRepository listingEventRepository;

	public BestsellerMarker(
			BestsellerProperties properties,
			IngestProperties ingestProperties,
			ListingEstimator listingEstimator,
			EtsyPublicListingClient publicListingClient,
			ListingRepository listingRepository,
		 ListingEventRepository listingEventRepository) {
		this.properties = properties;
		this.ingestProperties = ingestProperties;
		this.listingEstimator = listingEstimator;
		this.publicListingClient = publicListingClient;
		this.listingRepository = listingRepository;
		this.listingEventRepository = listingEventRepository;
	}

	@Transactional
	public void refresh(Instant observedAt) {
		refresh(observedAt, Set.of());
	}

	@Transactional
	public void refresh(Instant observedAt, Set<Long> touchedListingIds) {
		if (properties.siteSearchEnabled()) {
			VerificationRun run = verifyViaPublicApi(touchedListingIds, observedAt);
			if (run.reliable()) {
				clearPmFlags(observedAt);
				log.info(
						"bestseller etsy verified checked={} confirmed={} cleared={}",
						run.checked(),
						run.confirmed(),
						run.cleared());
				return;
			}
			log.warn("bestseller etsy verification unreliable (checked={}); using PM fallback", run.checked());
		}
		refreshPmFallback(observedAt);
	}

	private VerificationRun verifyViaPublicApi(Set<Long> touchedListingIds, Instant observedAt) {
		Set<Long> targets = verificationTargets(touchedListingIds, observedAt);
		int confirmed = 0;
		int cleared = 0;
		int checked = 0;
		int failures = 0;
		for (Long listingId : targets) {
			Optional<Boolean> bestseller = publicListingClient.isBestseller(listingId);
			if (bestseller.isEmpty()) {
				failures++;
				continue;
			}
			checked++;
			Listing listing = listingRepository.findById(listingId).orElse(null);
			if (listing == null || !listing.isPrintTee()) {
				continue;
			}
			if (bestseller.get()) {
				if (!listing.isEtsyBestseller()) {
					listing.setEtsyBestseller(true);
					if (listing.getEtsyBestsellerSince() == null) {
						listing.setEtsyBestsellerSince(observedAt);
					}
					listing.setEtsyBestsellerEndedAt(null);
					listingEventRepository.save(new ListingEvent(listing, ListingEvent.ETSY_BESTSELLER_ON, observedAt));
				}
				confirmed++;
			} else if (listing.isEtsyBestseller()) {
				listing.setEtsyBestseller(false);
				listing.setEtsyBestsellerEndedAt(observedAt);
				listingEventRepository.save(new ListingEvent(listing, ListingEvent.ETSY_BESTSELLER_OFF, observedAt));
				cleared++;
			}
		}
		boolean reliable = checked > 0 && failures < Math.max(1, checked / 2);
		return new VerificationRun(checked, confirmed, cleared, reliable);
	}

	private Set<Long> verificationTargets(Set<Long> touchedListingIds, Instant observedAt) {
		LinkedHashSet<Long> ordered = new LinkedHashSet<>();
		for (Listing listing : listingRepository.findByPrintTeeTrueAndEtsyBestsellerTrue()) {
			ordered.add(listing.getListingId());
		}
		if (touchedListingIds != null) {
			for (Long listingId : touchedListingIds) {
				ordered.add(listingId);
			}
		}
		int slot = (observedAt.atZone(ISTANBUL).getHour() / 4) % ROTATION_SLOTS;
		for (Listing listing : listingRepository.findByPrintTeeTrue()) {
			if (listing.getListingId() % ROTATION_SLOTS == slot) {
				ordered.add(listing.getListingId());
			}
		}
		int cap = Math.max(properties.maxChecksPerRun(), 1);
		if (ordered.size() <= cap) {
			return Set.copyOf(ordered);
		}
		return Set.copyOf(new ArrayList<>(ordered).subList(0, cap));
	}

	private void refreshPmFallback(Instant observedAt) {
		List<Listing> printTees = listingRepository.findByPrintTeeTrue();
		for (Listing listing : printTees) {
			boolean likely = isPmLikely(listing);
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

	private boolean isPmLikely(Listing listing) {
		Integer reviews30d = listing.getReviews30d();
		if (reviews30d != null && reviews30d >= 8) {
			return true;
		}
		Double estSales = listingEstimator.estSales(reviews30d);
		if (estSales != null && estSales >= PM_MIN_EST_SALES_30D) {
			return true;
		}
		Integer deltaFavorers = listing.getDeltaFavorers7d();
		return deltaFavorers != null && deltaFavorers >= ingestProperties.pmBestsellerMinFavorersDelta7d();
	}

	private void clearPmFlags(Instant observedAt) {
		for (Listing listing : listingRepository.findByPrintTeeTrueAndPmBestsellerTrue()) {
			listing.setPmBestseller(false);
			listing.setPmBestsellerEndedAt(observedAt);
			listingEventRepository.save(new ListingEvent(listing, ListingEvent.PM_BESTSELLER_OFF, observedAt));
		}
	}

	private record VerificationRun(int checked, int confirmed, int cleared, boolean reliable) {
	}
}
