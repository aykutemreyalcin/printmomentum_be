package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserFavoriteId implements Serializable {

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(name = "listing_id", nullable = false)
	private Long listingId;

	protected UserFavoriteId() {
	}

	public UserFavoriteId(Integer userId, Long listingId) {
		this.userId = userId;
		this.listingId = listingId;
	}

	public Integer getUserId() {
		return userId;
	}

	public Long getListingId() {
		return listingId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UserFavoriteId that)) {
			return false;
		}
		return Objects.equals(userId, that.userId) && Objects.equals(listingId, that.listingId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, listingId);
	}
}
