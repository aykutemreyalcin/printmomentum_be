package com.printmomentum.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

	Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

	List<UserSession> findByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(Integer userId, LocalDateTime now);

	List<UserSession> findByUser_IdOrderByLastUsedAtDesc(Integer userId);

	@Query("select max(s.lastUsedAt) from UserSession s where s.user.id = :userId")
	LocalDateTime findLastUsedAt(@Param("userId") Integer userId);
}
