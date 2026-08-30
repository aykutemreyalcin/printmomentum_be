package com.printmomentum.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface NicheWindowSnapshotRepository extends JpaRepository<NicheWindowSnapshot, NicheWindowSnapshot.NicheWindowSnapshotId> {

	List<NicheWindowSnapshot> findByNicheTermIdOrderByObservedDayDesc(Long nicheTermId, org.springframework.data.domain.Pageable pageable);

	@Modifying
	void deleteByObservedDay(LocalDate observedDay);

	@Modifying
	void deleteByNicheTermIdAndObservedDay(Long nicheTermId, LocalDate observedDay);
}
