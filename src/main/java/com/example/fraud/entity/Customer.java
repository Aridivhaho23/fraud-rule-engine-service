package com.example.fraud.entity;

import lombok.Getter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * A bank customer. ERD: Customer 1 ---- * Transaction.
 *
 * <p>{@code homeCountry} is kept so future rules can ask "is this purchase far from home?".
 * All timestamps in this application are UTC.
 */
@Getter // Lombok generates the getters at compile time incase I missed it.
// For future projects I will only use it for the getters and setters. I will write the rest of the code manually.
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "home_country", nullable = false, length = 2)
    private String homeCountry;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Required by JPA; protected so application code must use the real constructor. */
    protected Customer() {
    }

    public Customer(String fullName, String email, String homeCountry, LocalDateTime createdAt) {
        this.fullName = fullName;
        this.email = email;
        this.homeCountry = homeCountry;
        this.createdAt = createdAt;
    }

    /** Replace editable profile fields; identity and original creation time remain unchanged. */
    public void updateProfile(String fullName, String email, String homeCountry) {
        this.fullName = fullName;
        this.email = email;
        this.homeCountry = homeCountry;
    }


     public Long getId(){
       return id;
     }
     public String getFullName(){
      return fullName;
     }
     public String getEmail(){
      return email;
     }
     public String getHomeCountry(){
      return homeCountry;
     }
     public LocalDateTime getCreatedAt(){
      return createdAt;
     }

     /*
     * No setters are generated intentionally: entity changes must go through the
     * domain methods (e.g. updateProfile/transitionTo) so business rules and audit
     * history cannot be bypassed.
     */
}
