package com.medibook.provider.config;
 
import com.medibook.provider.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
 
import java.io.IOException;
import java.util.List;
 
/**
 * This filter handles two scenarios:
 *
 * Scenario A — Request comes through the API Gateway:
 *   The gateway already validated the JWT and injected X-User-* headers.
 *   We read those headers and build the SecurityContext from them directly —
 *   no need to re-parse the JWT.
 *
 * Scenario B — Direct call (Postman hitting :8082 directly, bypassing gateway):
 *   We fall back to parsing the raw Bearer token from the Authorization header.
 *   This is convenient for development / testing individual services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayJwtAuthenticationFilter extends OncePerRequestFilter {
 
    private final JwtUtil jwtUtil;
 
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
 
        // ── Scenario A: headers injected by API Gateway ───────────────────
        String userEmail  = request.getHeader("X-User-Email");
        String userRole   = request.getHeader("X-User-Role");
 
        if (userEmail != null && userRole != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
 
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + userRole))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
            return;
        }
 
        // ── Scenario B: direct Bearer token (bypassing gateway) ───────────
        String authHeader = request.getHeader("Authorization");
 
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
 
                    String email = jwtUtil.extractEmail(token);
                    String role  = jwtUtil.extractRole(token);
 
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.error("JWT validation failed in provider-service: {}", e.getMessage());
            }
        }
 
        filterChain.doFilter(request, response);
    }
}