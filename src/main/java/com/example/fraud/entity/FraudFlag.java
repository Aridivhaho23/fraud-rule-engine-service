package com.example.fraud.entity;

import com.example.fraud.exception.BusinessRuleException;
import lombok.Getter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

/**
 * The record that "rule X thinks transaction Y looks fraudulent". One transaction can have many
 * flags (one per rule that fired).
 *
 * <p>An analyst moves the flag through {@link FlagStatus}. The rules for that live in
 * {@link #transitionTo}, so the status can never be changed in an illegal way.
 */
@Getter // Lombok generates the getters at compile time incase I missed it.
// For future projects I will only use it for the getters and setters. I will write the rest of the code manually.
@Entity
@Table(name = "fraud_flags")
public class FraudFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /** Which rule fired, e.g. HIGH_AMOUNT. A text code (not an FK) so rules can be added without a migration. */
    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Convert(converter = SeverityConverter.class)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    /** 0-100, higher = more suspicious. */
    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Convert(converter = FlagStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private FlagStatus status;

    @Column(name = "flagged_at", nullable = false)
    private LocalDateTime flaggedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    /** Optimistic locking: if two analysts update the same flag at once, the second one gets a 409. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected FraudFlag() {
    }

    /** Creates a new OPEN flag. (The entity layer does not depend on the rule package.) */
    public FraudFlag(Transaction transaction, String ruleCode, Severity severity, int riskScore,
                     String reason, LocalDateTime flaggedAt) {
        this.transaction = transaction;
        this.ruleCode = ruleCode;
        this.severity = severity;
        this.riskScore = riskScore;
        this.reason = truncate(reason, 500);
        this.status = FlagStatus.OPEN;
        this.flaggedAt = flaggedAt;
    }

    /**
     * Analyst decision. Enforces the life-cycle rules and records who decided and when.
     *
     * @throws BusinessRuleException if the move is not allowed (e.g. from a final status)
     */
    public void transitionTo(FlagStatus target, String reviewer, String note, LocalDateTime now) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException("Flag " + id + " cannot move from " + status + " to " + target);
        }
        this.status = target;
        this.reviewedBy = reviewer;
        this.reviewedAt = now;
        this.reviewNote = note == null ? null : truncate(note, 500);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    public Long getId(){
      return id;
     }
     public Transaction getTransaction(){
      return transaction;
     }
    public String getRuleCode(){
      return ruleCode;
     }
     public Severity getSeverity(){
      return severity;
     }
     public int getRiskScore(){
      return riskScore;
     }
     public String getReason(){
      return reason;
     }
     public FlagStatus getStatus(){
      return status;
     }
     public LocalDateTime getFlaggedAt(){
      return flaggedAt;
     }
     public String getReviewedBy() {
      return reviewedBy;
     }
     public LocalDateTime getReviewedAt() {
      return reviewedAt;
     }
     public String getReviewNote() {
      return reviewNote;
     }

     /*
     * No setters are generated intentionally: entity changes must go through the
     * domain methods (e.g. updateProfile/transitionTo) so business rules and audit
     * history cannot be bypassed.
     */
}
