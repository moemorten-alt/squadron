package com.squadron.dto;

import com.squadron.entity.Squad;

import java.util.List;

public record SquadDto(
        Long id,
        String name,
        String description,
        List<AllocationDto> allocations,
        Integer totalHeadcount,
        Integer totalAllocationPercent
) {
    public static SquadDto from(Squad s, List<AllocationDto> allocations) {
        int totalPct = allocations.stream().mapToInt(AllocationDto::allocationPercent).sum();
        return new SquadDto(
                s.getId(),
                s.getName(),
                s.getDescription(),
                allocations,
                allocations.size(),
                totalPct
        );
    }
}
