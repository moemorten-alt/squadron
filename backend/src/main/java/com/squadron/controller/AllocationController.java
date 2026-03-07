package com.squadron.controller;

import com.squadron.dto.AllocationDto;
import com.squadron.dto.CreateAllocationRequest;
import com.squadron.service.AllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;

    @GetMapping
    public ResponseEntity<List<AllocationDto>> list(Authentication auth) {
        return ResponseEntity.ok(allocationService.findAll(isAdmin(auth)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationDto> create(@Valid @RequestBody CreateAllocationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(allocationService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AllocationDto> update(@PathVariable Long id, @Valid @RequestBody CreateAllocationRequest req) {
        return ResponseEntity.ok(allocationService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        allocationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
