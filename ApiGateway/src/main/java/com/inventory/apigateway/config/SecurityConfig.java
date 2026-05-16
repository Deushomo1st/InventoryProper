package com.inventory.apigateway.config;

import com.inventory.apigateway.filter.JwtAuthFilter;
import com.inventory.apigateway.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — JWT in Authorization header makes it unnecessary
                .csrf(csrf -> csrf.disable())

                // Disable HTTP Basic and Form Login to prevent "generated password" warning
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())

                // Stateless — no HttpSession, no server-side state
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Route rules
                .authorizeHttpRequests(auth -> auth
                        // Public — anyone can reach these
                        .requestMatchers(
                                "/login.html",
                                "/menu.html",
                                "/css/**",
                                "/js/**",
                                "/auth/login",
                                "/auth/register",
                                "/health/**",
                                "/ui/**"
                        ).permitAll()

                        // All other API routes — require authentication
                        .anyRequest().authenticated()
                )

                // Insert our JWT filter before Spring's standard filter
                .addFilterBefore(
                        new JwtAuthFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}