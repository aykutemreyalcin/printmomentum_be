package com.printmomentum.auth;

public class V2AuthenticationResponse {

	private String token;
	private long expiresInSeconds;
	private String authVersion;

	public V2AuthenticationResponse() {
	}

	public V2AuthenticationResponse(String token, long expiresInSeconds, String authVersion) {
		this.token = token;
		this.expiresInSeconds = expiresInSeconds;
		this.authVersion = authVersion;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public long getExpiresInSeconds() {
		return expiresInSeconds;
	}

	public void setExpiresInSeconds(long expiresInSeconds) {
		this.expiresInSeconds = expiresInSeconds;
	}

	public String getAuthVersion() {
		return authVersion;
	}

	public void setAuthVersion(String authVersion) {
		this.authVersion = authVersion;
	}
}
