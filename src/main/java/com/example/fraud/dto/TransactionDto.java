package com.example.fraud.dto;

import com.example.fraud.entity.FraudFlag;
import com.example.fraud.entity.MerchantCategory;
import com.example.fraud.entity.Severity;
import com.example.fraud.entity.Transaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/**
 * Shapes of JSON for transactions. DTOs keep the public API independent of the database entities.
 */
public final class TransactionDto {

    private TransactionDto() {
    }

    /**
     * A categorised transaction event. Example:
     * <pre>
     * { "reference": "TX-1001", "customerId": 1, "amount": 25000.00, "merchantName": "Gadget World",
     *   "merchantCategory": "RETAIL", "country": "ZA", "occurredAt": "2026-10-31T09:15:00Z" }
     * </pre>
     */
    public record Request(
            @NotBlank(message = "reference is required") @Size(max = 64) String reference,
            @NotNull(message = "customerId is required") Long customerId,
            @NotNull(message = "amount is required")
            @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
            @Digits(integer = 15, fraction = 2, message = "amount may have at most 2 decimal places")
            BigDecimal amount,
            @NotBlank(message = "merchantName is required") @Size(max = 150) String merchantName,
            @NotNull(message = "merchantCategory is required") MerchantCategory merchantCategory,
            @NotBlank(message = "country is required")
            @Pattern(regexp = "[A-Z]{2}", message = "country must be a 2-letter ISO code such as ZA") String country,
            @NotNull(message = "occurredAt is required (ISO-8601 with offset, e.g. 2026-10-31T09:15:00Z)")
            OffsetDateTime occurredAt) {
    }

    /** Batch ingestion envelope. Batches are deliberately capped to protect the synchronous screening API. */
    public record BatchRequest(
            @NotEmpty(message = "transactions must contain at least one item")
            @Size(max = 100, message = "a batch may contain at most 100 transactions")
            List<@NotNull @Valid Request> transactions) {
    }

    /** Summary returned after every item in a batch has been screened and committed together. */
    public record BatchResult(int received, int flagged, List<Result> results) {
    }

    /** A stored transaction without its flags. */
    public record Response(Long id, String reference, Long customerId, BigDecimal amount, String merchantName,
                           MerchantCategory merchantCategory, String country, OffsetDateTime occurredAt) {

        public static Response from(Transaction t) {
            return new Response(t.getId(), t.getReference(), t.getCustomer().getId(), t.getAmount(),
                    t.getMerchantName(), t.getMerchantCategory(), t.getCountry(),
                    t.getOccurredAt().atOffset(ZoneOffset.UTC));
        }

    }

    /**
     * What the ingestion API returns: the stored transaction PLUS the verdict of the rule engine.
     *
     * @param highestSeverity the most serious flag, or null when nothing fired
     * @param riskScore       the highest risk score among the flags (0 when nothing fired)
     */
    public record Result(Response transaction, boolean flagged, Severity highestSeverity, int riskScore,
                         List<FlagDto.Response> flags) {

        public static Result of(Transaction tx, List<FraudFlag> flags) {
            List<FlagDto.Response> flagDtos = flags.stream().map(FlagDto.Response::from).toList();
            Severity highest = flags.stream().map(FraudFlag::getSeverity).max(Comparator.naturalOrder()).orElse(null);
            int score = flags.stream().mapToInt(FraudFlag::getRiskScore).max().orElse(0);
            return new Result(Response.from(tx), !flags.isEmpty(), highest, score, flagDtos);
        }
    }
}
