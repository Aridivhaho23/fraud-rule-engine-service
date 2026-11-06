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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A financial transaction received for fraud screening. It is IMMUTABLE once stored: no setters,
 * no update methods - the fraud history must reflect exactly what was received.
 *
 * <p>ERD: Customer 1 ---- * Transaction 1 ---- * FraudFlag.
 */
@Getter // Lombok generates the getters at compile time incase I missed it.
// For future projects I will only use it for the getters and setters. I will write the rest of the code manually.
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many transactions -> one customer. LAZY: the customer row is loaded only when needed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Unique id from the sending system. Sending the same reference twice is rejected (idempotency). */
    @Column(name = "reference", nullable = false, unique = true, length = 64)
    private String reference;

    /** BigDecimal, never double: binary floating point cannot represent most decimal amounts exactly. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "merchant_name", nullable = false, length = 150)
    private String merchantName;

    @Convert(converter = MerchantCategoryConverter.class)
    @Column(name = "merchant_category", nullable = false, length = 30)
    private MerchantCategory merchantCategory;

    /** ISO country code where the purchase happened, e.g. ZA. */
    @Column(name = "country", nullable = false, length = 2)
    private String country;

    /** When the transaction happened (UTC). */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    /** When we received it (UTC). */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Transaction() {
    }

    public Transaction(Customer customer, String reference, BigDecimal amount, String merchantName,
                       MerchantCategory merchantCategory, String country,
                       LocalDateTime occurredAt, LocalDateTime createdAt) {
        this.customer = customer;
        this.reference = reference;
        this.amount = amount;
        this.merchantName = merchantName;
        this.merchantCategory = merchantCategory;
        this.country = country;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
    }

    public Long getId(){
      return id;
     }
     public Customer getCustomer(){
      return customer;
     }
     public String getReference(){
      return reference;
     }
     public BigDecimal getAmount(){
      return amount;
     }
     public String getMerchantName(){
      return merchantName;
     }
     public MerchantCategory getMerchantCategory(){
      return merchantCategory;
     }
     public String getCountry() {
      return country;
     }
     public LocalDateTime getOccurredAt() {
      return occurredAt;
     }
     public LocalDateTime getCreatedAt() {
      return createdAt;
     }

     /*
     * No setters are generated intentionally: entity changes must go through the
     * domain methods (e.g. updateProfile/transitionTo) so business rules and audit
     * history cannot be bypassed.
     */
}
