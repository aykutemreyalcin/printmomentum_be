package com.printmomentum.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NicheTermRepository extends JpaRepository<NicheTerm, Long> {

	Optional<NicheTerm> findBySlug(String slug);

	Optional<NicheTerm> findByLabel(String label);

	Page<NicheTerm> findByWindowState(String windowState, Pageable pageable);

	Page<NicheTerm> findAll(Pageable pageable);

	@Query(
			"""
			select n from NicheTerm n
			where lower(n.label) like lower(concat('%', :q, '%'))
			   or lower(n.slug) like lower(concat('%', :q, '%'))
			""")
	Page<NicheTerm> searchByLabelOrSlug(@Param("q") String q, Pageable pageable);

	@Query(
			"""
			select n from NicheTerm n
			where n.windowState = :windowState
			  and (lower(n.label) like lower(concat('%', :q, '%'))
			    or lower(n.slug) like lower(concat('%', :q, '%')))
			""")
	Page<NicheTerm> searchByWindowAndLabelOrSlug(
			@Param("windowState") String windowState, @Param("q") String q, Pageable pageable);

	@Query("select n.windowState, count(n) from NicheTerm n group by n.windowState")
	List<Object[]> countByWindowState();

	List<NicheTerm> findByListingCountGreaterThanEqualAndEtsyCheckedAtIsNullOrderByListingCountDesc(int minListings, Pageable pageable);
}
