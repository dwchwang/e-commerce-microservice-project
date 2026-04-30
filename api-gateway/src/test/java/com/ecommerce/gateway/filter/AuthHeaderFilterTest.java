package com.ecommerce.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthHeaderFilterTest {

    private final AuthHeaderFilter filter = new AuthHeaderFilter();

    @Test
    void stripsClientSuppliedIdentityHeadersWhenRequestIsUnauthenticated() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/cart")
                .header("X-User-Id", "fake-user")
                .header("X-User-Roles", "ROLE_ADMIN")
                .header("X-User-Email", "fake@example.com")
                .build());
        AtomicReference<HttpHeaders> forwardedHeaders = new AtomicReference<>();

        GatewayFilterChain chain = forwardedExchange -> {
            forwardedHeaders.set(forwardedExchange.getRequest().getHeaders());
            return Mono.empty();
        };

        filter.filter(exchange, chain).block(Duration.ofSeconds(1));

        assertThat(forwardedHeaders.get()).doesNotContainKeys("X-User-Id", "X-User-Roles", "X-User-Email");
    }
}
