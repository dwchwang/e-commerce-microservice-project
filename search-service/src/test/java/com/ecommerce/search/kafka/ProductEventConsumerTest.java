package com.ecommerce.search.kafka;

import com.ecommerce.search.service.ProductIndexService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ProductEventConsumerTest {

    @Test
    void invalidPayloadThrows() {
        ProductEventConsumer consumer = new ProductEventConsumer(new ObjectMapper(), mock(ProductIndexService.class));

        assertThatThrownBy(() -> consumer.handleProductCreated("{bad-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid product-created payload");
    }
}
