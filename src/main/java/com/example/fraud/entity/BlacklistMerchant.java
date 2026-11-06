package com.example.fraud.entity;

import lombok.Getter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * A merchant known to be involved in fraud. Matching is done on {@link #keyOf(String)} so that
 * "DarkBet Offshore", " darkbet offshore " and "DARKBET OFFSHORE" are the same merchant.
 *
 * <p>Removal is a SOFT delete ({@code removedAt}): we keep the history of what was blacklisted and why.
 */
@Getter // Lombok generates the getters at compile time incase I missed it.
// For future projects I will only use it for the getters and setters. I will write the rest of the code manually.
@Entity
@Table(name = "blacklist_merchants")
public class BlacklistMerchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_key", nullable = false, unique = true, length = 150)
    private String merchantKey;

    @Column(name = "merchant_name", nullable = false, length = 150)
    private String merchantName;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    protected BlacklistMerchant() {
    }

    public BlacklistMerchant(String merchantName, String reason, LocalDateTime addedAt) {
        this.merchantName = merchantName.trim();
        this.merchantKey = keyOf(merchantName);
        this.reason = reason;
        this.addedAt = addedAt;
    }

    /** The normalised form used for matching. */
    public static String keyOf(String merchantName) {
        return merchantName.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isActive() {
        return removedAt == null;
    }

    public void remove(LocalDateTime now) {
        this.removedAt = now;
    }

    /** Replace an active merchant's identifying details and reason. */
    public void update(String merchantName, String reason) {
        this.merchantName = merchantName.trim();
        this.merchantKey = keyOf(merchantName);
        this.reason = reason;
    }

    /** Put a previously removed merchant back on the list. */
    public void reactivate(String reason, LocalDateTime now) {
        this.reason = reason;
        this.addedAt = now;
        this.removedAt = null;
    }


     public Long getId(){
       return id;
     }
     public String getMerchantKey(){
      return merchantKey;
     }
     public String getMerchantName(){
      return merchantName;
     }
     public String getReason(){
       return reason;
     }
    public LocalDateTime getAddedAt(){
      return addedAt;
     }
     public LocalDateTime getRemovedAt(){
       return removedAt;
     }
     /* No setters are generated intentionally: entity changes must go through the
     * domain methods (e.g. updateProfile/transitionTo) so business rules and audit
     * history cannot be bypassed.
     */
}
