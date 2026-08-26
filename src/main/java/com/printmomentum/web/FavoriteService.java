package com.printmomentum.web;

import com.printmomentum.domain.Listing;
import com.printmomentum.domain.ListingRepository;
import com.printmomentum.domain.User;
import com.printmomentum.domain.UserFavorite;
import com.printmomentum.domain.UserFavoriteId;
import com.printmomentum.domain.UserFavoriteRepository;
import com.printmomentum.domain.UserRepository;
import com.printmomentum.util.CurrentUserHolder;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FavoriteService {

	private final CurrentUserHolder currentUserHolder;
	private final UserRepository userRepository;
	private final ListingRepository listingRepository;
	private final UserFavoriteRepository userFavoriteRepository;

	public FavoriteService(
			CurrentUserHolder currentUserHolder,
			UserRepository userRepository,
			ListingRepository listingRepository,
			UserFavoriteRepository userFavoriteRepository) {
		this.currentUserHolder = currentUserHolder;
		this.userRepository = userRepository;
		this.listingRepository = listingRepository;
		this.userFavoriteRepository = userFavoriteRepository;
	}

	@Transactional
	public void add(long listingId) {
		User user = requireUser();
		Listing listing = listingRepository
				.findById(listingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "listing not found"));
		UserFavoriteId id = new UserFavoriteId(user.getId(), listingId);
		if (userFavoriteRepository.existsById(id)) {
			return;
		}
		userFavoriteRepository.save(new UserFavorite(
				userRepository.getReferenceById(user.getId()), listing, Instant.now()));
	}

	@Transactional
	public void remove(long listingId) {
		User user = requireUser();
		userFavoriteRepository.deleteById(new UserFavoriteId(user.getId(), listingId));
	}

	private User requireUser() {
		User user = currentUserHolder.getCurrentUser();
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "login required");
		}
		return user;
	}
}
