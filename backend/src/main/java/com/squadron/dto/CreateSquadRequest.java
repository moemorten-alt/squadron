package com.squadron.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSquadRequest(
        @NotBlank String name,
        String description
) {}
