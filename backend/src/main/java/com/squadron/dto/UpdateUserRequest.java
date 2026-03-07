package com.squadron.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateUserRequest(
        String password,
        @Pattern(regexp = "ADMIN|VIEWER") String role,
        Boolean active
) {}
