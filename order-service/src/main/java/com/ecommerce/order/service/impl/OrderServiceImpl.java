package com.ecommerce.order.service.impl;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.event.OrderItemEvent;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.client.CartServiceClient;
import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.client.VoucherServiceClient;
import com.ecommerce.order.dto.OrderItemRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PlaceOrderRequest;
import com.ecommerce.order.dto.ProductResponse;
import com.ecommerce.order.dto.VoucherReservationRequest;
import com.ecommerce.order.dto.VoucherReservationResult;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productClient;
    private final VoucherServiceClient voucherClient;
    private final CartServiceClient cartClient;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public OrderResponse placeOrder(String userId, String userEmail, PlaceOrderRequest request) {
        validateNoDuplicateProducts(request);

        UUID orderId = UUID.randomUUID();
        boolean voucherReserved = false;

        try {
            List<OrderItem> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;

            for (OrderItemRequest itemReq : request.getItems()) {
                ProductResponse product = fetchProduct(itemReq.getProductId());
                OrderItem item = OrderItem.builder()
                        .productId(product.getId())
                        .sku(product.getSku())
                        .productName(product.getName())
                        .price(product.getPrice())
                        .quantity(itemReq.getQuantity())
                        .subtotal(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())))
                        .build();
                items.add(item);
                subtotal = subtotal.add(item.getSubtotal());
            }

            BigDecimal discount = BigDecimal.ZERO;
            if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
                ApiResponse<VoucherReservationResult> voucherResp = voucherClient.reserve(
                        request.getVoucherCode(),
                        orderId,
                        VoucherReservationRequest.builder()
                                .userId(userId)
                                .orderTotal(subtotal)
                                .build());
                VoucherReservationResult reservation = voucherResp.getData();
                if (!voucherResp.isSuccess() || reservation == null || !reservation.isReserved()) {
                    throw new BusinessException("Voucher cannot be reserved: "
                            + (reservation != null ? reservation.getMessage() : voucherResp.getMessage()));
                }
                discount = reservation.getDiscountAmount();
                voucherReserved = true;
            }

            BigDecimal totalAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);
            Order order = Order.builder()
                    .id(orderId)
                    .userId(userId)
                    .userEmail(userEmail)
                    .status(OrderStatus.PENDING)
                    .paymentMethod(request.getPaymentMethod())
                    .subtotal(subtotal)
                    .discountAmount(discount)
                    .totalAmount(totalAmount)
                    .voucherCode(blankToNull(request.getVoucherCode()))
                    .shippingName(request.getShippingName())
                    .shippingPhone(request.getShippingPhone())
                    .shippingAddress(request.getShippingAddress())
                    .isFlashSale(false)
                    .build();
            items.forEach(item -> item.setOrder(order));
            order.setItems(items);

            Order saved = orderRepository.save(order);
            orderRepository.flush();
            saved = orderRepository.findById(saved.getId())
                    .orElseThrow(() -> new IllegalStateException("Order disappeared after save: " + orderId));
            outboxService.saveEvent("Order", saved.getId().toString(), "ORDER_CREATED",
                    OrderCreatedEvent.builder()
                            .eventId(UUID.randomUUID())
                            .orderId(saved.getId())
                            .userId(userId)
                            .items(toItemEvents(items))
                            .totalAmount(totalAmount)
                            .build());

            registerCartClearAfterCommit(userId);
            return OrderResponse.from(saved);
        } catch (RuntimeException ex) {
            releaseVoucherAfterFailedPlacement(request, orderId, voucherReserved);
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getUserOrder(String userId, UUID orderId) {
        return OrderResponse.from(orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId)));
    }

    @Override
    @Transactional(readOnly = true)
    public String getOrderStatus(String userId, UUID orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId))
                .getStatus()
                .name();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasConfirmedOrderForProduct(String userId, UUID productId) {
        return orderRepository.existsConfirmedOrderForProduct(userId, productId);
    }

    private ProductResponse fetchProduct(UUID productId) {
        ApiResponse<ProductResponse> productResp = productClient.getProduct(productId);
        if (!productResp.isSuccess() || productResp.getData() == null) {
            throw new BusinessException("Product not found: " + productId);
        }
        ProductResponse product = productResp.getData();
        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new BusinessException("Product is not available: " + product.getName());
        }
        return product;
    }

    private void validateNoDuplicateProducts(PlaceOrderRequest request) {
        List<UUID> productIds = request.getItems().stream().map(OrderItemRequest::getProductId).toList();
        Set<UUID> uniqueIds = new HashSet<>(productIds);
        if (uniqueIds.size() != productIds.size()) {
            throw new BusinessException("Duplicate products in order");
        }
    }

    private List<OrderItemEvent> toItemEvents(List<OrderItem> items) {
        return items.stream()
                .map(item -> OrderItemEvent.builder()
                        .productId(item.getProductId().toString())
                        .sku(item.getSku())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .toList();
    }

    private void registerCartClearAfterCommit(String userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    cartClient.clearCart(userId);
                } catch (Exception ex) {
                    log.warn("Failed to clear cart for user {} after order commit: {}", userId, ex.getMessage());
                }
            }
        });
    }

    private void releaseVoucherAfterFailedPlacement(PlaceOrderRequest request, UUID orderId, boolean voucherReserved) {
        if (!voucherReserved || request.getVoucherCode() == null || request.getVoucherCode().isBlank()) {
            return;
        }
        try {
            voucherClient.release(request.getVoucherCode(), orderId);
        } catch (Exception releaseEx) {
            log.error("Voucher {} reserved for order {} but release compensation failed",
                    request.getVoucherCode(), orderId, releaseEx);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
