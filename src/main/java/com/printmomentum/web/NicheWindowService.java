package com.printmomentum.web;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingImage;
import com.printmomentum.domain.ListingNicheTerm;
import com.printmomentum.domain.ListingNicheTermRepository;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.NicheTerm;
import com.printmomentum.domain.NicheTermRepository;
import com.printmomentum.domain.NicheWindowSnapshot;
import com.printmomentum.domain.NicheWindowSnapshotRepository;
import com.printmomentum.niche.NicheWindowState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NicheWindowService {

	private final NicheTermRepository nicheTermRepository;
	private final ListingNicheTermRepository listingNicheTermRepository;
	private final ListingRepository listingRepository;
	private final NicheWindowSnapshotRepository nicheWindowSnapshotRepository;

	public NicheWindowService(
			NicheTermRepository nicheTermRepository,
			ListingNicheTermRepository listingNicheTermRepository,
			ListingRepository listingRepository,
			NicheWindowSnapshotRepository nicheWindowSnapshotRepository) {
		this.nicheTermRepository = nicheTermRepository;
		this.listingNicheTermRepository = listingNicheTermRepository;
		this.listingRepository = listingRepository;
		this.nicheWindowSnapshotRepository = nicheWindowSnapshotRepository;
	}

	@Transactional(readOnly = true)
	public NichePageResponse list(String window, String sort, int page, int size) {
		PageRequest pageable = PageRequest.of(page, size, sortOrder(sort));
		Page<NicheTerm> terms = window == null || window.isBlank()
				? nicheTermRepository.findAll(pageable)
				: nicheTermRepository.findByWindowState(NicheWindowState.parse(window).name(), pageable);
		List<NicheTermItem> items = terms.stream().map(this::toItem).toList();
		return new NichePageResponse(items, page, size, (int) terms.getTotalElements());
	}

	@Transactional(readOnly = true)
	public NicheDetailResponse detail(String slug) {
		NicheTerm term = nicheTermRepository.findBySlug(slug).orElse(null);
		if (term == null) {
			return null;
		}
		List<NicheTopListingItem> topListings = topListings(term.getId(), 20);
		List<NicheSnapshotItem> history = nicheWindowSnapshotRepository
				.findByNicheTermIdOrderByObservedDayDesc(term.getId(), PageRequest.of(0, 30))
				.stream()
				.map(snapshot -> new NicheSnapshotItem(
						snapshot.getObservedDay(),
						snapshot.getWindowState(),
						snapshot.getListingCount(),
						snapshot.getNewEntrants14d(),
						snapshot.getCloneDensity7d(),
						snapshot.getBreakInRate()))
				.toList();
		List<NicheTermItem> related = relatedTerms(term.getLabel());
		return new NicheDetailResponse(
				term.getSlug(),
				term.getLabel(),
				term.getWindowState(),
				term.getListingCount(),
				term.getNewEntrants14d(),
				term.getCloneDensity7d(),
				term.getBreakInRate(),
				term.getIncumbentAgeDays(),
				term.getEntrantMomentum(),
				term.getEtsyCount(),
				term.getWindowComputedAt(),
				history,
				topListings,
				related);
	}

	@Transactional(readOnly = true)
	public NicheStatsResponse stats() {
		Map<String, Long> counts = nicheTermRepository.countByWindowState().stream()
				.collect(Collectors.toMap(row -> (String) row[0], row -> (Long) row[1]));
		int open = counts.getOrDefault(NicheWindowState.OPEN.name(), 0L).intValue();
		int closing = counts.getOrDefault(NicheWindowState.CLOSING.name(), 0L).intValue();
		int closed = counts.getOrDefault(NicheWindowState.CLOSED.name(), 0L).intValue();
		int lowData = counts.getOrDefault(NicheWindowState.LOW_DATA.name(), 0L).intValue();
		int total = open + closing + closed + lowData;
		Instant computedAt = nicheTermRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "windowComputedAt")))
				.stream()
				.findFirst()
				.map(NicheTerm::getWindowComputedAt)
				.orElse(null);
		return new NicheStatsResponse(open, closing, closed, lowData, total, computedAt);
	}

	@Transactional(readOnly = true)
	public List<NicheTopListingItem> listings(String slug, int limit) {
		NicheTerm term = nicheTermRepository.findBySlug(slug).orElse(null);
		if (term == null) {
			return List.of();
		}
		return topListings(term.getId(), limit);
	}

	private List<NicheTopListingItem> topListings(long nicheTermId, int limit) {
		return listingNicheTermRepository.findByNicheTermId(nicheTermId).stream()
				.map(ListingNicheTerm::getListingId)
				.map(listingRepository::findById)
				.flatMap(java.util.Optional::stream)
				.sorted(Comparator.comparing(
						(Listing listing) -> listing.getLastScoreWeekly() == null
								? 0.0
								: listing.getLastScoreWeekly().doubleValue(),
						Comparator.reverseOrder()))
				.limit(limit)
				.map(this::toTopListing)
				.toList();
	}

	private List<NicheTermItem> relatedTerms(String label) {
		if (label == null || !label.contains(" ")) {
			return List.of();
		}
		String prefix = label.substring(0, label.lastIndexOf(' '));
		return nicheTermRepository.findAll(PageRequest.of(0, 50, Sort.by("listingCount").descending())).stream()
				.filter(term -> !term.getLabel().equals(label))
				.filter(term -> term.getLabel().startsWith(prefix) || prefix.startsWith(term.getLabel()))
				.limit(8)
				.map(this::toItem)
				.toList();
	}

	private NicheTermItem toItem(NicheTerm term) {
		NicheTopListingItem top = listingNicheTermRepository.findByNicheTermId(term.getId()).stream()
				.map(ListingNicheTerm::getListingId)
				.map(listingRepository::findById)
				.flatMap(java.util.Optional::stream)
				.max(Comparator.comparing(
						listing -> listing.getLastScoreWeekly() == null ? 0.0 : listing.getLastScoreWeekly().doubleValue()))
				.map(this::toTopListing)
				.orElse(null);
		return new NicheTermItem(
				term.getSlug(),
				term.getLabel(),
				term.getWindowState(),
				term.getListingCount(),
				term.getNewEntrants14d(),
				term.getCloneDensity7d(),
				term.getBreakInRate(),
				term.getIncumbentAgeDays(),
				term.getEntrantMomentum(),
				term.getEtsyCount(),
				term.getWindowComputedAt(),
				top);
	}

	private NicheTopListingItem toTopListing(Listing listing) {
		String imageUrl = listing.getImages().stream()
				.sorted(Comparator.comparingInt(ListingImage::getRank))
				.map(ListingImage::getUrl)
				.filter(url -> url != null && !url.isBlank())
				.findFirst()
				.orElse(null);
		return new NicheTopListingItem(
				listing.getListingId(),
				listing.getTitle(),
				imageUrl,
				listing.getUrl(),
				listing.getLastScoreWeekly());
	}

	private Sort sortOrder(String sort) {
		return switch (sort == null ? "" : sort) {
			case "listings" -> Sort.by(Sort.Direction.DESC, "listingCount");
			case "clone" -> Sort.by(Sort.Direction.ASC, "cloneDensity7d");
			case "entrants" -> Sort.by(Sort.Direction.DESC, "newEntrants14d");
			default -> Sort.by(Sort.Direction.DESC, "entrantMomentum");
		};
	}
}
