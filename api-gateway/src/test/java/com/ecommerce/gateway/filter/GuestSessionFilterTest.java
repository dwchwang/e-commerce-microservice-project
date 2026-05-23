package com.ecommerce.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class GuestSessionFilterTest {

    private final GuestSessionFilter filter = new GuestSessionFilter();

    @Test
    void rejectsTooShortSessionIdForCartRoutes() {
        ServerWebExchange exchange = exchangeWithSession("short");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainThatMarks(chainCalled)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void rejectsSessionIdWithIllegalCharsForCartRoutes() {
        ServerWebExchange exchange = exchangeWithSession("guest with space!");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainThatMarks(chainCalled)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(chainCalled).isFalse();
    }

    @Test
    void allowsUuidV4SessionIdForCartRoutes() {
        ServerWebExchange exchange = exchangeWithSession("ca5c5566-d632-469f-90c9-8006721db86b");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainThatMarks(chainCalled)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(chainCalled).isTrue();
    }

    @Test
    void allowsOpaqueAlphanumericSessionIdForCartRoutes() {
        ServerWebExchange exchange = exchangeWithSession("guest-20260523233118");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.filter(exchange, chainThatMarks(chainCalled)).block(Duration.ofSeconds(1));

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(chainCalled).isTrue();
    }

    private ServerWebExchange exchangeWithSession(String sessionId) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/cart")
                .header("X-Session-Id", sessionId)
                .build());
    }

    private GatewayFilterChain chainThatMarks(AtomicBoolean chainCalled) {
        return exchange -> {
            chainCalled.set(true);
            return Mono.empty();
        };
    }
}
