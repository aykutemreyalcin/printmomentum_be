package com.printmomentum.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NicheWindowSnapshotRepository extends JpaRepository<NicheWindowSnapshot, NicheWindowSnapshot.NicheWindowSnapshotId> {

	List<NicheWindowSnapshot> findByNicheTermIdOrderByObservedDayDesc(Long nicheTermId, org.springframework.data.domain.Pageable pageable);
}
