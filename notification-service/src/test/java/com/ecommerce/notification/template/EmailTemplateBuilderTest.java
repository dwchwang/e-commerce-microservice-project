package com.ecommerce.notification.template;

import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.common.event.OrderConfirmedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateBuilderTest {

    private final EmailTemplateBuilder builder = new EmailTemplateBuilder();

    @Test
    void buildsVietnameseUtf8OrderSubjectsAndBodies() {
        UUID orderId = UUID.randomUUID();

        assertThat(builder.confirmedSubject(OrderConfirmedEvent.builder().orderId(orderId).build()))
                .isEqualTo("Đơn hàng " + orderId + " đã được xác nhận");
        assertThat(builder.cancelledBody(OrderCancelledEvent.builder().orderId(orderId).reason(null).build()))
                .contains("Đơn hàng " + orderId + " đã bị hủy")
                .contains("Không xác định");
    }
}
