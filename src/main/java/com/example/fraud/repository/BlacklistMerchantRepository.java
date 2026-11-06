package com.example.fraud.repository;

import com.example.fraud.entity.BlacklistMerchant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link BlacklistMerchant}. */
public interface BlacklistMerchantRepository extends JpaRepository<BlacklistMerchant, Long> {

    Optional<BlacklistMerchant> findByMerchantKey(String merchantKey);

    /** True when the merchant is currently blacklisted (removed_at IS NULL). Used by the blacklist rule. */
    boolean existsByMerchantKeyAndRemovedAtIsNull(String merchantKey);

    List<BlacklistMerchant> findByRemovedAtIsNullOrderByMerchantNameAsc();

    /** Includes removed entries so administrators can audit the blacklist's soft-delete history. */
    List<BlacklistMerchant> findAllByOrderByAddedAtDesc();
}
