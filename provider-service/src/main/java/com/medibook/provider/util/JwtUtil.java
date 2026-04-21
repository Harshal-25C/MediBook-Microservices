package com.medibook.provider.util;
 
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
 
import java.nio.charset.StandardCharsets;
import java.security.Key;
 
@Slf4j
@Component
public class JwtUtil {
 
    @Value("${medibook.jwt.secret}")
    private String jwtSecret;
 
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
 
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }
 
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }
 
    public Long extractUserId(String token) {
        Object userId = getClaims(token).get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }
 
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
 
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}