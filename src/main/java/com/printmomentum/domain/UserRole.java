package com.printmomentum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_role")
public class UserRole {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(name = "role_value", nullable = false, length = 32)
	private UserRoleEnum value;

	protected UserRole() {
	}

	public Integer getId() {
		return id;
	}

	public UserRoleEnum getValue() {
		return value;
	}

	public void setValue(UserRoleEnum value) {
		this.value = value;
	}
}
