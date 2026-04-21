package com.medibook.provider.config;
 
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;
 
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
 
    private final GatewayJwtAuthenticationFilter gatewayJwtAuthenticationFilter;
 
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)   // disable default login
            .httpBasic(AbstractHttpConfigurer::disable)   // disable basic auth
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
 
                // ── Publicly accessible (guests can browse — PDF requirement) ──
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/providers",
                        "/api/v1/providers/**",
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/actuator/health"
                ).permitAll()
 
                // ── Everything else requires authentication ──
                .anyRequest().authenticated()
            )
            .addFilterBefore(gatewayJwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class);
 
        return http.build();
    }
}