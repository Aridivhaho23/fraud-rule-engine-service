package com.example.fraud.service;

import com.example.fraud.dto.CustomerDto;
import com.example.fraud.dto.PageResponse;
import com.example.fraud.entity.Customer;
import com.example.fraud.exception.ConflictException;
import com.example.fraud.exception.ResourceNotFoundException;
import com.example.fraud.repository.CustomerRepository;
import com.example.fraud.repository.TransactionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for customers. (In the full platform these come from the core banking system.) */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final Clock clock;
    private final TransactionRepository transactionRepository;

    public CustomerService(CustomerRepository customerRepository, TransactionRepository transactionRepository, Clock clock) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    @Transactional
    public CustomerDto.Response create(CustomerDto.Request request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new ConflictException("A customer with e-mail " + request.email() + " already exists");
        }
        Customer customer = new Customer(request.fullName(), request.email(), request.homeCountry(),
                LocalDateTime.now(clock));
        return CustomerDto.Response.from(customerRepository.save(customer));
    }

    /** Full replacement of the customer's editable profile; historical IDs/timestamps are preserved. */
    @Transactional
    public CustomerDto.Response update(Long id, CustomerDto.Request request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        if (customerRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException("A different customer already uses e-mail " + request.email());
        }
        customer.updateProfile(request.fullName(), request.email(), request.homeCountry());
        return CustomerDto.Response.from(customer);
    }

    /** Delete only unused customer records; never cascade-delete a financial transaction history. */
    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
        if (transactionRepository.existsByCustomerId(id)) {
            throw new ConflictException("Customer " + id + " has transactions and cannot be deleted; retain the ledger for audit integrity");
        }
        customerRepository.delete(customer);
    }

    public CustomerDto.Response findById(Long id) {
        return customerRepository.findById(id).map(CustomerDto.Response::from)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    public PageResponse<CustomerDto.Response> findAll(Pageable pageable) {
        return PageResponse.from(customerRepository.findAll(pageable), CustomerDto.Response::from);
    }
}
