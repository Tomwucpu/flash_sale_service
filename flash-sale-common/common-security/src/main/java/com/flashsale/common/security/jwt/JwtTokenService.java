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

/**
 * JWT 令牌服务，负责访问令牌的签发与解析。
 */
public class JwtTokenService {

    private final JwtProperties jwtProperties;

    private final Clock clock;

    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        // 基于配置中的密钥构建 HMAC 签名 Key
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 根据当前登录用户上下文生成访问令牌。
     *
     * @param userContext 登录用户信息
     * @return JWT 访问令牌
     */
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

    /**
     * 解析并校验访问令牌，提取用户上下文信息。
     *
     * @param token JWT 访问令牌
     * @return 用户上下文
     * @throws UnauthorizedException 令牌无效、签名错误或已过期时抛出
     */
    public UserContext parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    // 使用统一时钟，便于测试场景下控制“当前时间”
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
            // 统一转换为业务层可识别的未授权异常
            throw new UnauthorizedException("无效或已过期的访问令牌", exception);
        }
    }
}
