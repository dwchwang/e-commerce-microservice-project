package com.ecommerce.notification.template;

import com.ecommerce.common.event.OrderCancelledEvent;
import com.ecommerce.common.event.OrderConfirmedEvent;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String confirmedSubject(OrderConfirmedEvent event) {
        return "Đơn hàng " + event.getOrderId() + " đã được xác nhận";
    }

    public String confirmedBody(OrderConfirmedEvent event) {
        return "Đơn hàng " + event.getOrderId()
                + " đã được xác nhận. Chúng tôi sẽ tiếp tục xử lý và giao hàng trong thời gian sớm nhất.";
    }

    public String cancelledSubject(OrderCancelledEvent event) {
        return "Đơn hàng " + event.getOrderId() + " đã bị hủy";
    }

    public String cancelledBody(OrderCancelledEvent event) {
        return "Đơn hàng " + event.getOrderId() + " đã bị hủy. Lý do: "
                + (event.getReason() == null ? "Không xác định" : event.getReason()) + ".";
    }
}
