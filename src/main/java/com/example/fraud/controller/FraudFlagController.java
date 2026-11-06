package com.example.fraud.controller;

import com.example.fraud.dto.FlagDto;
import com.example.fraud.dto.PageResponse;
import com.example.fraud.entity.FlagStatus;
import com.example.fraud.entity.Severity;
import com.example.fraud.service.FraudFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Analyst-facing API: find fraud flags and record review decisions. */
@RestController
@RequestMapping("/api/v1/flags")
@Tag(name = "Fraud flags", description = "Search flags and review them")
public class FraudFlagController {

    private final FraudFlagService flagService;

    public FraudFlagController(FraudFlagService flagService) {
        this.flagService = flagService;
    }

    @GetMapping
    @Operation(summary = "Search flags. Every filter is optional; filters combine with AND. Paged, newest first.")
    public PageResponse<FlagDto.Response> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long transactionId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) FlagStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 20, sort = {"flaggedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return flagService.search(customerId, transactionId, ruleCode, severity, status, from, to, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one flag")
    public FlagDto.Response get(@PathVariable Long id) {
        return flagService.findById(id);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Audit trail of status changes for a flag")
    public List<FlagDto.HistoryEntry> history(@PathVariable Long id) {
        return flagService.history(id);
    }

    /**
     * PATCH = change PART of a resource (here only the status). The reviewer name is taken from the
     * authenticated user, NOT from the request body, so it cannot be forged.
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change the review status of a flag (OPEN -> UNDER_REVIEW -> CONFIRMED_FRAUD / FALSE_POSITIVE)")
    public FlagDto.Response updateStatus(@PathVariable Long id,
                                         @Valid @RequestBody FlagDto.StatusUpdateRequest request,
                                         Authentication authentication) {
        return flagService.updateStatus(id, request, authentication.getName());
    }
}
