package com.printmomentum.web;

import com.printmomentum.domain.User;
import com.printmomentum.domain.UserRepository;
import com.printmomentum.domain.UserRole;
import com.printmomentum.domain.UserRoleEnum;
import com.printmomentum.domain.UserRoleRepository;
import com.printmomentum.domain.UserSessionRepository;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

	private static final int MIN_PASSWORD_LENGTH = 6;
	private static final String REVOKE_PASSWORD_CHANGED = "password_changed";

	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final UserSessionRepository userSessionRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			UserSessionRepository userSessionRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.userSessionRepository = userSessionRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void changePassword(ChangePasswordRequest request, User user) {
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
		}
		requireText(request.getCurrentPassword(), "Current password is required");
		requireText(request.getNewPassword(), "New password is required");
		requireText(request.getConfirmationPassword(), "Please confirm your new password");
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Wrong password");
		}
		if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Password are not the same");
		}
		requirePasswordLength(request.getNewPassword());
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
		revokeAllActiveSessions(user, REVOKE_PASSWORD_CHANGED);
	}

	@Transactional
	public Integer register(RegisterUserRequest request) {
		requireText(request.getEmail(), "Email is required");
		requireText(request.getPassword(), "Password is required");
		requireText(request.getName(), "Name is required");
		requirePasswordLength(request.getPassword());
		if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Email already in use");
		}
		UserRoleEnum roleValue = request.getRole() == null ? UserRoleEnum.user : request.getRole();
		UserRole userRole = userRoleRepository
				.findByValue(roleValue)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "User role not found"));
		User created = new User(request.getEmail().trim(), passwordEncoder.encode(request.getPassword()), userRole);
		created.setName(request.getName().trim());
		String displayName = request.getDisplayName() == null || request.getDisplayName().isBlank()
				? request.getName().trim()
				: request.getDisplayName().trim();
		created.setDisplayName(displayName);
		return userRepository.save(created).getId();
	}

	private void revokeAllActiveSessions(User user, String reason) {
		if (user.getId() == null) {
			return;
		}
		LocalDateTime now = LocalDateTime.now();
		var activeSessions =
				userSessionRepository.findByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(user.getId(), now);
		if (activeSessions.isEmpty()) {
			return;
		}
		activeSessions.forEach(session -> {
			session.setRevokedAt(now);
			session.setRevokeReason(reason);
		});
		userSessionRepository.saveAll(activeSessions);
	}

	private static void requireText(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
		}
	}

	private static void requirePasswordLength(String password) {
		if (password.length() < MIN_PASSWORD_LENGTH) {
			throw new ResponseStatusException(
					HttpStatus.NOT_ACCEPTABLE, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}
	}
}
