package com.example.fraud.repository;

import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Transaction}, including the "look back in time" queries the rules need.
 *
 * <p>The method NAMES are the queries (Spring Data "derived queries"). For example
 * {@code countByCustomerIdAndOccurredAtBetween} becomes
 * {@code SELECT count(*) ... WHERE customer_id = ? AND occurred_at BETWEEN ? AND ?}.
 * The index {@code idx_transactions_customer_time} makes these fast.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByReference(String reference);

    /** Stable client-supplied identifier; unlike the generated database ID, this is portable across environments. */
    @EntityGraph(attributePaths = {"customer"})
    java.util.Optional<Transaction> findByReference(String reference);

    /** A customer with ledger activity must be retained for audit and referential integrity. */
    boolean existsByCustomerId(Long customerId);

    /** Velocity rule: how many transactions did this customer make in a time window? */
    long countByCustomerIdAndOccurredAtBetween(Long customerId, LocalDateTime from, LocalDateTime to);

    /** Gambling rule: how many transactions of one category in a window? */
    long countByCustomerIdAndMerchantCategoryAndOccurredAtBetween(Long customerId, MerchantCategory category,
                                                                  LocalDateTime from, LocalDateTime to);

    /** Card-testing rule: how many SMALL transactions in a window? */
    long countByCustomerIdAndAmountLessThanEqualAndOccurredAtBetween(Long customerId, BigDecimal maxAmount,
                                                                     LocalDateTime from, LocalDateTime to);

    /** Geography rule: did this customer transact in a DIFFERENT country within the window (excluding this transaction)? */
    boolean existsByCustomerIdAndCountryNotAndIdNotAndOccurredAtBetween(Long customerId, String country,
                                                                        Long excludedTransactionId,
                                                                        LocalDateTime from, LocalDateTime to);

    /** Newest first. EntityGraph loads the customer in the same query (avoids the N+1 problem). */
    @EntityGraph(attributePaths = {"customer"})
    Page<Transaction> findByCustomerId(Long customerId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer"})
    Page<Transaction> findAll(Pageable pageable);
}
