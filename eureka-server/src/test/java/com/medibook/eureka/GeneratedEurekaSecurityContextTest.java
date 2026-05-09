
package com.medibook.eureka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.medibook.eureka.config.EurekaSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;

class GeneratedEurekaSecurityContextTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void securityFilterChainConfigurationIsApplied() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        CsrfConfigurer csrf = mock(CsrfConfigurer.class);
        AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry registry =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);
        AuthorizeHttpRequestsConfigurer.AuthorizedUrl authorizedUrl =
                mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

        when(http.csrf(any())).thenAnswer(inv -> {
            ((Customizer) inv.getArgument(0)).customize(csrf);
            return http;
        });
        when(csrf.ignoringRequestMatchers("/eureka/**")).thenReturn(csrf);
        when(http.authorizeHttpRequests(any())).thenAnswer(inv -> {
            ((Customizer) inv.getArgument(0)).customize(registry);
            return http;
        });
        when(registry.anyRequest()).thenReturn(authorizedUrl);
        when(authorizedUrl.authenticated()).thenReturn(registry);
        when(http.httpBasic(any())).thenAnswer(inv -> {
            ((Customizer) inv.getArgument(0)).customize(mock(HttpBasicConfigurer.class));
            return http;
        });
        when(http.build()).thenReturn(chain);

        assertThatCode(() -> new EurekaSecurityConfig().filterChain(http)).doesNotThrowAnyException();
    }
}
