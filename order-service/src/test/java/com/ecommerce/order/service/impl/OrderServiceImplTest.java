package com.ecommerce.order.service.impl;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.client.VoucherServiceClient;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.PaymentMethod;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OutboxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductServiceClient productClient;

    @Mock
    private VoucherServiceClient voucherClient;

    @Mock
    private CartServiceClient cartClient;

    @Mock
    private OutboxService outboxService;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void placeOrderFlushesAndReloadsBeforeBuildingResponse() {
        AtomicReference<Order> savedOrder = new AtomicReference<>();
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 30, 10, 15);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 30, 10, 16);
        UUID productId = UUID.randomUUID();

        ProductResponse product = new ProductResponse();
        product.setId(productId);
        product.setSku("SKU-1");
        product.setName("Test product");
        product.setPrice(BigDecimal.valueOf(25));
        product.setIsActive(true);

        when(productClient.getProduct(productId)).thenReturn(ApiResponse.ok(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            savedOrder.set(order);
            return order;
        });
        doAnswer(invocation -> {
            savedOrder.get().setCreatedAt(createdAt);
            savedOrder.get().setUpdatedAt(updatedAt);
            return null;
        }).when(orderRepository).flush();
        when(orderRepository.findById(any(UUID.class))).thenAnswer(invocation -> Optional.of(savedOrder.get()));

        TransactionSynchronizationManager.initSynchronization();
        OrderServiceImpl service = new OrderServiceImpl(
                orderRepository,
                productClient,
                voucherClient,
                cartClient,
                outboxService);

        OrderResponse response = service.placeOrder("user-1", "user@example.com", placeOrderRequest(productId));

        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
        verify(orderRepository).flush();
        verify(orderRepository).findById(savedOrder.get().getId());
    }

    private PlaceOrderRequest placeOrderRequest(UUID productId) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(2);

        PlaceOrderRequest request = new PlaceOrderRequest();
        request.setItems(List.of(item));
        request.setPaymentMethod(PaymentMethod.COD);
        request.setShippingName("Test User");
        request.setShippingPhone("0901234567");
        request.setShippingAddress("123 Test Street");
        return request;
    }
}
