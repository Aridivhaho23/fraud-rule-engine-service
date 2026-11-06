package com.example.fraud.controller;

import com.example.fraud.dto.BlacklistDto;
import com.example.fraud.service.BlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manage the merchant blacklist (ADMIN to change, ANALYST to read). */
@RestController
@RequestMapping("/api/v1/blacklist")
@Tag(name = "Blacklist")
public class BlacklistController {

    private final BlacklistService blacklistService;

    public BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    /** Historical entries include soft-deleted merchants and are reserved for administrators. */
    @GetMapping("/history")
    @Operation(summary = "View all blacklist entries, including removed entries (ADMIN)")
    public List<BlacklistDto.HistoryResponse> history() {
        return blacklistService.findHistory();
    }

    @GetMapping
    @Operation(summary = "List the active blacklisted merchants")
    public List<BlacklistDto.Response> list() {
        return blacklistService.findActive();
    }

    @PostMapping
    @Operation(summary = "Blacklist a merchant (ADMIN)")
    public ResponseEntity<BlacklistDto.Response> add(@Valid @RequestBody BlacklistDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blacklistService.add(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an active blacklist entry (ADMIN)", description = "Full update of merchantName and reason. The original addedAt timestamp is preserved.")
    public BlacklistDto.Response update(@PathVariable Long id, @Valid @RequestBody BlacklistDto.Request request) {
        return blacklistService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a merchant from the blacklist (ADMIN)")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        blacklistService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
