package com.flashsale.common.security.jwt;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void generateTokenRoundTripsUserContext() {
        JwtTokenService tokenService = new JwtTokenService(defaultProperties(), fixedClock("2026-04-14T10:00:00Z"));
        UserContext expected = new UserContext(1001L, "operator", "ADMIN");

        String token = tokenService.generateToken(expected);
        UserContext actual = tokenService.parseToken(token);

        assertTrue(token.startsWith("eyJ"));
        assertEquals(expected, actual);
    }

    @Test
    void parseTokenRejectsExpiredToken() {
        JwtProperties properties = defaultProperties();
        properties.setAccessTokenTtl(Duration.ofMinutes(5));
        JwtTokenService issuedService = new JwtTokenService(properties, fixedClock("2026-04-14T10:00:00Z"));
        JwtTokenService expiredService = new JwtTokenService(properties, fixedClock("2026-04-14T10:06:00Z"));

        String token = issuedService.generateToken(new UserContext(2001L, "buyer", "USER"));

        assertThrows(UnauthorizedException.class, () -> expiredService.parseToken(token));
    }

    @Test
    void parseTokenRejectsTamperedToken() {
        JwtTokenService tokenService = new JwtTokenService(defaultProperties(), fixedClock("2026-04-14T10:00:00Z"));
        String token = tokenService.generateToken(new UserContext(3001L, "publisher", "PUBLISHER"));

        assertThrows(UnauthorizedException.class, () -> tokenService.parseToken(token + "broken"));
    }

    private static JwtProperties defaultProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setJwtSecret(SECRET);
        properties.setAccessTokenTtl(Duration.ofHours(2));
        return properties;
    }

    private static Clock fixedClock(String isoInstant) {
        return Clock.fixed(Instant.parse(isoInstant), ZoneOffset.UTC);
    }
}
