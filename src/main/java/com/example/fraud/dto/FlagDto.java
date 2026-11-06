package com.example.fraud.dto;

import com.example.fraud.entity.FlagStatus;
import com.example.fraud.entity.FlagStatusChange;
import com.example.fraud.entity.FraudFlag;
import com.example.fraud.entity.Severity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Shapes of JSON for fraud flags and their audit trail. */
public final class FlagDto {

    private FlagDto() {
    }

    /** Body of PATCH /api/v1/flags/{id}/status. */
    public record StatusUpdateRequest(
            @NotNull(message = "status is required") FlagStatus status,
            @Size(max = 500, message = "note may be at most 500 characters") String note) {
    }

    public record Response(Long id, Long transactionId, String transactionReference, Long customerId,
                           String merchantName, BigDecimal amount, String ruleCode, Severity severity,
                           int riskScore, String reason, FlagStatus status, OffsetDateTime flaggedAt,
                           String reviewedBy, OffsetDateTime reviewedAt, String reviewNote) {

        public static Response from(FraudFlag f) {
            return new Response(f.getId(), f.getTransaction().getId(), f.getTransaction().getReference(),
                    f.getTransaction().getCustomer().getId(), f.getTransaction().getMerchantName(),
                    f.getTransaction().getAmount(), f.getRuleCode(), f.getSeverity(), f.getRiskScore(),
                    f.getReason(), f.getStatus(), f.getFlaggedAt().atOffset(ZoneOffset.UTC),
                    f.getReviewedBy(),
                    f.getReviewedAt() == null ? null : f.getReviewedAt().atOffset(ZoneOffset.UTC),
                    f.getReviewNote());
        }
    }

    /** One line of the audit trail. */
    public record HistoryEntry(Long id, FlagStatus fromStatus, FlagStatus toStatus, String changedBy,
                               OffsetDateTime changedAt, String note) {

        public static HistoryEntry from(FlagStatusChange c) {
            return new HistoryEntry(c.getId(), c.getFromStatus(), c.getToStatus(), c.getChangedBy(),
                    c.getChangedAt().atOffset(ZoneOffset.UTC), c.getNote());
        }
    }
}
