package com.medibook.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

/**
 * Global JWT filter running on every request through the gateway.
 *
 * PUBLIC routes (no token needed) — same as monolith SecurityConfig:
 *   POST /auth/register, /auth/login, /auth/verify-otp, /auth/resend-otp,
 *   /auth/add-phone, /auth/forgot-password, /auth/verify-reset-otp,
 *   /auth/reset-password, /auth/admin/register, /auth/google/complete,
 *   GET  /providers/**, GET /slots/available/**
 *   OAuth2 redirect: /oauth2/**, /login/oauth2/**
 *
 * PROTECTED routes — need valid Bearer token:
 *   /appointments/**, /payments/**, /reviews/**, /records/**,
 *   /slots/** (non-available), /notifications/**
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    // ── All paths that are completely PUBLIC (no token needed) ─────────
    private static final List<String> PUBLIC_PATHS = List.of(
        "/auth/register",
        "/auth/login",
        "/auth/verify-otp",
        "/auth/resend-otp",
        "/auth/add-phone",
        "/auth/forgot-password",
        "/auth/verify-reset-otp",
        "/auth/reset-password",
        "/auth/admin/register",
        "/auth/google/complete",
        "/auth/refresh"
    );

    private static final List<String> PUBLIC_PREFIXES = List.of(
        "/oauth2/",
        "/login/oauth2/",
        "/providers/",       // guests can browse doctors
        "/slots/available/"  // guests can view available slots
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        
        // Allow CORS preflight requests
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // Allow all public paths without token
        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        // Check Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = parseToken(token);
            // Forward userId and role as headers to downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(claims.get("userId")))
                .header("X-User-Role", (String) claims.get("role"))
                .header("X-User-Email", claims.getSubject())
                .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublic(String path, HttpMethod method) {
        // Exact match for public POST paths
        for (String pub : PUBLIC_PATHS) {
            if (path.equals(pub)) return true;
        }
        // Prefix match
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        // GET /providers (root) is also public
        if (path.equals("/providers") && HttpMethod.GET.equals(method)) return true;
        return false;
    }

    private Claims parseToken(String token) {
        Key key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    @Override
    public int getOrder() {
        return -1; // Run before all other filters
    }
}
