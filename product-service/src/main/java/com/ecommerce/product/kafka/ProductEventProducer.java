package com.ecommerce.product.kafka;

import com.ecommerce.common.event.ProductCreatedEvent;
import com.ecommerce.common.event.ProductDeletedEvent;
import com.ecommerce.common.event.ProductUpdatedEvent;
import com.ecommerce.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCreated(Product product) {
        kafkaTemplate.send("product-created", product.getId().toString(), ProductCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId().toString() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId().toString() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .isActive(product.getIsActive())
                .imageUrls(imageUrls(product))
                .build());
    }

    public void publishUpdated(Product product) {
        kafkaTemplate.send("product-updated", product.getId().toString(), ProductUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .productId(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId().toString() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId().toString() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .isActive(product.getIsActive())
                .imageUrls(imageUrls(product))
                .build());
    }

    public void publishDeleted(UUID productId, boolean hardDelete) {
        kafkaTemplate.send("product-deleted", productId.toString(), ProductDeletedEvent.builder()
                .eventId(UUID.randomUUID())
                .productId(productId)
                .hardDelete(hardDelete)
                .occurredAt(Instant.now())
                .build());
    }

    private List<String> imageUrls(Product product) {
        if (product.getImages() == null) {
            return List.of();
        }
        return product.getImages().stream()
                .map(image -> image.getImageUrl())
                .toList();
    }
}
