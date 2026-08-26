package com.printmomentum.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/auth")
public class V2AuthenticationController {

	private final V2AuthenticationService service;

	public V2AuthenticationController(V2AuthenticationService service) {
		this.service = service;
	}

	@PostMapping("/login")
	public ResponseEntity<V2AuthenticationResponse> login(
			@RequestBody V2AuthenticationRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse response) {
		return ResponseEntity.ok(service.authenticate(request, httpRequest, response));
	}

	@PostMapping("/refresh")
	public ResponseEntity<V2AuthenticationResponse> refresh(
			@RequestBody(required = false) V2RefreshRequest request,
			HttpServletRequest httpRequest,
			HttpServletResponse response) {
		return ResponseEntity.ok(service.refresh(request, httpRequest, response));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		service.logout(request, response);
		return ResponseEntity.noContent().build();
	}
}
