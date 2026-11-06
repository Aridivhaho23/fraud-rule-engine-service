package com.example.fraud.repository;

import com.example.fraud.entity.FraudFlag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Data access for {@link FraudFlag}. {@link JpaSpecificationExecutor} adds dynamic, composable filters:
 * the analyst API combines any of customer / status / severity / rule / date into one query.
 */
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long>, JpaSpecificationExecutor<FraudFlag> {

    @EntityGraph(attributePaths = {"transaction", "transaction.customer"})
    List<FraudFlag> findByTransactionIdOrderByIdAsc(Long transactionId);

    /** Loads the transaction and customer in the same query so a page of flags does not trigger N+1 queries. */
    @Override
    @EntityGraph(attributePaths = {"transaction", "transaction.customer"})
    Page<FraudFlag> findAll(Specification<FraudFlag> spec, Pageable pageable);
}
