package com.example.fraud.dto;

import com.example.fraud.entity.BlacklistMerchant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Shapes of JSON for the merchant blacklist. */
public final class BlacklistDto {

    private BlacklistDto() {
    }

    public record Request(
            @NotBlank(message = "merchantName is required") @Size(max = 150) String merchantName,
            @NotBlank(message = "reason is required") @Size(max = 255) String reason) {
    }

    public record HistoryResponse(Long id, String merchantName, String reason, OffsetDateTime addedAt,
                                  OffsetDateTime removedAt, boolean active) {
        public static HistoryResponse from(BlacklistMerchant merchant) {
            return new HistoryResponse(merchant.getId(), merchant.getMerchantName(), merchant.getReason(),
                    merchant.getAddedAt().atOffset(ZoneOffset.UTC),
                    merchant.getRemovedAt() == null ? null : merchant.getRemovedAt().atOffset(ZoneOffset.UTC),
                    merchant.isActive());
        }
    }

    public record Response(Long id, String merchantName, String reason, OffsetDateTime addedAt) {

        public static Response from(BlacklistMerchant m) {
            return new Response(m.getId(), m.getMerchantName(), m.getReason(), m.getAddedAt().atOffset(ZoneOffset.UTC));
        }
    }
}
