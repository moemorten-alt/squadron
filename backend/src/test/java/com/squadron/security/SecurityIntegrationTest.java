package com.squadron.security;

import com.squadron.entity.AppUser;
import com.squadron.entity.UserRole;
import com.squadron.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration-style test proving the SecurityConfig / JwtAuthFilter / @PreAuthorize
 * chain actually enforces authn/authz end-to-end, using real JWTs minted by JwtUtil
 * (not @WithMockUser, which would bypass the filter entirely).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();

        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("admin@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());
        AppUser viewer = appUserRepository.save(AppUser.builder()
                .email("viewer@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.VIEWER)
                .active(true)
                .build());

        adminToken = jwtUtil.generateToken(new User(admin.getEmail(), admin.getPasswordHash(),
                List.of(() -> "ROLE_ADMIN")));
        viewerToken = jwtUtil.generateToken(new User(viewer.getEmail(), viewer.getPasswordHash(),
                Collections.emptyList()));
    }

    @Test
    void unauthenticatedRequest_toProtectedEndpoint_isRejected() throws Exception {
        // No custom AuthenticationEntryPoint is configured, so Spring Security's
        // default falls back to 403 (not 401) for anonymous requests too. This
        // test pins that actual behavior down as a regression guard.
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedRequest_withValidToken_isAllowedThrough() throws Exception {
        mockMvc.perform(get("/api/persons").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    void viewerToken_onAdminOnlyWrite_returns403() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Person\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminToken_onAdminOnlyWrite_isAllowedThrough() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Person\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void adminOnlyUsersRouter_rejectsNonAdmin() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedOrTamperedToken_isTreatedAsUnauthenticated_notA500() throws Exception {
        // Regression guard: JwtAuthFilter previously let JwtException propagate
        // uncaught, turning any bad Authorization header into a 500 instead of
        // a clean auth rejection.
        mockMvc.perform(get("/api/persons").header("Authorization", "Bearer " + adminToken + "tampered"))
                .andExpect(status().isForbidden());
    }
}
