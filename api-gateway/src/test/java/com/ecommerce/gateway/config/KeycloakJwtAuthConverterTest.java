package com.ecommerce.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthConverterTest {

    private final KeycloakJwtAuthConverter converter = new KeycloakJwtAuthConverter();

    @Test
    void mapsRealmAndResourceAccessRolesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .claim("realm_access", Map.of("roles", List.of("admin")))
                .claim("resource_access", Map.of(
                        "ecommerce-client", Map.of("roles", List.of("user", "ROLE_MANAGER"))))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt).block(Duration.ofSeconds(1));

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_ADMIN", "ROLE_USER", "ROLE_MANAGER");
    }
}
