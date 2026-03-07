package com.squadron.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull @Pattern(regexp = "ADMIN|VIEWER") String role
) {}
