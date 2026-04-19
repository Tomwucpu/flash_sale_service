package com.flashsale.gateway.security;

import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.jwt.JwtProperties;
import com.flashsale.common.security.jwt.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setJwtSecret("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenTtl(Duration.ofHours(2));
        tokenService = new JwtTokenService(properties, Clock.fixed(Instant.parse("2026-04-14T10:00:00Z"), ZoneOffset.UTC));
        filter = new JwtAuthenticationFilter(tokenService);
    }

    @Test
    void publicEndpointBypassesAuthorizationCheck() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/users/login").build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, successChain(chainInvoked, new AtomicReference<>()))
                .block();

        assertTrue(chainInvoked.get());
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void publicActivitiesEndpointBypassesAuthorizationCheck() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/activities").build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, successChain(chainInvoked, new AtomicReference<>()))
                .block();

        assertTrue(chainInvoked.get());
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedEndpointRejectsMissingBearerToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/activities").build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, successChain(chainInvoked, new AtomicReference<>()))
                .block();

        assertFalse(chainInvoked.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void preflightRequestBypassesAuthorizationCheck() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/activities").build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, successChain(chainInvoked, new AtomicReference<>()))
                .block();

        assertTrue(chainInvoked.get());
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void validTokenAddsCanonicalHeadersBeforeForwarding() {
        String token = tokenService.generateToken(new UserContext(9001L, "gateway-user", "ADMIN"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/activities")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        filter.filter(exchange, successChain(chainInvoked, forwardedRequest))
                .block();

        assertTrue(chainInvoked.get());
        assertEquals("9001", forwardedRequest.get().getHeaders().getFirst(UserContext.USER_ID_HEADER));
        assertEquals("gateway-user", forwardedRequest.get().getHeaders().getFirst(UserContext.USERNAME_HEADER));
        assertEquals("ADMIN", forwardedRequest.get().getHeaders().getFirst(UserContext.ROLE_HEADER));
    }

    private GatewayFilterChain successChain(AtomicBoolean chainInvoked, AtomicReference<ServerHttpRequest> forwardedRequest) {
        return exchange -> {
            chainInvoked.set(true);
            forwardedRequest.set(exchange.getRequest());
            if (exchange.getResponse().getStatusCode() == null) {
                exchange.getResponse().setStatusCode(HttpStatus.OK);
            }
            return Mono.empty();
        };
    }
}
