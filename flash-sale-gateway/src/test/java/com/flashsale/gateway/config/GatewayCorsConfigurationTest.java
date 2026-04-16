package com.flashsale.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayCorsConfigurationTest {

    @Test
    void preflightRequestShouldExposeCorsHeadersForFrontendOrigin() {
        GatewayCorsConfiguration configuration = new GatewayCorsConfiguration();
        CorsWebFilter filter = configuration.corsWebFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("http://localhost:18080/api/users/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,x-request-id")
                        .build()
        );

        filter.filter(exchange, ignored -> Mono.empty()).block();

        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertEquals("http://localhost:5173", responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertTrue(responseHeaders.getAccessControlAllowMethods().contains(HttpMethod.POST));
        assertTrue(responseHeaders.getAccessControlAllowHeaders().contains("content-type"));
        assertTrue(responseHeaders.getAccessControlAllowHeaders().contains("x-request-id"));
    }

    @Test
    void corsConfigurationShouldAllowCredentialsForFrontendRequests() {
        GatewayCorsConfiguration configuration = new GatewayCorsConfiguration();
        CorsConfiguration corsConfiguration = configuration.corsConfiguration();

        assertEquals(Boolean.TRUE, corsConfiguration.getAllowCredentials());
        assertTrue(corsConfiguration.getAllowedOriginPatterns().contains("http://localhost:5173"));
    }
}
