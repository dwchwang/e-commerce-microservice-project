package com.ecommerce.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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

    @Test
    void forwardsNormalizedRealmAndResourceRolesFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("email", "admin@example.com")
                .claim("realm_access", Map.of("roles", List.of("admin")))
                .claim("resource_access", Map.of(
                        "ecommerce-client", Map.of("roles", List.of("user", "ROLE_MANAGER"))))
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/admin")
                .header("X-User-Id", "fake-user")
                .header("X-User-Roles", "ROLE_USER")
                .header("X-User-Email", "fake@example.com")
                .build());
        AtomicReference<HttpHeaders> forwardedHeaders = new AtomicReference<>();

        GatewayFilterChain chain = forwardedExchange -> {
            forwardedHeaders.set(forwardedExchange.getRequest().getHeaders());
            return Mono.empty();
        };

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(new JwtAuthenticationToken(jwt)))
                .block(Duration.ofSeconds(1));

        assertThat(forwardedHeaders.get().getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(forwardedHeaders.get().getFirst("X-User-Email")).isEqualTo("admin@example.com");
        assertThat(forwardedHeaders.get().getFirst("X-User-Roles"))
                .contains("ROLE_ADMIN")
                .contains("ROLE_USER")
                .contains("ROLE_MANAGER")
                .doesNotContain("fake");
    }
}
