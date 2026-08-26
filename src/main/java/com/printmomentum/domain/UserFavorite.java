package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_favorite")
public class UserFavorite {

	@EmbeddedId
	private UserFavoriteId id = new UserFavoriteId();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("listingId")
	@JoinColumn(name = "listing_id", nullable = false)
	private Listing listing;

	@Column(nullable = false)
	private Instant createdAt;

	protected UserFavorite() {
	}

	public UserFavorite(User user, Listing listing, Instant createdAt) {
		this.user = user;
		this.listing = listing;
		this.id = new UserFavoriteId(user.getId(), listing.getListingId());
		this.createdAt = createdAt;
	}

	public UserFavoriteId getId() {
		return id;
	}

	public Listing getListing() {
		return listing;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
