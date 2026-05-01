package com.ecommerce.notification.service.impl;

import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.common.event.OrderConfirmedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationServiceImplTest {

    @Test
    void orderNotificationHandlersAreTransactionalButPreserveFailureAudit() throws NoSuchMethodException {
        assertNoRollbackForRuntime(NotificationServiceImpl.class
                .getMethod("sendOrderConfirmed", OrderConfirmedEvent.class)
                .getAnnotation(Transactional.class));
        assertNoRollbackForRuntime(NotificationServiceImpl.class
                .getMethod("sendOrderCancelled", OrderCancelledEvent.class)
                .getAnnotation(Transactional.class));
    }

    private void assertNoRollbackForRuntime(Transactional transactional) {
        assertThat(transactional).isNotNull();
        assertThat(Arrays.asList(transactional.noRollbackFor())).contains(RuntimeException.class);
    }
}
