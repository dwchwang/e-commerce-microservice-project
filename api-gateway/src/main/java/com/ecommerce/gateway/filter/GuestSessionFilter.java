package com.ecommerce.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@Component
public class GuestSessionFilter implements GlobalFilter, Ordered {

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_.\\-]{8,128}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String sessionId = exchange.getRequest().getHeaders().getFirst("X-Session-Id");
        if (path.startsWith("/api/cart") && sessionId != null && !sessionId.isBlank() && !SESSION_ID_PATTERN.matcher(sessionId).matches()) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
