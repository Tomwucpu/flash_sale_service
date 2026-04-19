package com.flashsale.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashsale.common.core.api.ApiResponse;
import com.flashsale.common.security.context.UserContext;
import com.flashsale.common.security.exception.UnauthorizedException;
import com.flashsale.common.security.jwt.JwtTokenService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局 JWT 认证过滤器。
 * <p>
 * 对非白名单请求执行 Bearer Token 校验，并将用户上下文透传到下游服务。
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/public/activities",
            "/api/public/activities/**",
            "/actuator/health",
            "/actuator/info"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 认证过滤主流程：
     * 1. 放行 OPTIONS 与公开路径
     * 2. 校验 Authorization 头中的 Bearer Token
     * 3. 解析令牌并写入用户上下文请求头
     * 4. 失败时统一返回 401
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // 预检请求和公开路径无需鉴权
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        // 必须提供 Bearer Token
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return writeUnauthorized(exchange, "缺少有效的 Bearer Token");
        }

        try {
            // 解析令牌并将用户身份透传给下游服务
            UserContext userContext = jwtTokenService.parseToken(authorizationHeader.substring(7));
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header(UserContext.USER_ID_HEADER, String.valueOf(userContext.userId()))
                    .header(UserContext.USERNAME_HEADER, userContext.username())
                    .header(UserContext.ROLE_HEADER, userContext.role())
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (UnauthorizedException exception) {
            // 令牌无效或过期
            return writeUnauthorized(exchange, exception.getMessage());
        }
    }

    /**
     * 过滤器顺序，数值越小越先执行。
     */
    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 判断当前请求路径是否在免鉴权白名单中。
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 返回统一的 401 未授权响应。
     */
    private Mono<Void> writeUnauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] responseBody = serialize(ApiResponse.failure("UNAUTHORIZED", message, requestId(exchange)));
        return exchange.getResponse().writeWith(Mono.just(
                exchange.getResponse().bufferFactory().wrap(responseBody)
        ));
    }

    /**
     * 获取链路请求 ID（若客户端未传递则可能为空）。
     */
    private String requestId(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("X-Request-Id");
    }

    /**
     * 序列化响应对象，失败时返回兜底 JSON。
     */
    private byte[] serialize(ApiResponse<Void> response) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(response);
        } catch (JsonProcessingException exception) {
            return "{\"code\":\"UNAUTHORIZED\",\"message\":\"unauthorized\",\"data\":null}".getBytes(StandardCharsets.UTF_8);
        }
    }
}
