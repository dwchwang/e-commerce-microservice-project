package com.ecommerce.product.service;

import com.ecommerce.product.kafka.ProductEventProducer;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ProductServiceTest {

    private final ProductService productService = new ProductService(
            mock(ProductRepository.class),
            mock(CategoryRepository.class),
            mock(BrandRepository.class),
            mock(ProductEventProducer.class));

    @Test
    void normalizePageableSortDefaultsToCreatedAtColumnWhenUnsorted() {
        Pageable pageable = PageRequest.of(2, 20);

        Pageable normalized = productService.normalizePageableSort(pageable);

        assertThat(normalized.getPageNumber()).isEqualTo(2);
        assertThat(normalized.getPageSize()).isEqualTo(20);
        assertThat(normalized.getSort().getOrderFor("created_at")).isNotNull();
        assertThat(normalized.getSort().getOrderFor("created_at").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void normalizePageableSortMapsCamelCaseFieldsToDatabaseColumns() {
        Pageable pageable = PageRequest.of(0, 8,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("updatedAt")));

        Pageable normalized = productService.normalizePageableSort(pageable);

        assertThat(normalized.getSort().getOrderFor("created_at")).isNotNull();
        assertThat(normalized.getSort().getOrderFor("created_at").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(normalized.getSort().getOrderFor("updated_at")).isNotNull();
        assertThat(normalized.getSort().getOrderFor("updated_at").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(normalized.getSort().getOrderFor("createdAt")).isNull();
    }

    @Test
    void normalizePageableSortFallsBackWhenOnlyUnknownSortFieldsAreProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("categoryName"));

        Pageable normalized = productService.normalizePageableSort(pageable);

        assertThat(normalized.getSort().getOrderFor("created_at")).isNotNull();
        assertThat(normalized.getSort().getOrderFor("categoryName")).isNull();
    }
}
