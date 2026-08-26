package com.printmomentum.web;

import com.printmomentum.config.JwtAuthenticationFilter;
import com.printmomentum.domain.User;
import com.printmomentum.domain.UserRepository;
import com.printmomentum.domain.UserRole;
import com.printmomentum.domain.UserRoleEnum;
import com.printmomentum.domain.UserRoleRepository;
import com.printmomentum.domain.UserSessionRepository;
import com.printmomentum.util.CurrentUserHolder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

	private static final int MIN_PASSWORD_LENGTH = 6;
	private static final String REVOKE_PASSWORD_CHANGED = "password_changed";
	private static final String REVOKE_DEACTIVATED = "deactivated";

	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final UserSessionRepository userSessionRepository;
	private final PasswordEncoder passwordEncoder;
	private final CurrentUserHolder currentUserHolder;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public UserService(
			UserRepository userRepository,
			UserRoleRepository userRoleRepository,
			UserSessionRepository userSessionRepository,
			PasswordEncoder passwordEncoder,
			CurrentUserHolder currentUserHolder,
			JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.userRepository = userRepository;
		this.userRoleRepository = userRoleRepository;
		this.userSessionRepository = userSessionRepository;
		this.passwordEncoder = passwordEncoder;
		this.currentUserHolder = currentUserHolder;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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

	public List<User> listMembers() {
		return userRepository.findAllByOrderByIdAsc();
	}

	public Instant lastLoginAt(User user) {
		if (user.getId() == null) {
			return null;
		}
		return toInstant(userSessionRepository.findLastUsedAt(user.getId()));
	}

	public List<UserSessionView> listSessions(Integer userId) {
		userRepository
				.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Can't find user"));
		LocalDateTime now = LocalDateTime.now();
		return userSessionRepository.findByUser_IdOrderByLastUsedAtDesc(userId).stream()
				.map(session -> new UserSessionView(
						session.getId(),
						session.getDeviceId(),
						session.getIpAddress(),
						session.getUserAgent(),
						toInstant(session.getLastUsedAt()),
						toInstant(session.getCreatedAt()),
						toInstant(session.getExpiresAt()),
						session.getRevokedAt() == null && session.getExpiresAt() != null && session.getExpiresAt().isAfter(now)))
				.toList();
	}

	@Transactional
	public User updateProfile(UpdateProfileRequest request, User user) {
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
		}
		requireText(request.getName(), "Name is required");
		user.setName(request.getName().trim());
		String displayName = request.getDisplayName() == null || request.getDisplayName().isBlank()
				? request.getName().trim()
				: request.getDisplayName().trim();
		user.setDisplayName(displayName);
		String previousEmail = user.getEmail();
		if (request.getEmail() != null && !request.getEmail().isBlank()) {
			String nextEmail = request.getEmail().trim();
			if (!nextEmail.equalsIgnoreCase(previousEmail)) {
				requireText(request.getCurrentPassword(), "Current password is required");
				if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
					throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Wrong password");
				}
				if (userRepository.findByEmail(nextEmail).isPresent()) {
					throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Email already in use");
				}
				user.setEmail(nextEmail);
				jwtAuthenticationFilter.evictUser(previousEmail);
				revokeAllActiveSessions(user, "email_changed");
			}
		}
		User saved = userRepository.save(user);
		jwtAuthenticationFilter.evictUser(saved.getEmail());
		return saved;
	}

	@Transactional
	public void changeActive(Integer id, boolean status) {
		User actor = currentUserHolder.getCurrentUser();
		if (actor == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
		}
		User target = userRepository
				.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Can't find user"));
		if (actor.getId().equals(target.getId())) {
			throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Cannot deactivate your own account");
		}
		if (!status && target.getRole().getValue() == UserRoleEnum.admin) {
			long activeAdmins = userRepository.countByRole_ValueAndActive(UserRoleEnum.admin, true);
			if (activeAdmins <= 1) {
				throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Cannot deactivate the last admin");
			}
		}
		target.setActive(status);
		userRepository.save(target);
		jwtAuthenticationFilter.evictUser(target.getEmail());
		if (!status) {
			revokeAllActiveSessions(target, REVOKE_DEACTIVATED);
		}
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

	private static Instant toInstant(LocalDateTime value) {
		return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
	}

	public record UserSessionView(
			Long id,
			String deviceId,
			String ipAddress,
			String userAgent,
			Instant lastUsedAt,
			Instant createdAt,
			Instant expiresAt,
			boolean active) {
	}
}
