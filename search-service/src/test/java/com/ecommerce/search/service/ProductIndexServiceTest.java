package com.ecommerce.search.service;

import com.ecommerce.common.event.ProductCreatedEvent;
import com.ecommerce.common.event.ProductDeletedEvent;
import com.ecommerce.search.document.ProductDocument;
import com.ecommerce.search.repository.ProcessedSearchEventRepository;
import com.ecommerce.search.repository.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductIndexServiceTest {

    @Mock
    private ProductSearchRepository productSearchRepository;

    @Mock
    private ProcessedSearchEventRepository processedSearchEventRepository;

    private ProductIndexService service;

    @BeforeEach
    void setUp() {
        service = new ProductIndexService(productSearchRepository, processedSearchEventRepository);
    }

    @Test
    void indexCreatedSkipsDuplicateEvent() {
        ProductCreatedEvent event = createdEvent();
        when(processedSearchEventRepository.existsById(event.getEventId().toString())).thenReturn(true);

        service.indexCreated(event);

        verify(productSearchRepository, never()).save(any());
    }

    @Test
    void indexCreatedUpsertsProductWithSearchFields() {
        ProductCreatedEvent event = createdEvent();
        when(processedSearchEventRepository.existsById(event.getEventId().toString())).thenReturn(false);
        when(productSearchRepository.findById(event.getProductId().toString())).thenReturn(Optional.empty());

        service.indexCreated(event);

        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(productSearchRepository).save(captor.capture());
        ProductDocument document = captor.getValue();
        assertThat(document.getId()).isEqualTo(event.getProductId().toString());
        assertThat(document.getBrandId()).isEqualTo("brand-1");
        assertThat(document.getBrandName()).isEqualTo("Apple");
        assertThat(document.getIsActive()).isTrue();
        assertThat(document.getImageUrls()).containsExactly("https://example.test/iphone.jpg");
        verify(processedSearchEventRepository).save(any());
    }

    @Test
    void hardDeleteRemovesDocumentAndMarksProcessed() {
        UUID eventId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(processedSearchEventRepository.existsById(eventId.toString())).thenReturn(false);

        service.deleteOrDeactivate(ProductDeletedEvent.builder()
                .eventId(eventId)
                .productId(productId)
                .hardDelete(true)
                .occurredAt(Instant.now())
                .build());

        verify(productSearchRepository).deleteById(productId.toString());
        verify(processedSearchEventRepository).save(any());
    }

    @Test
    void softDeleteHidesDocument() {
        UUID eventId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductDocument document = ProductDocument.builder()
                .id(productId.toString())
                .isActive(true)
                .build();
        when(processedSearchEventRepository.existsById(eventId.toString())).thenReturn(false);
        when(productSearchRepository.findById(productId.toString())).thenReturn(Optional.of(document));

        service.deleteOrDeactivate(ProductDeletedEvent.builder()
                .eventId(eventId)
                .productId(productId)
                .hardDelete(false)
                .occurredAt(Instant.now())
                .build());

        assertThat(document.getIsActive()).isFalse();
        verify(productSearchRepository).save(document);
        verify(processedSearchEventRepository).save(any());
    }

    private ProductCreatedEvent createdEvent() {
        return ProductCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .sku("IPHONE-15")
                .name("iPhone 15")
                .description("Phone")
                .price(BigDecimal.valueOf(999))
                .categoryId("cat-1")
                .categoryName("Phones")
                .brandId("brand-1")
                .brandName("Apple")
                .isActive(true)
                .imageUrls(java.util.List.of("https://example.test/iphone.jpg"))
                .build();
    }
}
