package com.example.fraud.controller;

import com.example.fraud.dto.CustomerDto;
import com.example.fraud.dto.PageResponse;
import com.example.fraud.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Customers referenced by transactions. */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(summary = "Register a customer (ADMIN)")
    public ResponseEntity<CustomerDto.Response> create(@Valid @RequestBody CustomerDto.Request request) {
        CustomerDto.Response created = customerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a customer's profile (ADMIN)", description = "Full update of fullName, email and homeCountry. Customer ID and creation timestamp are preserved.")
    public CustomerDto.Response update(@PathVariable Long id, @Valid @RequestBody CustomerDto.Request request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer with no transactions (ADMIN)", description = "Deletion is rejected with 409 if the customer has transaction history, preserving the financial audit ledger.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a customer")
    public CustomerDto.Response get(@PathVariable Long id) {
        return customerService.findById(id);
    }

    @GetMapping
    @Operation(summary = "List customers (paged)")
    public PageResponse<CustomerDto.Response> list(Pageable pageable) {
        return customerService.findAll(pageable);
    }
}
