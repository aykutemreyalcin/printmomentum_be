package com.printmomentum.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UserFavoriteId> {

	@Query(
			"""
					select distinct f from UserFavorite f
					join fetch f.listing l
					join fetch l.shop
					left join fetch l.images
					where f.id.userId = :userId
					order by f.createdAt desc
					""")
	List<UserFavorite> findWithListingByUserId(@Param("userId") Integer userId);

	@Query("select f.id.listingId from UserFavorite f where f.id.userId = :userId")
	List<Long> findListingIdsByUserId(@Param("userId") Integer userId);
}
