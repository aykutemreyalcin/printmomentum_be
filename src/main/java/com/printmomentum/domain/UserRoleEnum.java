package com.printmomentum.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum UserRoleEnum {

	admin(Set.of(Permission.ADMIN)),
	user(Set.of(Permission.USER));

	private final Set<Permission> permissions;

	UserRoleEnum(Set<Permission> permissions) {
		this.permissions = permissions;
	}

	public Set<Permission> getPermissions() {
		return permissions;
	}

	public List<SimpleGrantedAuthority> getAuthorities() {
		var authorities = getPermissions().stream()
				.map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
				.collect(Collectors.toList());
		authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
		return authorities;
	}

	public static UserRoleEnum fromString(String roleName) {
		try {
			return UserRoleEnum.valueOf(roleName);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid role name: " + roleName);
		}
	}
}
