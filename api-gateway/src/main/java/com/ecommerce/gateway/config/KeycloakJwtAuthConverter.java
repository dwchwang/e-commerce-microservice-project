package com.ecommerce.gateway.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KeycloakJwtAuthConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(defaultConverter.convert(jwt));

        keycloakRoles(jwt).stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    }

    private List<String> keycloakRoles(Jwt jwt) {
        List<String> roles = new ArrayList<>();
        addRoles(roles, jwt.getClaimAsMap("realm_access"));

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(value -> {
                if (value instanceof Map<?, ?> access) {
                    addRoles(roles, access);
                }
            });
        }

        return roles;
    }

    private void addRoles(List<String> roles, Map<?, ?> access) {
        if (access == null) {
            return;
        }
        Object rawRoles = access.get("roles");
        if (!(rawRoles instanceof Collection<?> roleCollection)) {
            return;
        }

        roleCollection.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(this::normalizeRole)
                .forEach(roles::add);
    }

    private String normalizeRole(String role) {
        String normalized = role.toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
