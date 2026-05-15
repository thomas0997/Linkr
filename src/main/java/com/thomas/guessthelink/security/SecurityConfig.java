package com.thomas.guessthelink.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            // Disable Spring's built-in form login — we handle auth ourselves
            .formLogin(form -> form.disable())

            // Disable HTTP Basic Auth — stops the browser popup and the generated password
            .httpBasic(basic -> basic.disable())

            // Open everything — our AdminController handles its own session check
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            );

        return http.build();
    }
}