package com.printmomentum.web;

import com.printmomentum.domain.User;
import com.printmomentum.domain.UserRoleEnum;
import com.printmomentum.util.CurrentUserHolder;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

	private final CurrentUserHolder currentUserHolder;
	private final UserService userService;

	public UserController(CurrentUserHolder currentUserHolder, UserService userService) {
		this.currentUserHolder = currentUserHolder;
		this.userService = userService;
	}

	@GetMapping
	public ResponseEntity<UserResponse> userDetail() {
		User user = currentUserHolder.getCurrentUser();
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
		}
		return ResponseEntity.ok(UserResponse.from(user, userService.lastLoginAt(user)));
	}

	@PatchMapping
	public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request) {
		userService.changePassword(request, currentUserHolder.getCurrentUser());
		return ResponseEntity.accepted().build();
	}

	@PutMapping
	public ResponseEntity<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
		User updated = userService.updateProfile(request, currentUserHolder.getCurrentUser());
		return ResponseEntity.ok(UserResponse.from(updated, userService.lastLoginAt(updated)));
	}

	@GetMapping("/members")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<List<UserResponse>> members() {
		return ResponseEntity.ok(userService.listMembers().stream()
				.map(user -> UserResponse.from(user, userService.lastLoginAt(user)))
				.toList());
	}

	@GetMapping("/members/{id}/sessions")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<List<UserService.UserSessionView>> sessions(@PathVariable Integer id) {
		return ResponseEntity.ok(userService.listSessions(id));
	}

	@PatchMapping("/{id}")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<Void> changeActive(@PathVariable Integer id, @RequestParam boolean status) {
		userService.changeActive(id, status);
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/register")
	@PreAuthorize("hasRole('admin')")
	public ResponseEntity<Integer> register(@RequestBody RegisterUserRequest request) {
		return ResponseEntity.ok(userService.register(request));
	}

	public record UserResponse(
			Integer id,
			String name,
			String displayName,
			String email,
			UserRoleEnum role,
			Boolean active,
			java.time.Instant lastLoginAt) {

		static UserResponse from(User user, java.time.Instant lastLoginAt) {
			return new UserResponse(
					user.getId(),
					user.getName(),
					user.getDisplayName(),
					user.getEmail(),
					user.getRole().getValue(),
					user.getActive(),
					lastLoginAt);
		}
	}
}
