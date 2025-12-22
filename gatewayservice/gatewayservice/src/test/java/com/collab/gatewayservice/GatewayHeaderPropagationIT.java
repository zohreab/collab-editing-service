package com.collab.gatewayservice;

import com.collab.gatewayservice.config.ModifyHeaderFilter;
import com.collab.gatewayservice.security.JwtUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;

import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {GatewayserviceApplication.class, GatewayHeaderPropagationIT.TestRoutes.class})
@AutoConfigureWebTestClient
class GatewayHeaderPropagationIT {

    private static DisposableServer downstream;
    private static int downstreamPort;
    private static final AtomicReference<String> lastXUser = new AtomicReference<>(null);

    @BeforeAll
    static void startDownstream() {
        downstream = HttpServer.create()
                .port(0) // random free port
                .route(routes -> routes.get("/docs/test", (req, res) -> {
                    lastXUser.set(req.requestHeaders().get("X-User"));
                    return res.status(200).sendString(reactor.core.publisher.Mono.just("ok"));
                }))
                .bindNow();

        downstreamPort = downstream.port();
    }

    @AfterAll
    static void stopDownstream() {
        if (downstream != null) downstream.disposeNow();
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    JwtUtils jwtUtils;

    @Test
    void gatewayAddsXUserHeaderToDownstream_whenBearerTokenPresent() {
        String token = jwtUtils.generateToken("zohreh");
        lastXUser.set(null);

        webTestClient.get()
                .uri("/docs/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");

        assertEquals("zohreh", lastXUser.get());
    }

    @Test
    void gatewayReturns401_whenTokenMissingOnProtectedRoute() {
        lastXUser.set(null);

        webTestClient.get()
                .uri("/docs/test")
                .exchange()
                .expectStatus().isUnauthorized();

        assertNull(lastXUser.get());
    }

    @TestConfiguration
    static class TestRoutes {

        // Override routes for tests to point to our in-test downstream server
        @Bean
        RouteLocator testRouteLocator(RouteLocatorBuilder builder, ModifyHeaderFilter modifyHeaderFilter) {
            String uri = "http://localhost:" + downstreamPort;

            return builder.routes()
                    .route("docservice-test", r -> r.path("/docs/**")
                            .filters(f -> f.filter(modifyHeaderFilter.apply(new ModifyHeaderFilter.Config())))
                            .uri(uri))
                    .build();
        }
    }
}
