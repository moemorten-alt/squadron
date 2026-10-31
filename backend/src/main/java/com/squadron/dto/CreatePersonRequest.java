package com.squadron.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreatePersonRequest(
        @NotBlank String name,
        String email,
        LocalDate leaverDate,
        String adminNote,
        List<Long> tagIds
) {}
