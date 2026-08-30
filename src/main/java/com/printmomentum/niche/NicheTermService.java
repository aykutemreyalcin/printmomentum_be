package com.printmomentum.niche;

import com.printmomentum.config.NicheProperties;
import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingNicheTerm;
import com.printmomentum.domain.ListingNicheTermRepository;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.NicheTerm;
import com.printmomentum.domain.NicheTermRepository;
import com.printmomentum.domain.NicheWindowSnapshot;
import com.printmomentum.domain.NicheWindowSnapshotRepository;
import com.printmomentum.ingest.EtsyClient;
import com.printmomentum.ingest.EtsySearchPage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class NicheTermService {

	private static final Logger log = LoggerFactory.getLogger(NicheTermService.class);
	private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

	private final ListingRepository listingRepository;
	private final NicheTermRepository nicheTermRepository;
	private final ListingNicheTermRepository listingNicheTermRepository;
	private final NicheWindowSnapshotRepository nicheWindowSnapshotRepository;
	private final NicheTermExtractor nicheTermExtractor;
	private final NicheWindowCalculator nicheWindowCalculator;
	private final NicheProperties properties;
	private final ObjectMapper objectMapper;
	private final EtsyClient etsyClient;

	public NicheTermService(
			ListingRepository listingRepository,
			NicheTermRepository nicheTermRepository,
			ListingNicheTermRepository listingNicheTermRepository,
			NicheWindowSnapshotRepository nicheWindowSnapshotRepository,
			NicheTermExtractor nicheTermExtractor,
			NicheWindowCalculator nicheWindowCalculator,
			NicheProperties properties,
			ObjectMapper objectMapper,
			EtsyClient etsyClient) {
		this.listingRepository = listingRepository;
		this.nicheTermRepository = nicheTermRepository;
		this.listingNicheTermRepository = listingNicheTermRepository;
		this.nicheWindowSnapshotRepository = nicheWindowSnapshotRepository;
		this.nicheTermExtractor = nicheTermExtractor;
		this.nicheWindowCalculator = nicheWindowCalculator;
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.etsyClient = etsyClient;
	}

	@Transactional
	public void assignListing(Listing listing) {
		if (listing == null || !listing.isPrintTee()) {
			return;
		}
		List<NicheTermExtractor.ExtractedTerm> extracted =
				nicheTermExtractor.extract(listing.getTitle(), readTags(listing.getTags()));
		listingNicheTermRepository.deleteByListingId(listing.getListingId());
		Instant now = Instant.now();
		Set<Long> assignedTermIds = new HashSet<>();
		for (NicheTermExtractor.ExtractedTerm extractedTerm : extracted) {
			if (!passesQuickFilter(extractedTerm.label())) {
				continue;
			}
			NicheTerm term = nicheTermRepository
					.findByLabel(extractedTerm.label())
					.orElseGet(() -> nicheTermRepository.save(new NicheTerm(
							NicheSlug.fromLabel(extractedTerm.label()), extractedTerm.label(), now)));
			term.touch(now);
			nicheTermRepository.save(term);
			if (assignedTermIds.add(term.getId())) {
				listingNicheTermRepository.save(new ListingNicheTerm(
						listing.getListingId(),
						term,
						BigDecimal.valueOf(extractedTerm.weight()).setScale(3, RoundingMode.HALF_UP),
						extractedTerm.source()));
			}
		}
	}

	@Transactional
	public int reindexAll() {
		List<Listing> listings = listingRepository.findByPrintTeeTrue();
		Map<String, Integer> docFreq = new HashMap<>();
		Map<Long, List<NicheTermExtractor.ExtractedTerm>> pending = new HashMap<>();
		for (Listing listing : listings) {
			List<NicheTermExtractor.ExtractedTerm> extracted =
					nicheTermExtractor.extract(listing.getTitle(), readTags(listing.getTags()));
			pending.put(listing.getListingId(), extracted);
			Set<String> uniqueLabels = new HashSet<>();
			for (NicheTermExtractor.ExtractedTerm term : extracted) {
				if (uniqueLabels.add(term.label())) {
					docFreq.merge(term.label(), 1, Integer::sum);
				}
			}
		}
		int total = Math.max(listings.size(), 1);
		listingNicheTermRepository.deleteAllInBatch();
		Map<String, NicheTerm> termsByLabel = new HashMap<>();
		Instant now = Instant.now();
		int assignments = 0;
		for (Listing listing : listings) {
			Set<Long> assignedTermIds = new HashSet<>();
			for (NicheTermExtractor.ExtractedTerm extractedTerm : pending.getOrDefault(listing.getListingId(), List.of())) {
				int freq = docFreq.getOrDefault(extractedTerm.label(), 0);
				if (freq / (double) total > properties.maxIdfRatio()) {
					continue;
				}
				if (!passesQuickFilter(extractedTerm.label())) {
					continue;
				}
				NicheTerm term = termsByLabel.computeIfAbsent(extractedTerm.label(), label -> nicheTermRepository
						.findByLabel(label)
						.orElseGet(() -> nicheTermRepository.save(new NicheTerm(NicheSlug.fromLabel(label), label, now))));
				term.touch(now);
				if (assignedTermIds.add(term.getId())) {
					listingNicheTermRepository.save(new ListingNicheTerm(
							listing.getListingId(),
							term,
							BigDecimal.valueOf(extractedTerm.weight()).setScale(3, RoundingMode.HALF_UP),
							extractedTerm.source()));
					assignments++;
				}
			}
		}
		nicheTermRepository.findAll().forEach(term -> {
			int count = listingNicheTermRepository.findByNicheTermId(term.getId()).size();
			term.setListingCount(count);
			nicheTermRepository.save(term);
		});
		recomputeWindows(now);
		validateEtsyCounts(now);
		log.info("niche reindex complete listings={} assignments={}", listings.size(), assignments);
		return assignments;
	}

	@Transactional
	public void recomputeWindows(Instant now) {
		LocalDate day = now.atZone(ISTANBUL).toLocalDate();
		for (NicheTerm term : nicheTermRepository.findAll()) {
			List<Listing> cohort = listingNicheTermRepository.findByNicheTermId(term.getId()).stream()
					.map(ListingNicheTerm::getListingId)
					.map(listingRepository::findById)
					.flatMap(java.util.Optional::stream)
					.filter(Listing::isPrintTee)
					.toList();
			term.setListingCount(cohort.size());
			NicheWindowCalculator.Metrics metrics = nicheWindowCalculator.compute(cohort, now);
			term.applyWindow(
					metrics.state().name(),
					metrics.newEntrants14d(),
					decimal(metrics.cloneDensity7d(), 4),
					decimal(metrics.breakInRate(), 4),
					metrics.incumbentAgeDays() == null ? null : decimal(metrics.incumbentAgeDays(), 2),
					metrics.entrantMomentum() == null ? null : decimal(metrics.entrantMomentum(), 6),
					now);
			nicheTermRepository.save(term);
			nicheWindowSnapshotRepository.save(new NicheWindowSnapshot(
					term,
					day,
					metrics.state().name(),
					metrics.listingCount(),
					metrics.newEntrants14d(),
					decimal(metrics.cloneDensity7d(), 4),
					decimal(metrics.breakInRate(), 4),
					metrics.incumbentAgeDays() == null ? null : decimal(metrics.incumbentAgeDays(), 2),
					metrics.entrantMomentum() == null ? null : decimal(metrics.entrantMomentum(), 6),
					term.getEtsyCount()));
		}
	}

	@Transactional
	public void validateEtsyCounts(Instant now) {
		List<NicheTerm> due = nicheTermRepository.findByListingCountGreaterThanEqualAndEtsyCheckedAtIsNullOrderByListingCountDesc(
				properties.etsyValidateMinListings(), PageRequest.of(0, properties.etsyValidateMaxPerDay()));
		int validated = 0;
		for (NicheTerm term : due) {
			if (validated >= properties.etsyValidateMaxPerDay()) {
				break;
			}
			try {
				EtsySearchPage page = etsyClient.searchActive(term.getLabel(), 1603L, 1, 0);
				term.setEtsyCount(page.count());
				term.setEtsyCheckedAt(now);
				nicheTermRepository.save(term);
				validated++;
			} catch (RuntimeException ex) {
				log.warn("niche etsy validate failed term={}: {}", term.getLabel(), ex.toString());
			}
		}
	}

	private boolean passesQuickFilter(String label) {
		if (label == null || label.isBlank()) {
			return false;
		}
		String slug = NicheSlug.fromLabel(label);
		return !slug.isBlank() && label.split("\\s+").length <= 4;
	}

	private List<String> readTags(String tagsJson) {
		if (tagsJson == null || tagsJson.isBlank()) {
			return List.of();
		}
		try {
			JsonNode tags = objectMapper.readTree(tagsJson);
			if (!tags.isArray()) {
				return List.of();
			}
			List<String> values = new ArrayList<>();
			for (JsonNode tag : tags) {
				if (tag.isString()) {
					values.add(tag.stringValue());
				}
			}
			return values;
		} catch (RuntimeException ex) {
			return List.of();
		}
	}

	private static BigDecimal decimal(double value, int scale) {
		return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
	}
}
