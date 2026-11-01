package com.squadron.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * A person's complete split. Squads listed are updated or created; any active
 * allocation in a squad NOT listed is removed. dryRun reports what would
 * happen without saving.
 */
public record SetPersonAllocationsRequest(
        @NotEmpty List<@Valid Item> items,
        boolean dryRun
) {
    /**
     * roleIds / technologyIds are optional. Omitted on an existing allocation
     * they are kept; omitted on a new one they are copied from the person's
     * largest current allocation.
     */
    public record Item(
            @NotNull Long squadId,
            @NotNull @Min(1) @Max(100) Integer allocationPercent,
            List<Long> roleIds,
            List<Long> technologyIds
    ) {}
}
