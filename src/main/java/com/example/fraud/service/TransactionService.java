package com.example.fraud.service;

import com.example.fraud.dto.PageResponse;
import com.example.fraud.dto.TransactionDto;
import com.example.fraud.entity.Customer;
import com.example.fraud.entity.FraudFlag;
import com.example.fraud.entity.Transaction;
import com.example.fraud.exception.BusinessRuleException;
import com.example.fraud.exception.ConflictException;
import com.example.fraud.exception.ResourceNotFoundException;
import com.example.fraud.repository.CustomerRepository;
import com.example.fraud.repository.FraudFlagRepository;
import com.example.fraud.repository.TransactionRepository;
import com.example.fraud.rule.FraudEngine;
import com.example.fraud.rule.RuleResult;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The main use case: receive a transaction, screen it, store the verdict.
 *
 * <p>Flow of {@link #ingest}: validate -> store the transaction -> {@link FraudEngine} runs every rule ->
 * store one {@link FraudFlag} per rule that fired. It is ONE database transaction: either the transaction
 * AND its flags are saved, or nothing is.
 */
@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    /** Allowed clock difference between the sending system and us. */
    private static final long MAX_FUTURE_MINUTES = 5;

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;
    private final FraudFlagRepository flagRepository;
    private final FraudEngine fraudEngine;
    private final Clock clock;

    public TransactionService(TransactionRepository transactionRepository, CustomerRepository customerRepository,
                              FraudFlagRepository flagRepository, FraudEngine fraudEngine, Clock clock) {
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
        this.flagRepository = flagRepository;
        this.fraudEngine = fraudEngine;
        this.clock = clock;
    }

    @Transactional
    public TransactionDto.Result ingest(TransactionDto.Request request) {
        return ingestOne(request);
    }

    /**
     * Ingest a bounded batch atomically. A duplicate reference or invalid customer causes the whole batch
     * to roll back, preventing a sender from receiving a partial-success result it cannot safely reconcile.
     */
    @Transactional
    public TransactionDto.BatchResult ingestBatch(List<TransactionDto.Request> requests) {
        Set<String> references = new HashSet<>();
        for (TransactionDto.Request request : requests) {
            if (!references.add(request.reference())) {
                throw new ConflictException("Duplicate transaction reference inside batch: " + request.reference());
            }
            if (transactionRepository.existsByReference(request.reference())) {
                throw new ConflictException("Transaction reference '" + request.reference() + "' was already received");
            }
        }

        List<TransactionDto.Result> results = requests.stream().map(this::ingestOne).toList();
        int flagged = (int) results.stream().filter(TransactionDto.Result::flagged).count();
        return new TransactionDto.BatchResult(results.size(), flagged, results);
    }

    /** Shared implementation keeps single and batch ingestion behaviour identical. */
    private TransactionDto.Result ingestOne(TransactionDto.Request request) {
        // 1. Idempotency: the same reference must not be processed twice
        if (transactionRepository.existsByReference(request.reference())) {
            throw new ConflictException("Transaction reference '" + request.reference() + "' was already received");
        }
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));

        // 2. Normalise time to UTC and reject timestamps from the future
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime occurredAt = request.occurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        if (occurredAt.isAfter(now.plusMinutes(MAX_FUTURE_MINUTES))) {
            throw new BusinessRuleException("occurredAt is in the future");
        }

        // 3. Store the transaction FIRST so the rules can count it in the customer's history
        Transaction transaction = transactionRepository.save(new Transaction(customer, request.reference(),
                request.amount(), request.merchantName().trim(), request.merchantCategory(),
                request.country(), occurredAt, now));

        // 4. Run the rules and store a flag for each one that fired
        List<RuleResult> hits = fraudEngine.evaluate(transaction);
        List<FraudFlag> flags = flagRepository.saveAll(hits.stream()
                .map(h -> new FraudFlag(transaction, h.ruleCode(), h.severity(), h.riskScore(), h.reason(), now))
                .toList());

        log.info("Transaction screened reference={} customerId={} amount={} flags={}",
                transaction.getReference(), customer.getId(), transaction.getAmount(),
                flags.stream().map(FraudFlag::getRuleCode).toList());
        return TransactionDto.Result.of(transaction, flags);
    }

    public TransactionDto.Result findById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        return TransactionDto.Result.of(transaction, flagRepository.findByTransactionIdOrderByIdAsc(id));
    }

    /** Looks up a transaction by the stable client-supplied reference. */
    public TransactionDto.Result findByReference(String reference) {
        Transaction transaction = transactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction reference", reference));
        return TransactionDto.Result.of(transaction,
                flagRepository.findByTransactionIdOrderByIdAsc(transaction.getId()));
    }

    /** Pages of transactions, optionally for one customer. */
    public PageResponse<TransactionDto.Response> search(Long customerId, Pageable pageable) {
        Page<Transaction> page = customerId == null
                ? transactionRepository.findAll(pageable)
                : transactionRepository.findByCustomerId(customerId, pageable);
        return PageResponse.from(page, TransactionDto.Response::from);
    }
}
