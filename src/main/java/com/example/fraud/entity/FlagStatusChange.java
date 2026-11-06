package com.example.fraud.entity;

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
import java.time.LocalDateTime;

/**
 * One line of the AUDIT TRAIL: who moved which flag from which status to which, and when.
 * Append-only - there are no setters and no code path that updates or deletes these rows.
 */
@Getter // Lombok generates the getters at compile time incase I missed it.
// For future projects I will only use it for the getters and setters. I will write the rest of the code manually.
@Entity
@Table(name = "flag_status_history")
public class FlagStatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FraudFlag flag;

    @Convert(converter = FlagStatusConverter.class)
    @Column(name = "from_status", nullable = false, length = 20)
    private FlagStatus fromStatus;

    @Convert(converter = FlagStatusConverter.class)
    @Column(name = "to_status", nullable = false, length = 20)
    private FlagStatus toStatus;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "note", length = 500)
    private String note;

    protected FlagStatusChange() {
    }

    public FlagStatusChange(FraudFlag flag, FlagStatus fromStatus, FlagStatus toStatus,
                            String changedBy, LocalDateTime changedAt, String note) {
        this.flag = flag;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.note = note;
    }

    public Long getId(){
      return id;
     }
     public FraudFlag getFlag(){
      return flag;
     }
     public FlagStatus getFromStatus(){
      return fromStatus;
     }
     public FlagStatus getToStatus(){
      return toStatus;
     }
     public String getChangedBy(){
      return changedBy;
     }
     public LocalDateTime getChangedAt(){
      return changedAt;
     }
     public String getNote(){
      return note;
     }

     /*
     * No setters are generated intentionally: entity changes must go through the
     * domain methods (e.g. updateProfile/transitionTo) so business rules and audit
     * history cannot be bypassed.
     */
}
