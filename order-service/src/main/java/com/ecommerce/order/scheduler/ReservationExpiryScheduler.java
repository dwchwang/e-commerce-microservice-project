package com.ecommerce.order.scheduler;

import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.order.client.VoucherServiceClient;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentMethod;
import com.ecommerce.order.kafka.SagaSupport;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiryScheduler {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final VoucherServiceClient voucherClient;
    private final SagaSupport sagaSupport;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpiredReservations() {
        List<Order> expired = orderRepository.findByStatusAndPaymentMethodAndReservationExpiredAtBefore(
                OrderStatus.STOCK_RESERVED,
                PaymentMethod.VNPAY,
                LocalDateTime.now());

        for (Order order : expired) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelReason("VNPay payment not completed within 30-minute window");

            if (order.getVoucherCode() != null) {
                sagaSupport.requireSuccess(voucherClient.release(order.getVoucherCode(), order.getId()),
                        "Release voucher " + order.getVoucherCode());
            }

            outboxService.saveEvent("Order", order.getId().toString(), "ORDER_CANCELLED",
                    OrderCancelledEvent.builder()
                            .eventId(UUID.randomUUID())
                            .orderId(order.getId())
                            .userId(order.getUserId())
                            .userEmail(order.getUserEmail())
                            .reason("VNPay payment not completed within 30-minute window")
                            .items(sagaSupport.toItemEvents(order))
                            .build());

            orderRepository.save(order);
            log.info("Cancelled expired VNPay reservation for order {}", order.getId());
        }
    }
}
