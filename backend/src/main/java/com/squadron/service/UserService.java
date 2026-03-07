package com.squadron.service;

import com.squadron.dto.CreateUserRequest;
import com.squadron.dto.UpdateUserRequest;
import com.squadron.dto.UserDto;
import com.squadron.entity.AppUser;
import com.squadron.entity.UserRole;
import com.squadron.exception.ResourceNotFoundException;
import com.squadron.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return appUserRepository.findAll().stream()
                .map(UserDto::from)
                .toList();
    }

    @Transactional
    public UserDto create(CreateUserRequest req) {
        if (appUserRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already in use: " + req.email());
        }
        AppUser user = AppUser.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .userRole(UserRole.valueOf(req.role()))
                .active(true)
                .build();
        appUserRepository.save(user);
        return UserDto.from(user);
    }

    @Transactional
    public UserDto update(Long id, UpdateUserRequest req) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (req.role() != null) {
            user.setUserRole(UserRole.valueOf(req.role()));
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        if (req.active() != null) {
            user.setActive(req.active());
        }
        appUserRepository.save(user);
        return UserDto.from(user);
    }

    @Transactional
    public void delete(Long id) {
        AppUser user = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        appUserRepository.delete(user);
    }
}
