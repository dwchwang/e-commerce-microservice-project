package com.ecommerce.payment.scheduler;

import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentTimeoutScheduler {

    private final PaymentService paymentService;

    @Scheduled(fixedDelay = 60_000)
    public void timeoutExpiredPayments() {
        paymentService.timeoutExpiredPayments();
    }
}
