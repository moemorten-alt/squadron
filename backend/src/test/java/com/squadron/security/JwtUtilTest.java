package com.squadron.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test for {@link JwtUtil} - no Spring context needed.
 * secret/expirationMs are normally injected via @Value from application.yml;
 * here they're set directly via reflection since JwtUtil isn't a Spring bean in this test.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 256-bit base64 key, same shape as the dev default in application.yml
    private static final String TEST_SECRET = "c3F1YWRyb25TZWNyZXRLZXlGb3JEZXZPbmx5LU5vdEZvclByb2R1Y3Rpb24h";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60_000L);
    }

    private UserDetails userDetails(String email) {
        return new User(email, "irrelevant-hash", Collections.emptyList());
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        UserDetails user = userDetails("admin@squadron.local");

        String token = jwtUtil.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("admin@squadron.local");
    }

    @Test
    void isTokenValid_returnsTrue_forMatchingUserAndUnexpiredToken() {
        UserDetails user = userDetails("admin@squadron.local");
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_whenUsernameDoesNotMatch() {
        UserDetails tokenOwner = userDetails("admin@squadron.local");
        UserDetails otherUser = userDetails("someone-else@squadron.local");
        String token = jwtUtil.generateToken(tokenOwner);

        assertThat(jwtUtil.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_whenTokenExpired() {
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", -1_000L); // already-expired token
        UserDetails user = userDetails("admin@squadron.local");
        String token = jwtUtil.generateToken(user);

        assertThat(jwtUtil.isTokenValid(token, user)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_whenTokenIsTamperedOrMalformed() {
        UserDetails user = userDetails("admin@squadron.local");
        String token = jwtUtil.generateToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtUtil.isTokenValid(tampered, user)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_whenSignedWithDifferentKey() {
        UserDetails user = userDetails("admin@squadron.local");
        String token = jwtUtil.generateToken(user);

        JwtUtil otherKeyUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherKeyUtil, "secret", "YW5vdGhlcktleUZvclRlc3RpbmdQdXJwb3Nlc09ubHkh");
        ReflectionTestUtils.setField(otherKeyUtil, "expirationMs", 60_000L);

        assertThat(otherKeyUtil.isTokenValid(token, user)).isFalse();
    }
}
