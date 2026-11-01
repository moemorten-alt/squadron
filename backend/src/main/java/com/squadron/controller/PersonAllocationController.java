package com.squadron.controller;

import com.squadron.dto.SetPersonAllocationsRequest;
import com.squadron.dto.SetPersonAllocationsResult;
import com.squadron.service.AllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/persons/{personId}/allocations")
@RequiredArgsConstructor
public class PersonAllocationController {

    private final AllocationService allocationService;

    /** Replace a person's whole split atomically. See SetPersonAllocationsRequest. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SetPersonAllocationsResult> set(@PathVariable Long personId,
                                                          @Valid @RequestBody SetPersonAllocationsRequest req) {
        return ResponseEntity.ok(allocationService.setPersonAllocations(personId, req));
    }
}
