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

import java.util.Map;
import java.util.TreeSet;

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

    private String extractRoles(Jwt jwt) {
        TreeSet<String> roles = new TreeSet<>();
        addRoles(roles, jwt.getClaimAsMap("realm_access"));

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(value -> {
                if (value instanceof Map<?, ?> access) {
                    addRoles(roles, access);
                }
            });
        }

        return String.join(",", roles);
    }

    private void addRoles(TreeSet<String> roles, Map<?, ?> access) {
        if (access == null) {
            return;
        }
        Object rawRoles = access.get("roles");
        if (!(rawRoles instanceof Iterable<?> roleList)) {
            return;
        }

        roleList.forEach(role -> {
            String roleName = String.valueOf(role).trim();
            if (!roleName.isBlank()) {
                roles.add(normalizeRole(roleName));
            }
        });
    }

    private String normalizeRole(String role) {
        String normalized = role.toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
