package com.ecommerce.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterConfigTest {

    private final RateLimiterConfig config = new RateLimiterConfig();

    @Test
    void userKeyResolverPrefersUserIdHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header("X-User-Id", "user-123")
                .header("X-Forwarded-For", "203.0.113.10")
                .remoteAddress(new InetSocketAddress("192.0.2.15", 5150))
                .build());

        String key = config.userKeyResolver().resolve(exchange).block(Duration.ofSeconds(1));

        assertThat(key).isEqualTo("user-123");
    }

    @Test
    void userKeyResolverFallsBackToForwardedFor() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                .remoteAddress(new InetSocketAddress("192.0.2.15", 5150))
                .build());

        String key = config.userKeyResolver().resolve(exchange).block(Duration.ofSeconds(1));

        assertThat(key).isEqualTo("203.0.113.10");
    }

    @Test
    void userKeyResolverFallsBackToRemoteAddress() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/orders")
                .remoteAddress(new InetSocketAddress("192.0.2.15", 5150))
                .build());

        String key = config.userKeyResolver().resolve(exchange).block(Duration.ofSeconds(1));

        assertThat(key).isEqualTo("192.0.2.15");
    }

    @Test
    void userKeyResolverFallsBackToAnonymousWhenNoAddressAvailable() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders").build());

        String key = config.userKeyResolver().resolve(exchange).block(Duration.ofSeconds(1));

        assertThat(key).isEqualTo("anonymous");
    }

    @Test
    void ipKeyResolverPrefersFirstForwardedForAddress() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login")
                .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                .build());

        String key = config.ipKeyResolver().resolve(exchange).block(Duration.ofSeconds(1));

        assertThat(key).isEqualTo("203.0.113.10");
    }

    @Test
    void ipKeyResolverFallsBackToRemoteAddress() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login")
                .remoteAddress(new InetSocketAddress("192.0.2.15", 5150))
                .build());

        String key = config.ipKeyResolver().resolve(exchange).block(Duration.ofSeconds(1));

        assertThat(key).isEqualTo("192.0.2.15");
    }
}
