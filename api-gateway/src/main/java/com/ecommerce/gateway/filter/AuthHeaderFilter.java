package com.ecommerce.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class AuthHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Always strip client-supplied identity headers first to prevent spoofing
        ServerWebExchange stripped = stripIdentityHeaders(exchange);
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication())
                .filter(authentication -> authentication != null && authentication.getPrincipal() instanceof Jwt)
                .map(authentication -> (Jwt) authentication.getPrincipal())
                .map(jwt -> withIdentityHeaders(stripped, jwt))
                .defaultIfEmpty(stripped)
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return 1;
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Roles");
                    headers.remove("X-User-Email");
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange withIdentityHeaders(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.add("X-User-Id", jwt.getSubject());
                    headers.add("X-User-Roles", extractRoles(jwt));
                    headers.add("X-User-Email", extractEmail(jwt));
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private String extractEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        String username = jwt.getClaimAsString("preferred_username");
        return username != null && !username.isBlank() ? username : jwt.getSubject();
    }

    @SuppressWarnings("unchecked")
    private String extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return "";
        }
        Object roles = realmAccess.get("roles");
        if (!(roles instanceof List<?> roleList)) {
            return "";
        }
        return String.join(",", (List<String>) roleList);
    }
}
