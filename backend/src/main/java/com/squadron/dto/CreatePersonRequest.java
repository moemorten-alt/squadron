package com.squadron.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreatePersonRequest(
        @NotBlank String name,
        String email,
        String adminNote,
        List<Long> tagIds
) {}
