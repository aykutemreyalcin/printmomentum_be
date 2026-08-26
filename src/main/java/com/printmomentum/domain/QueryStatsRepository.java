package com.printmomentum.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueryStatsRepository extends JpaRepository<QueryStats, QueryStatsId> {

	@Query("select max(qs.id.observedDay) from QueryStats qs")
	Optional<LocalDate> findLatestObservedDay();

	List<QueryStats> findByIdObservedDayOrderByIdQueryAsc(LocalDate observedDay);

	List<QueryStats> findByIdQueryInAndIdObservedDay(Collection<String> queries, LocalDate observedDay);
}
