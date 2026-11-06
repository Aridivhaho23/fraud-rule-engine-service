package com.example.fraud.repository;

import com.example.fraud.entity.FlagStatus;
import com.example.fraud.entity.FraudFlag;
import com.example.fraud.entity.Severity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small reusable filter "building blocks" for the flag search.
 * Each method returns a {@link Specification}: a piece of a WHERE clause. When the filter value is
 * null the block adds NO restriction, so callers can chain them all without if-statements.
 */
public final class FlagSpecifications {

    private FlagSpecifications() {
    }

    public static Specification<FraudFlag> customerId(Long customerId) {
        return (root, query, cb) -> customerId == null ? null
                : cb.equal(root.get("transaction").get("customer").get("id"), customerId);
    }

    public static Specification<FraudFlag> transactionId(Long transactionId) {
        return (root, query, cb) -> transactionId == null ? null
                : cb.equal(root.get("transaction").get("id"), transactionId);
    }

    public static Specification<FraudFlag> ruleCode(String ruleCode) {
        return (root, query, cb) -> ruleCode == null ? null : cb.equal(root.get("ruleCode"), ruleCode);
    }

    public static Specification<FraudFlag> severity(Severity severity) {
        return (root, query, cb) -> severity == null ? null : cb.equal(root.get("severity"), severity);
    }

    public static Specification<FraudFlag> status(FlagStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<FraudFlag> flaggedFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.<LocalDateTime>get("flaggedAt"), from);
    }

    public static Specification<FraudFlag> flaggedTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.<LocalDateTime>get("flaggedAt"), to);
    }
}
