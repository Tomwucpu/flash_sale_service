package com.flashsale.common.security.jwt;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

public class JwtTokenService {

    private final JwtProperties jwtProperties;

    private final Clock clock;

    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserContext userContext) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(jwtProperties.getAccessTokenTtl());

        return Jwts.builder()
                .subject(String.valueOf(userContext.userId()))
                .claim("username", userContext.username())
                .claim("role", userContext.role())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public UserContext parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new UserContext(
                    Long.parseLong(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", String.class)
            );
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("无效或已过期的访问令牌", exception);
        }
    }
}
