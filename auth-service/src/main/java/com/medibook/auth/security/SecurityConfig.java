package com.medibook.auth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /*
     * JwtFilter runs before every request.
     * It checks the Authorization header for valid JWT token.
     * Spring injects this automatically.
     */
    @Autowired
    private JwtFilter jwtFilter;

    /*
     * OAuth2SuccessHandler handles Google login success.
     * After Google redirects back, this runs to create/find
     * the user and issue a JWT token.
     */
    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            /*
             * Disable CSRF protection.
             * CSRF is needed for browser form based apps.
             * We use JWT tokens instead — so CSRF not needed.
             */
            .csrf(csrf -> csrf.disable())

            /*
             * Disable Spring Security's own CORS handling.
             * CORS is fully managed by the API Gateway globalcors config.
             * If we let Spring Security also set CORS headers here,
             * the browser receives two Access-Control-Allow-Origin headers
             * and rejects the response entirely.
             */
            .cors(cors -> cors.disable())

            /*
             * Disable default form login page.
             * Without this, Spring Security redirects unauthenticated
             * requests to /login and POST requests get 403.
             * We use JWT tokens — no form login needed.
             */
            .formLogin(form -> form.disable())

            /*
             * Disable HTTP Basic authentication.
             * We use JWT Bearer tokens — HTTP Basic not needed.
             */
            .httpBasic(basic -> basic.disable())

            /*
             * Stateless session management.
             * No HttpSession created on server side.
             * Every request must carry JWT token.
             * This is required for microservices architecture.
             *
             * Note: sessionFixation().none() added because
             * Spring OAuth2 briefly needs session during the
             * Google redirect state check — then drops it.
             */
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .sessionFixation().migrateSession()
            )

            /*
             * URL access rules — who can access what.
             *
             * PDF section 2.1:
             * "Guests can browse providers and view profiles
             * and available slots without logging in."
             */
            .authorizeHttpRequests(auth -> auth

                /*
                 * PUBLIC endpoints — no JWT token needed.
                 * Anyone including guests can access these.
                 * PDF explicitly requires guests to browse
                 * providers and slots without login.
                 */
                .requestMatchers(
                    "/auth/register",           // anyone can register
                    "/auth/login",              // anyone can login
                    "/auth/profile/**",         // Profile of provider/patient/admin
                    "/auth/refresh",            // anyone can refresh token
                    "/auth/admin/register",     // owner can create admin (secret code protected)
                    "/auth/google/**",          // Google OAuth2 completion endpoint
                    "/login/oauth2/**",         // Spring's internal OAuth2 redirect URI
                    "/oauth2/**",               // OAuth2 authorization requests
                    "/providers",               // guests browse providers
                    "/providers/**",            // guests view provider profiles
                    "/slots/available/**",      // guests view available slots
                    "/swagger-ui/**",           // API documentation
                    "/swagger-ui.html",         // Swagger UI page
                    "/api-docs/**",             // OpenAPI docs
                    "/v3/api-docs/**",
                    "/auth/verify-otp",
                    "/auth/resend-otp",
                    "/auth/add-phone",
                    "/auth/forgot-password",
                    "/auth/verify-reset-otp",
                    "/auth/reset-password"
                ).permitAll()

                /*
                 * PROTECTED endpoints — need valid JWT token.
                 * Any logged in user (Patient/Provider/Admin) can access.
                 *
                 * Why not restrict by role here?
                 * Role based restrictions will be added in
                 * MVC controllers (PatientController, ProviderController,
                 * AdminController) when we build the web layer.
                 * Business logic in ServiceImpl also validates access.
                 *
                 * PDF roles:
                 * /appointments/** → patients book, doctors manage
                 * /payments/**     → patients pay, admin views
                 * /reviews/**      → patients submit, admin moderates
                 * /records/**      → doctors create, patients view own
                 * /slots/**        → doctors manage their slots
                 * /admin/**        → admin platform management
                 * /notifications/**→ all users receive notifications
                 */
                .requestMatchers(
                    "/appointments/**",
                    "/payments/**",
                    "/reviews/**",
                    "/records/**",
                    "/slots/**",
                    "/admin/**",
                    "/notifications/**"
                ).authenticated()

                /*
                 * Everything else also needs authentication.
                 * Safety net for any endpoint not listed above.
                 */
                .anyRequest().authenticated()
            )

            /*
             * Google OAuth2 login configuration.
             * Spring handles the /oauth2/authorization/google redirect.
             * After Google approves, OAuth2SuccessHandler runs.
             * It finds/creates the user and issues our JWT token.
             */
            .oauth2Login(oauth -> oauth
                .successHandler(oAuth2SuccessHandler)
            )

            /*
             * Add JwtFilter before Spring's default auth filter.
             * JwtFilter runs first → reads token → sets user in context
             * Then Spring Security checks if user is authenticated.
             */
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
     * BCrypt password encoder.
     * PDF non-functional requirements explicitly state:
     * "Passwords stored as bcrypt hashes"
     * BCrypt is industry standard — even if database is hacked
     * passwords cannot be reversed from the hash.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /*
     * AuthenticationManager bean.
     * Required by Spring Security internals.
     * Used when validating credentials during login.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}