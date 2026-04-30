package com.ecommerce.cart.service.impl;

import com.ecommerce.cart.client.ProductServiceClient;
import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.ProductResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.model.CartItem;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final Duration USER_CART_TTL = Duration.ofDays(7);
    private static final Duration GUEST_CART_TTL = Duration.ofMinutes(30);

    private static final String ADD_ITEM_LUA = """
            local existing = redis.call('HGET', KEYS[1], KEYS[2])
            local item
            if existing then
                item = cjson.decode(existing)
                item['quantity'] = item['quantity'] + tonumber(ARGV[1])
            else
                item = cjson.decode(ARGV[2])
            end
            redis.call('HSET', KEYS[1], KEYS[2], cjson.encode(item))
            return redis.call('TTL', KEYS[1])
            """;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductServiceClient productServiceClient;

    @Override
    public CartResponse getCart(String cartKey) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);
        List<CartItem> items = entries.values().stream()
                .map(this::readCartItem)
                .sorted(Comparator.comparing(CartItem::getAddedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        BigDecimal totalPrice = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .items(items)
                .totalPrice(totalPrice)
                .totalItems(totalItems)
                .build();
    }

    @Override
    public CartResponse addItem(String cartKey, AddToCartRequest request) {
        ProductResponse product = fetchActiveProduct(request.getProductId());
        CartItem newItem = CartItem.builder()
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .quantity(request.getQuantity())
                .addedAt(Instant.now())
                .build();

        RedisScript<Long> script = RedisScript.of(ADD_ITEM_LUA, Long.class);
        redisTemplate.execute(
                script,
                List.of(cartKey, request.getProductId().toString()),
                String.valueOf(request.getQuantity()),
                writeCartItem(newItem));
        applyTtl(cartKey);

        return getCart(cartKey);
    }

    @Override
    public CartResponse updateItem(String cartKey, UUID productId, UpdateCartItemRequest request) {
        String productKey = productId.toString();
        String itemJson = (String) redisTemplate.opsForHash().get(cartKey, productKey);
        if (itemJson == null) {
            throw new BusinessException("Product is not in cart");
        }

        CartItem item = readCartItem(itemJson);
        item.setQuantity(request.getQuantity());
        redisTemplate.opsForHash().put(cartKey, productKey, writeCartItem(item));
        applyTtl(cartKey);

        return getCart(cartKey);
    }

    @Override
    public CartResponse removeItem(String cartKey, UUID productId) {
        redisTemplate.opsForHash().delete(cartKey, productId.toString());
        applyTtlIfExists(cartKey);
        return getCart(cartKey);
    }

    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    private ProductResponse fetchActiveProduct(UUID productId) {
        ApiResponse<ProductResponse> response = productServiceClient.getProduct(productId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BusinessException("Product not found");
        }

        ProductResponse product = response.getData();
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BusinessException("Product is not available");
        }
        if (product.getPrice() == null) {
            throw new BusinessException("Product price is not available");
        }
        return product;
    }

    private void applyTtl(String cartKey) {
        redisTemplate.expire(cartKey, ttlFor(cartKey));
    }

    private void applyTtlIfExists(String cartKey) {
        Boolean exists = redisTemplate.hasKey(cartKey);
        if (Boolean.TRUE.equals(exists)) {
            applyTtl(cartKey);
        }
    }

    private Duration ttlFor(String cartKey) {
        return cartKey.startsWith("cart:guest:") ? GUEST_CART_TTL : USER_CART_TTL;
    }

    private CartItem readCartItem(Object value) {
        try {
            return objectMapper.readValue((String) value, CartItem.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cart data is corrupted");
        }
    }

    private String writeCartItem(CartItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("Cart item cannot be serialized");
        }
    }
}
