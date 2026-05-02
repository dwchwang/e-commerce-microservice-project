package com.ecommerce.search.service;

import com.ecommerce.common.event.ProductCreatedEvent;
import com.ecommerce.common.event.ProductDeletedEvent;
import com.ecommerce.common.event.ProductUpdatedEvent;
import com.ecommerce.search.document.ProcessedSearchEventDocument;
import com.ecommerce.search.document.ProductDocument;
import com.ecommerce.search.repository.ProcessedSearchEventRepository;
import com.ecommerce.search.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductIndexService {

    private static final String PRODUCT_CREATED_TOPIC = "product-created";
    private static final String PRODUCT_UPDATED_TOPIC = "product-updated";
    private static final String PRODUCT_DELETED_TOPIC = "product-deleted";

    private final ProductSearchRepository productSearchRepository;
    private final ProcessedSearchEventRepository processedSearchEventRepository;

    public void indexCreated(ProductCreatedEvent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        Instant now = Instant.now();
        ProductDocument existing = productSearchRepository.findById(event.getProductId().toString()).orElse(null);
        productSearchRepository.save(fromCreated(event, existing, now));
        markProcessed(event.getEventId(), PRODUCT_CREATED_TOPIC, now);
    }

    public void indexUpdated(ProductUpdatedEvent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        Instant now = Instant.now();
        ProductDocument existing = productSearchRepository.findById(event.getProductId().toString()).orElse(null);
        productSearchRepository.save(fromUpdated(event, existing, now));
        markProcessed(event.getEventId(), PRODUCT_UPDATED_TOPIC, now);
    }

    public void deleteOrDeactivate(ProductDeletedEvent event) {
        if (alreadyProcessed(event.getEventId())) {
            return;
        }
        Instant processedAt = Instant.now();
        String productId = event.getProductId().toString();
        if (event.isHardDelete()) {
            productSearchRepository.deleteById(productId);
        } else {
            productSearchRepository.findById(productId).ifPresent(document -> {
                document.setIsActive(false);
                document.setUpdatedAt(event.getOccurredAt() != null ? event.getOccurredAt() : processedAt);
                productSearchRepository.save(document);
            });
        }
        markProcessed(event.getEventId(), PRODUCT_DELETED_TOPIC, processedAt);
    }

    private boolean alreadyProcessed(UUID eventId) {
        return eventId != null && processedSearchEventRepository.existsById(eventId.toString());
    }

    private void markProcessed(UUID eventId, String topic, Instant processedAt) {
        if (eventId == null) {
            return;
        }
        processedSearchEventRepository.save(ProcessedSearchEventDocument.builder()
                .eventId(eventId.toString())
                .topic(topic)
                .processedAt(processedAt)
                .build());
    }

    private ProductDocument fromCreated(ProductCreatedEvent event, ProductDocument existing, Instant now) {
        return ProductDocument.builder()
                .id(event.getProductId().toString())
                .sku(event.getSku())
                .name(event.getName())
                .description(event.getDescription())
                .price(toDouble(event.getPrice()))
                .categoryId(event.getCategoryId())
                .categoryName(event.getCategoryName())
                .brandId(event.getBrandId())
                .brandName(event.getBrandName())
                .imageUrls(event.getImageUrls() != null ? event.getImageUrls() : List.of())
                .isActive(event.getIsActive())
                .createdAt(existing != null && existing.getCreatedAt() != null ? existing.getCreatedAt() : now)
                .updatedAt(now)
                .build();
    }

    private ProductDocument fromUpdated(ProductUpdatedEvent event, ProductDocument existing, Instant now) {
        return ProductDocument.builder()
                .id(event.getProductId().toString())
                .sku(event.getSku())
                .name(event.getName())
                .description(event.getDescription())
                .price(toDouble(event.getPrice()))
                .categoryId(event.getCategoryId())
                .categoryName(event.getCategoryName())
                .brandId(event.getBrandId())
                .brandName(event.getBrandName())
                .imageUrls(event.getImageUrls() != null ? event.getImageUrls() : List.of())
                .isActive(event.getIsActive())
                .createdAt(existing != null && existing.getCreatedAt() != null ? existing.getCreatedAt() : now)
                .updatedAt(now)
                .build();
    }

    private Double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : null;
    }
}
