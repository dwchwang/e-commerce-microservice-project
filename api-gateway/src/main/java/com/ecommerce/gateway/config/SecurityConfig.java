package com.ecommerce.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${gateway.cors.allowed-origins:http://localhost:3000}")
    private String allowedOriginsCsv;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(auth -> auth
                        // --- Admin-only routes (checked first) ---
                        .pathMatchers("/api/orders/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/users/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/inventory/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers("/api/reviews/admin/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/vouchers/active").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/vouchers", "/api/vouchers/*").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/vouchers/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/vouchers/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/vouchers/**").hasAuthority("ROLE_ADMIN")
                        // Product mutations: admin-only
                        .pathMatchers(HttpMethod.POST, "/api/products/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/products/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/products/**").hasAuthority("ROLE_ADMIN")
                        // Flash-sale mutations: admin-only
                        .pathMatchers(HttpMethod.POST, "/api/flash-sales/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/flash-sales/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/flash-sales/**").hasAuthority("ROLE_ADMIN")
                        // Content mutations: admin-only (banners, CMS pages)
                        .pathMatchers(HttpMethod.POST, "/api/content/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/content/**").hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/content/**").hasAuthority("ROLE_ADMIN")
                        // Inventory stock adjustments: admin-only
                        .pathMatchers("/api/inventory/stock-in", "/api/inventory/stock-out").hasAuthority("ROLE_ADMIN")
                        // --- Public endpoints (storefront) ---
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/content/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/flash-sales/**").permitAll()
                        .pathMatchers("/api/cart", "/api/cart/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/payments/vnpay/ipn").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/payments/vnpay/return").permitAll()
                        .pathMatchers("/eureka/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakJwtAuthConverter())))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Session-Id"));
        configuration.setExposedHeaders(List.of("X-User-Id", "X-User-Roles", "X-User-Email"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
