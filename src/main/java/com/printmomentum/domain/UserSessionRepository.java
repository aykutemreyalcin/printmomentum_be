package com.printmomentum.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

	Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

	List<UserSession> findByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(Integer userId, LocalDateTime now);
}
