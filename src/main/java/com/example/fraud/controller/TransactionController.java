package com.example.fraud.controller;

import com.example.fraud.dto.PageResponse;
import com.example.fraud.dto.TransactionDto;
import com.example.fraud.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Entry point for transactions. A controller stays THIN: it reads the HTTP request, calls ONE service
 * method and chooses the status code. No business rules live here.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Submit transactions for fraud screening")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /** {@code @Valid} runs the validation annotations on the body BEFORE this method is called (else 400). */
    @PostMapping
    @Operation(summary = "Submit a categorised transaction; returns it together with any fraud flags raised")
    public ResponseEntity<TransactionDto.Result> submit(@Valid @RequestBody TransactionDto.Request request) {
        TransactionDto.Result result = transactionService.ingest(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(result.transaction().id()).toUri();
        return ResponseEntity.created(location).body(result);
    }

    /** Batch endpoint for upstream systems; each batch is limited to 100 and commits atomically. */
    @PostMapping("/batch")
    @Operation(summary = "Submit up to 100 transactions as one atomic ingestion batch")
    public ResponseEntity<TransactionDto.BatchResult> submitBatch(
            @Valid @RequestBody TransactionDto.BatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.ingestBatch(request.transactions()));
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get one transaction by its client-supplied reference")
    public TransactionDto.Result getByReference(@PathVariable String reference) {
        return transactionService.findByReference(reference);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one transaction with its fraud flags")
    public TransactionDto.Result get(@PathVariable Long id) {
        return transactionService.findById(id);
    }

    @GetMapping
    @Operation(summary = "List transactions (paged, newest first), optionally for one customer")
    public PageResponse<TransactionDto.Response> list(
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.search(customerId, pageable);
    }
}
