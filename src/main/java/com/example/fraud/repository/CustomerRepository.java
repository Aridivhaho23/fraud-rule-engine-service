package com.example.fraud.repository;

import com.example.fraud.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Customer}. Spring Data generates the implementation. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    /** Ignore the current row when checking whether an email is already assigned. */
    boolean existsByEmailAndIdNot(String email, Long id);
}
