package com.ecommerce.inventory.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.StockMovement;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.StockMovementRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminInventoryController {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    @GetMapping
    public ApiResponse<List<InventoryResponse>> listInventory(@RequestParam(required = false) String q) {
        String normalizedQ = q == null ? "" : q.trim().toLowerCase();
        return ApiResponse.ok(inventoryRepository.findAll(Sort.by(Sort.Direction.ASC, "sku")).stream()
                .filter(item -> normalizedQ.isBlank()
                        || safeLower(item.getSku()).contains(normalizedQ)
                        || safeLower(item.getProductName()).contains(normalizedQ))
                .map(this::toInventoryResponse)
                .toList());
    }

    @GetMapping("/movements")
    public ApiResponse<List<MovementResponse>> movements(@RequestParam(defaultValue = "100") int size) {
        return ApiResponse.ok(stockMovementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .limit(Math.max(1, Math.min(size, 500)))
                .map(this::toMovementResponse)
                .toList());
    }

    private InventoryResponse toInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .sku(inventory.getSku())
                .productName(inventory.getProductName())
                .quantity(inventory.getQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.availableQuantity())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    private MovementResponse toMovementResponse(StockMovement movement) {
        return MovementResponse.builder()
                .id(movement.getId())
                .sku(movement.getSku())
                .movementType(movement.getMovementType().name())
                .quantity(movement.getQuantity())
                .referenceId(movement.getReferenceId())
                .note(movement.getNote())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    @Data
    @Builder
    public static class MovementResponse {
        private UUID id;
        private String sku;
        private String movementType;
        private Integer quantity;
        private String referenceId;
        private String note;
        private LocalDateTime createdAt;
    }
}
