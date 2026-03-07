package com.squadron.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.squadron.entity.Person;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonDto(
        Long id,
        String name,
        String email,
        boolean active,
        List<String> tags,
        String adminNote,       // null for VIEWER role
        Integer totalAllocation,
        List<AllocationDto> allocations
) {
    public static PersonDto from(Person p, boolean includeAdminFields, List<AllocationDto> allocations, Integer totalAllocation) {
        return new PersonDto(
                p.getId(),
                p.getName(),
                p.getEmail(),
                p.isActive(),
                p.getTags().stream().map(t -> t.getName()).sorted().toList(),
                includeAdminFields ? p.getAdminNote() : null,
                totalAllocation,
                allocations
        );
    }
}
