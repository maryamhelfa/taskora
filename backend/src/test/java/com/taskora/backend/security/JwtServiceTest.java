package com.taskora.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.taskora.backend.user.User;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-for-jwt-unit-tests-32chars-min";

    private User user(String email) {
        return User.builder().id(1L).email(email).firstName("Jane").lastName("Doe").build();
    }

    @Test
    void generateToken_shouldProduceTokenValidForSameUser() {
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        User user = user("jane@doe.com");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("jane@doe.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDiffersFromToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        User owner = user("jane@doe.com");
        User someoneElse = user("other@doe.com");

        String token = jwtService.generateToken(owner);

        assertThat(jwtService.isTokenValid(token, someoneElse)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        JwtService jwtService = new JwtService(SECRET, -1_000L);
        User user = user("jane@doe.com");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsMalformed() {
        JwtService jwtService = new JwtService(SECRET, 60_000L);
        User user = user("jane@doe.com");

        assertThat(jwtService.isTokenValid("not-a-valid-jwt", user)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenSignedWithDifferentSecret() {
        JwtService signer = new JwtService("another-secret-key-completely-different-32-chars-min", 60_000L);
        JwtService verifier = new JwtService(SECRET, 60_000L);
        User user = user("jane@doe.com");

        String token = signer.generateToken(user);

        assertThat(verifier.isTokenValid(token, user)).isFalse();
    }

}
