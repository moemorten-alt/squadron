package com.squadron.security;

import com.squadron.entity.AppUser;
import com.squadron.entity.UserRole;
import com.squadron.repository.AppUserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    private String adminToken;
    private String viewerToken;

    // None of these tests exercise password verification (that only happens via the
    // /api/auth/login flow, not covered here - tokens are minted directly via
    // JwtUtil), so fixture users only need to be created once per class rather than
    // re-hashed with bcrypt before every test method.
    @BeforeAll
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
    void unauthenticatedRequest_toProtectedEndpoint_returns401() throws Exception {
        // SecurityConfig installs an AuthenticationEntryPoint so that *authentication*
        // failures answer 401, leaving 403 to mean "authenticated but not permitted".
        // The frontend relies on that split to tell an expired session (sign in again)
        // apart from a VIEWER touching an admin-only action (stay signed in).
        mockMvc.perform(get("/api/persons"))
                .andExpect(status().isUnauthorized());
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
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankToken_isTreatedAsUnauthenticated_notA500() throws Exception {
        // Regression guard: jjwt throws IllegalArgumentException (not JwtException)
        // for a blank/empty token - "Authorization: Bearer " with nothing after it.
        // JwtAuthFilter must catch this too, not just JwtException.
        mockMvc.perform(get("/api/persons").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401_soTheClientKnowsToReauthenticate() throws Exception {
        // The bug this guards: an expired JWT used to surface as 403, identical to a
        // permission denial, so the frontend could not tell it had to sign the user
        // back in - it just rendered "Failed to load squads" forever.
        mockMvc.perform(get("/api/persons").header("Authorization", "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void viewerToken_onAdminOnlyWrite_stillReturns403_notAnAuthenticationFailure() throws Exception {
        // The other half of the split: a VIEWER is authenticated just fine, so hitting
        // an admin-only write must stay 403 and must NOT sign them out.
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Person\"}"))
                .andExpect(status().isForbidden());
    }

    /** A correctly signed token whose exp claim is already in the past. */
    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("admin@test.local")
                .issuedAt(new Date(now - 7_200_000))
                .expiration(new Date(now - 3_600_000))
                .signWith(key)
                .compact();
    }
}
