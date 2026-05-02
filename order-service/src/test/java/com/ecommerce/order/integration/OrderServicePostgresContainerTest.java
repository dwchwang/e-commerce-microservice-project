package com.ecommerce.order.integration;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.entity.PaymentMethod;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class OrderServicePostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5-alpine");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesSchemaAndRepositoryPersistsOrderAggregate() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('orders', 'order_items', 'outbox', 'processed_events')
                """, Integer.class);
        Integer flashSaleIndexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'uq_orders_flash_sale_user'
                """, Integer.class);

        assertThat(tableCount).isEqualTo(4);
        assertThat(flashSaleIndexCount).isEqualTo(1);

        UUID productId = UUID.randomUUID();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .userId("user-1")
                .userEmail("user@example.com")
                .status(OrderStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.COD)
                .subtotal(BigDecimal.valueOf(25))
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.valueOf(25))
                .shippingName("Test User")
                .shippingPhone("0901234567")
                .shippingAddress("123 Test Street")
                .isFlashSale(false)
                .build();
        OrderItem item = OrderItem.builder()
                .order(order)
                .productId(productId)
                .sku("SKU-1")
                .productName("Test Product")
                .price(BigDecimal.valueOf(25))
                .quantity(1)
                .subtotal(BigDecimal.valueOf(25))
                .build();
        order.setItems(List.of(item));

        Order saved = orderRepository.saveAndFlush(order);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(orderRepository.existsConfirmedOrderForProduct("user-1", productId)).isTrue();
    }
}
