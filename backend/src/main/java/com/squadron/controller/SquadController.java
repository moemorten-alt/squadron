package com.squadron.controller;

import com.squadron.dto.CreateSquadRequest;
import com.squadron.dto.SquadDto;
import com.squadron.service.SquadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;

    @GetMapping
    public ResponseEntity<List<SquadDto>> list(Authentication auth) {
        return ResponseEntity.ok(squadService.findAll(isAdmin(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SquadDto> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(squadService.findById(id, isAdmin(auth)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SquadDto> create(@Valid @RequestBody CreateSquadRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(squadService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SquadDto> update(@PathVariable Long id, @Valid @RequestBody CreateSquadRequest req) {
        return ResponseEntity.ok(squadService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        squadService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
