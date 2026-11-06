package com.example.fraud.service;

import com.example.fraud.dto.FlagDto;
import com.example.fraud.dto.PageResponse;
import com.example.fraud.entity.FlagStatus;
import com.example.fraud.entity.FlagStatusChange;
import com.example.fraud.entity.FraudFlag;
import com.example.fraud.entity.Severity;
import com.example.fraud.exception.ResourceNotFoundException;
import com.example.fraud.repository.FlagSpecifications;
import com.example.fraud.repository.FlagStatusChangeRepository;
import com.example.fraud.repository.FraudFlagRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Analyst use cases: search flags, read one, review (change status) and view the audit trail. */
@Service
@Transactional(readOnly = true)
public class FraudFlagService {

    private static final Logger log = LoggerFactory.getLogger(FraudFlagService.class);

    private final FraudFlagRepository flagRepository;
    private final FlagStatusChangeRepository historyRepository;
    private final Clock clock;

    public FraudFlagService(FraudFlagRepository flagRepository, FlagStatusChangeRepository historyRepository,
                            Clock clock) {
        this.flagRepository = flagRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    /** Filters are all optional and combine with AND. Newest flags are returned first by default. */
    public PageResponse<FlagDto.Response> search(Long customerId, Long transactionId, String ruleCode,
                                                 Severity severity, FlagStatus status,
                                                 OffsetDateTime from, OffsetDateTime to, Pageable pageable) {
        Specification<FraudFlag> spec = (root, query, cb) -> cb.conjunction(); // "no restriction" starting point
        spec = spec.and(FlagSpecifications.customerId(customerId))
                .and(FlagSpecifications.transactionId(transactionId))
                .and(FlagSpecifications.ruleCode(ruleCode))
                .and(FlagSpecifications.severity(severity))
                .and(FlagSpecifications.status(status))
                .and(FlagSpecifications.flaggedFrom(toUtc(from)))
                .and(FlagSpecifications.flaggedTo(toUtc(to)));
        return PageResponse.from(flagRepository.findAll(spec, pageable), FlagDto.Response::from);
    }

    public FlagDto.Response findById(Long id) {
        return FlagDto.Response.from(getOrThrow(id));
    }

    public List<FlagDto.HistoryEntry> history(Long id) {
        getOrThrow(id); // 404 if the flag does not exist
        return historyRepository.findByFlagIdOrderByIdAsc(id).stream().map(FlagDto.HistoryEntry::from).toList();
    }

    /**
     * Analyst decision. The flag enforces the allowed transitions; here we also write the audit trail.
     *
     * @param reviewer the authenticated username (taken from the security context by the controller)
     */
    @Transactional
    public FlagDto.Response updateStatus(Long id, FlagDto.StatusUpdateRequest request, String reviewer) {
        FraudFlag flag = getOrThrow(id);
        FlagStatus from = flag.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);

        flag.transitionTo(request.status(), reviewer, request.note(), now); // throws 422 if not allowed
        historyRepository.save(new FlagStatusChange(flag, from, request.status(), reviewer, now, request.note()));

        // Dirty checking writes the UPDATE when the transaction commits - no explicit save() needed.
        log.info("AUDIT flag={} status {} -> {} reviewer={}", id, from, request.status(), reviewer);
        return FlagDto.Response.from(flag);
    }

    private FraudFlag getOrThrow(Long id) {
        return flagRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Flag", id));
    }

    private static LocalDateTime toUtc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
