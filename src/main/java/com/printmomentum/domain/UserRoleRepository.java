package com.printmomentum.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

	Optional<UserRole> findByValue(UserRoleEnum value);
}
