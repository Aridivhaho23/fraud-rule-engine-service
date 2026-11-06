package com.example.fraud.service;

import com.example.fraud.dto.BlacklistDto;
import com.example.fraud.entity.BlacklistMerchant;
import com.example.fraud.exception.ConflictException;
import com.example.fraud.exception.ResourceNotFoundException;
import com.example.fraud.repository.BlacklistMerchantRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manage the merchant blacklist used by {@code BlacklistMerchantRule}. */
@Service
@Transactional(readOnly = true)
public class BlacklistService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);

    private final BlacklistMerchantRepository repository;
    private final Clock clock;

    public BlacklistService(BlacklistMerchantRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Full soft-delete history is admin-only at the controller/security layer. */
    public List<BlacklistDto.HistoryResponse> findHistory() {
        return repository.findAllByOrderByAddedAtDesc().stream()
                .map(BlacklistDto.HistoryResponse::from).toList();
    }

    public List<BlacklistDto.Response> findActive() {
        return repository.findByRemovedAtIsNullOrderByMerchantNameAsc().stream()
                .map(BlacklistDto.Response::from).toList();
    }

    @Transactional
    public BlacklistDto.Response add(BlacklistDto.Request request) {
        LocalDateTime now = LocalDateTime.now(clock);
        String key = BlacklistMerchant.keyOf(request.merchantName());
        BlacklistMerchant merchant = repository.findByMerchantKey(key).map(existing -> {
            if (existing.isActive()) {
                throw new ConflictException("Merchant '" + request.merchantName() + "' is already blacklisted");
            }
            existing.reactivate(request.reason(), now); // it was removed earlier: put it back
            return existing;
        }).orElseGet(() -> repository.save(new BlacklistMerchant(request.merchantName(), request.reason(), now)));
        log.info("AUDIT blacklist add merchant='{}'", merchant.getMerchantName());
        return BlacklistDto.Response.from(merchant);
    }

    /** Full replacement of an active blacklist entry; the historical addedAt timestamp is preserved. */
    @Transactional
    public BlacklistDto.Response update(Long id, BlacklistDto.Request request) {
        BlacklistMerchant merchant = repository.findById(id)
                .filter(BlacklistMerchant::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active blacklisted merchant", id));
        String newKey = BlacklistMerchant.keyOf(request.merchantName());
        repository.findByMerchantKey(newKey).filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> { throw new ConflictException("Merchant '" + request.merchantName() + "' already has a blacklist record"); });
        merchant.update(request.merchantName(), request.reason());
        log.info("AUDIT blacklist update id={} merchant='{}'", id, merchant.getMerchantName());
        return BlacklistDto.Response.from(merchant);
    }

    /** Soft delete: the row stays for history, but the rule stops matching it. */
    @Transactional
    public void remove(Long id) {
        BlacklistMerchant merchant = repository.findById(id)
                .filter(BlacklistMerchant::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Blacklisted merchant", id));
        merchant.remove(LocalDateTime.now(clock));
        log.info("AUDIT blacklist remove merchant='{}'", merchant.getMerchantName());
    }
}
