package com.printmomentum.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByEmail(String email);

	List<User> findAllByOrderByIdAsc();

	long countByRole_ValueAndActive(UserRoleEnum role, Boolean active);
}
