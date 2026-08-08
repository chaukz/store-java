package com.chaukz.store.config;

import com.chaukz.store.exception.ErrorResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: browsing the catalog needs no login
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                // Public: you can't be logged in yet when you're trying to log in
                .requestMatchers("/api/auth/**").permitAll()
                // Admin-only: everything under /api/admin/**
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Everything else just needs SOME valid login (cart, checkout, orders, addresses)
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // No token / invalid token on a protected route
                .authenticationEntryPoint((request, response, authException) ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required"))
                // Valid token, but wrong role (e.g. CUSTOMER hitting /api/admin/**)
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeError(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource"))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Writes a response in the same {message, status, timestamp} shape as
     * GlobalExceptionHandler, so 401/403 look identical to every other error
     * in the API instead of Spring Security's default blank body.
     */
    private void writeError(jakarta.servlet.http.HttpServletResponse response,
                            HttpStatus status,
                            String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        ErrorResponse body = ErrorResponse.of(message, status.value());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
