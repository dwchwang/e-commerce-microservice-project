package com.ecommerce.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductSummaryResponse;
import com.ecommerce.product.dto.SpecificationDto;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.entity.ProductImage;
import com.ecommerce.product.entity.ProductSpecification;
import com.ecommerce.product.kafka.ProductEventProducer;
import com.ecommerce.product.repository.BrandRepository;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductEventProducer productEventProducer;

    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getAllProducts(
            UUID categoryId,
            UUID brandId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String keyword,
            Pageable pageable) {
        Pageable effectivePageable = normalizePageableSort(pageable);
        return productRepository.searchProducts(
                        normalizeKeyword(keyword),
                        categoryId != null ? categoryId.toString() : null,
                        brandId != null ? brandId.toString() : null,
                        minPrice,
                        maxPrice,
                        effectivePageable)
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return toResponse(productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        return toResponse(productRepository.findBySkuAndIsActiveTrue(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku)));
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Product SKU already exists: " + request.getSku());
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(findCategory(request.getCategoryId()))
                .brand(findBrand(request.getBrandId()))
                .isActive(true)
                .build();
        applyChildren(product, request);

        Product saved = productRepository.save(product);
        productEventProducer.publishCreated(saved);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new BusinessException("Product SKU already exists: " + request.getSku());
        }

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(findCategory(request.getCategoryId()));
        product.setBrand(findBrand(request.getBrandId()));
        applyChildren(product, request);

        Product saved = productRepository.save(product);
        productEventProducer.publishUpdated(saved);
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setIsActive(false);
        productRepository.save(product);
        productEventProducer.publishDeleted(id, false);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    Pageable normalizePageableSort(Pageable pageable) {
        Sort normalizedSort = normalizeSort(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }

    private Sort normalizeSort(Sort sort) {
        if (sort.isUnsorted()) {
            return Sort.by("created_at").descending();
        }

        List<Sort.Order> orders = sort.stream()
                .map(this::normalizeOrder)
                .filter(order -> order != null)
                .toList();

        return orders.isEmpty() ? Sort.by("created_at").descending() : Sort.by(orders);
    }

    private Sort.Order normalizeOrder(Sort.Order order) {
        String property = switch (order.getProperty()) {
            case "createdAt", "created_at" -> "created_at";
            case "updatedAt", "updated_at" -> "updated_at";
            case "isActive", "is_active" -> "is_active";
            case "id", "sku", "name", "price" -> order.getProperty();
            default -> null;
        };
        return property == null ? null : new Sort.Order(order.getDirection(), property);
    }

    private Category findCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
    }

    private Brand findBrand(UUID brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", "id", brandId));
    }

    private void applyChildren(Product product, ProductRequest request) {
        product.getImages().clear();
        List<String> imageUrls = request.getImageUrls() != null ? request.getImageUrls() : List.of();
        IntStream.range(0, imageUrls.size())
                .filter(index -> imageUrls.get(index) != null && !imageUrls.get(index).isBlank())
                .mapToObj(index -> ProductImage.builder()
                        .product(product)
                        .imageUrl(imageUrls.get(index).trim())
                        .sortOrder(index)
                        .build())
                .forEach(product.getImages()::add);

        product.getSpecifications().clear();
        List<SpecificationDto> specs = request.getSpecs() != null ? request.getSpecs() : List.of();
        specs.stream()
                .map(spec -> ProductSpecification.builder()
                        .product(product)
                        .specName(spec.getSpecName())
                        .specValue(spec.getSpecValue())
                        .build())
                .forEach(product.getSpecifications()::add);
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .isActive(product.getIsActive())
                .imageUrls(product.getImages().stream().map(ProductImage::getImageUrl).toList())
                .specs(product.getSpecifications().stream()
                        .map(spec -> SpecificationDto.builder()
                                .specName(spec.getSpecName())
                                .specValue(spec.getSpecValue())
                                .build())
                        .toList())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductSummaryResponse toSummaryResponse(Product product) {
        return ProductSummaryResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .primaryImageUrl(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl())
                .build();
    }
}
