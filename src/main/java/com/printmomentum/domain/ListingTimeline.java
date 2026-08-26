package com.printmomentum.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 4–8 timeline points for the listing workspace. Synthesized from listing timestamps
 * plus persisted bestseller events; not a row per crawl.
 */
public final class ListingTimeline {

	public static final String LISTED = "LISTED";
	public static final String FIRST_SEEN = "FIRST_SEEN";
	public static final String ENTERED_TOP = "ENTERED_TOP";
	public static final String BESTSELLER_SINCE = "BESTSELLER_SINCE";
	public static final String LAST_REVIEW = "LAST_REVIEW";

	private static final int MAX_POINTS = 8;

	public record Point(String kind, Instant at, String label) {
	}

	public List<Point> points(Listing listing, List<ListingEvent> events) {
		if (listing == null) {
			return List.of();
		}
		List<Point> points = new ArrayList<>();
		Instant listed = listing.getOriginalCreatedAt() != null
				? listing.getOriginalCreatedAt()
				: listing.getEtsyCreatedAt();
		add(points, LISTED, listed, "Listed");
		add(points, FIRST_SEEN, listing.getFirstSeenAt(), "First seen by us");
		add(points, ENTERED_TOP, listing.getFirstSeenInTopAt(), "Entered our top-N");
		add(points, BESTSELLER_SINCE, listing.getEtsyBestsellerSince(), "Bestseller since");
		add(points, LAST_REVIEW, listing.getLastReviewAt(), "Last public review");
		if (events != null) {
			for (ListingEvent event : events) {
				if (event != null && event.getKind() != null && event.getObservedAt() != null) {
					add(points, event.getKind(), event.getObservedAt(), labelFor(event.getKind()));
				}
			}
		}
		LinkedHashMap<String, Point> unique = new LinkedHashMap<>();
		points.sort(Comparator.comparing(Point::at).thenComparing(Point::kind));
		for (Point point : points) {
			unique.putIfAbsent(point.kind() + "|" + point.at(), point);
		}
		List<Point> ordered = new ArrayList<>(unique.values());
		if (ordered.size() <= MAX_POINTS) {
			return List.copyOf(ordered);
		}
		List<Point> clipped = new ArrayList<>();
		clipped.add(ordered.get(0));
		clipped.addAll(ordered.subList(ordered.size() - (MAX_POINTS - 1), ordered.size()));
		return List.copyOf(clipped);
	}

	private static void add(List<Point> points, String kind, Instant at, String label) {
		if (at != null) {
			points.add(new Point(kind, at, label));
		}
	}

	private static String labelFor(String kind) {
		return switch (kind) {
			case ListingEvent.ETSY_BESTSELLER_ON -> "Etsy bestseller on";
			case ListingEvent.ETSY_BESTSELLER_OFF -> "Etsy bestseller off";
			case ListingEvent.PM_BESTSELLER_ON -> "PM bestseller on";
			case ListingEvent.PM_BESTSELLER_OFF -> "PM bestseller off";
			default -> kind;
		};
	}
}
