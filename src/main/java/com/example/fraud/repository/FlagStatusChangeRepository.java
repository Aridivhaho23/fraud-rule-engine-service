package com.example.fraud.repository;

import com.example.fraud.entity.FlagStatusChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for the append-only audit trail. */
public interface FlagStatusChangeRepository extends JpaRepository<FlagStatusChange, Long> {

    List<FlagStatusChange> findByFlagIdOrderByIdAsc(Long flagId);
}
