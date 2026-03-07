package com.squadron.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record CreateAllocationRequest(
        @NotNull Long personId,
        @NotNull Long squadId,
        @NotNull List<Long> roleIds,
        List<Long> technologyIds,
        @NotNull @Min(0) @Max(100) Integer allocationPercent,
        String publicComment,
        String adminNote,
        LocalDate startDate,
        LocalDate endDate
) {}
