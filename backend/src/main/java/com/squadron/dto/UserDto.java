package com.squadron.dto;

import com.squadron.entity.AppUser;

public record UserDto(Long id, String email, String role, boolean active) {

    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(), u.getEmail(), u.getUserRole().name(), u.isActive());
    }
}
