package com.ecommerce.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null && !userId.isBlank()) {
                return Mono.just(userId);
            }
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return Mono.just(forwardedFor.split(",")[0].trim());
            }
            return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                    .map(address -> address.getAddress().getHostAddress())
                    .defaultIfEmpty("anonymous");
        };
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // Trust X-Forwarded-For only when Gateway is behind a controlled reverse proxy.
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return Mono.just(forwardedFor.split(",")[0].trim());
            }
            return Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                    .map(address -> address.getAddress().getHostAddress())
                    .defaultIfEmpty("anonymous");
        };
    }
}
