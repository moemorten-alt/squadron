package com.squadron.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.squadron.entity.Allocation;

import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AllocationDto(
        Long id,
        Long personId,
        String personName,
        Long squadId,
        String squadName,
        List<String> roles,
        List<String> technologies,
        Integer allocationPercent,
        String publicComment,
        String adminNote,        // null for VIEWER role
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {
    public static AllocationDto from(Allocation a, boolean includeAdminFields) {
        return new AllocationDto(
                a.getId(),
                a.getPerson().getId(),
                a.getPerson().getName(),
                a.getSquad().getId(),
                a.getSquad().getName(),
                a.getRoles().stream().map(r -> r.getName()).sorted().toList(),
                a.getTechnologies().stream().map(t -> t.getName()).sorted().toList(),
                a.getAllocationPercent(),
                a.getPublicComment(),
                includeAdminFields ? a.getAdminNote() : null,
                a.getStartDate(),
                a.getEndDate(),
                a.isActive()
        );
    }
}
