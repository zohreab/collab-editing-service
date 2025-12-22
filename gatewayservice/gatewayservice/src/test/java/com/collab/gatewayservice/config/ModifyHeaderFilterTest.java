package com.collab.gatewayservice.config;

import com.collab.gatewayservice.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModifyHeaderFilterTest {

    @Test
    void openEndpoint_login_skipsAuthAndDoesNotRequireToken() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        ModifyHeaderFilter factory = new ModifyHeaderFilter(jwtUtils);
        GatewayFilter filter = factory.apply(new ModifyHeaderFilter.Config());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/users/login")
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        );

        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        // validateAndGetUsername should never be called because /users/login is open
        verify(jwtUtils, never()).validateAndGetUsername(anyString());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        ModifyHeaderFilter factory = new ModifyHeaderFilter(jwtUtils);
        GatewayFilter filter = factory.apply(new ModifyHeaderFilter.Config());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/docs/123")
        );

        // chain should never be reached
        GatewayFilterChain chain = ex -> {
            fail("Chain should not be called when token is missing");
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        MockServerHttpResponse resp = exchange.getResponse();
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void protectedEndpoint_withBearerToken_addsXUserHeader() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.validateAndGetUsername("GOODTOKEN")).thenReturn("zohreh");

        ModifyHeaderFilter factory = new ModifyHeaderFilter(jwtUtils);
        GatewayFilter filter = factory.apply(new ModifyHeaderFilter.Config());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/docs/123")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer GOODTOKEN")
        );

        GatewayFilterChain chain = ex -> {
            ServerHttpRequest req = ex.getRequest();
            assertEquals("zohreh", req.getHeaders().getFirst("X-User"));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        verify(jwtUtils).validateAndGetUsername("GOODTOKEN");
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.validateAndGetUsername("BADTOKEN")).thenThrow(new RuntimeException("bad token"));

        ModifyHeaderFilter factory = new ModifyHeaderFilter(jwtUtils);
        GatewayFilter filter = factory.apply(new ModifyHeaderFilter.Config());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/docs/123")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer BADTOKEN")
        );

        GatewayFilterChain chain = ex -> {
            fail("Chain should not be called when token is invalid");
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void websocketStyleTokenQueryParam_addsXUserHeader() {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.validateAndGetUsername("QPARAMTOKEN")).thenReturn("arya");

        ModifyHeaderFilter factory = new ModifyHeaderFilter(jwtUtils);
        GatewayFilter filter = factory.apply(new ModifyHeaderFilter.Config());

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/ws-docs/someRoom?token=QPARAMTOKEN")
        );

        GatewayFilterChain chain = ex -> {
            assertEquals("arya", ex.getRequest().getHeaders().getFirst("X-User"));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
    }
}
