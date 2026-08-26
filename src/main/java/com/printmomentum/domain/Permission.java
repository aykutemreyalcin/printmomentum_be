package com.printmomentum.domain;

public enum Permission {

	ADMIN("admin"),
	USER("user");

	private final String permission;

	Permission(String permission) {
		this.permission = permission;
	}

	public String getPermission() {
		return permission;
	}
}
