package com.collab.userservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/users/register", "/users/login").permitAll()
                        .requestMatchers("/users/me", "/users/me/password").permitAll()

                        // ADD THIS LINE:
                        .requestMatchers("/users/exists/**").permitAll()

                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.disable())
                .build();
    }
}