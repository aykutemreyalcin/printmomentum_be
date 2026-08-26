package com.printmomentum.web;

import com.printmomentum.domain.User;
import com.printmomentum.domain.UserRoleEnum;
import com.printmomentum.util.CurrentUserHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/v1/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

	private final CurrentUserHolder currentUserHolder;

	public UserController(CurrentUserHolder currentUserHolder) {
		this.currentUserHolder = currentUserHolder;
	}

	@GetMapping
	public ResponseEntity<UserResponse> userDetail() {
		User user = currentUserHolder.getCurrentUser();
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
		}
		return ResponseEntity.ok(UserResponse.from(user));
	}

	public record UserResponse(
			Integer id, String name, String displayName, String email, UserRoleEnum role, Boolean active) {

		static UserResponse from(User user) {
			return new UserResponse(
					user.getId(),
					user.getName(),
					user.getDisplayName(),
					user.getEmail(),
					user.getRole().getValue(),
					user.getActive());
		}
	}
}
