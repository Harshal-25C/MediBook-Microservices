
package com.medibook.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.medibook.gateway.filter.JwtAuthenticationFilter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

class GeneratedGatewayFilterCoverageTest {
    private static final String SECRET = "0123456789012345678901234567890123456789012345678901234567890123";

    @Test
    void publicPathPassesThroughAndOrderIsEarly() throws Exception {
        JwtAuthenticationFilter filter = filter();
        GatewayFilterChain chain = org.mockito.Mockito.mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/auth/login").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(filter.getOrder()).isEqualTo(-1);
    }

    @Test
    void missingOrBadTokenReturnsUnauthorized() throws Exception {
        JwtAuthenticationFilter filter = filter();
        GatewayFilterChain chain = org.mockito.Mockito.mock(GatewayFilterChain.class);
        MockServerWebExchange missing = MockServerWebExchange.from(MockServerHttpRequest.get("/appointments/1").build());
        filter.filter(missing, chain).block();
        assertThat(missing.getResponse().getStatusCode().value()).isEqualTo(401);

        MockServerWebExchange bad = MockServerWebExchange.from(MockServerHttpRequest.get("/appointments/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer broken").build());
        filter.filter(bad, chain).block();
        assertThat(bad.getResponse().getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void validTokenAddsForwardHeaders() throws Exception {
        JwtAuthenticationFilter filter = filter();
        GatewayFilterChain chain = org.mockito.Mockito.mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        String token = Jwts.builder().setSubject("user@x.com").claim("userId", 5).claim("role", "Patient")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256).compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "/appointments/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(org.mockito.ArgumentMatchers.argThat(ex ->
                "5".equals(ex.getRequest().getHeaders().getFirst("X-User-Id"))
                        && "Patient".equals(ex.getRequest().getHeaders().getFirst("X-User-Role"))
                        && "user@x.com".equals(ex.getRequest().getHeaders().getFirst("X-User-Email"))));
    }

    private static JwtAuthenticationFilter filter() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        Field secret = JwtAuthenticationFilter.class.getDeclaredField("secret");
        secret.setAccessible(true);
        secret.set(filter, SECRET);
        return filter;
    }
}
