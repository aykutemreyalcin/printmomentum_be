package com.printmomentum.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {

	Optional<ApiClient> findByApiKeyAndActiveTrue(String apiKey);
}
