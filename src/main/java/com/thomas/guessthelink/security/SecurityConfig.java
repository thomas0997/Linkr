package com.thomas.guessthelink.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Admin setup page is public — needed to do the first scan
                .requestMatchers("/admin/setup", "/admin/login").permitAll()
                // Everything else under /admin requires being logged in as admin
                .requestMatchers("/admin/**").authenticated()
                // All game routes are public
                .anyRequest().permitAll()
            )
            // When an unauthenticated user hits /admin, send them to /admin/login
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login-process")
                .defaultSuccessUrl("/admin", true)
                .failureUrl("/admin/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
                .permitAll()
            );

        return http.build();
    }
}