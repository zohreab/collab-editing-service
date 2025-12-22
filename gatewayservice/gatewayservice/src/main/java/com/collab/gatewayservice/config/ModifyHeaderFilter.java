package com.collab.gatewayservice.config;

import com.collab.gatewayservice.security.JwtUtils; // The utility you just copied
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class ModifyHeaderFilter extends AbstractGatewayFilterFactory<ModifyHeaderFilter.Config> {

    private final JwtUtils jwtUtils;

    public ModifyHeaderFilter(JwtUtils jwtUtils) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // 1. Skip validation for open endpoints
            if (path.contains("/users/login") || path.contains("/users/register")) {
                return chain.filter(exchange);
            }

            // 2. Try to get token from Header OR Query Parameter
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            String token = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else {
                // Check query param (for WebSockets)
                token = exchange.getRequest().getQueryParams().getFirst("token");
            }

            if (token != null) {
                try {
                    String username = jwtUtils.validateAndGetUsername(token);

                    // Add X-User header for the downstream service (DocService)
                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-User", username)
                            .build();

                    return chain.filter(exchange.mutate().request(request).build());
                } catch (Exception e) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
            }

            // No token found
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }
    public static class Config {}
}