package com.ecommerce.inventory.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.StockCheckResponse;
import com.ecommerce.inventory.dto.StockInRequest;
import com.ecommerce.inventory.dto.StockOutRequest;
import com.ecommerce.inventory.entity.Inventory;
import com.ecommerce.inventory.entity.MovementType;
import com.ecommerce.inventory.entity.StockMovement;
import com.ecommerce.inventory.repository.InventoryRepository;
import com.ecommerce.inventory.repository.StockMovementRepository;
import com.ecommerce.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(String sku) {
        return toResponse(inventoryRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "sku", sku)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockCheckResponse> checkStock(List<String> skus) {
        List<String> normalizedSkus = skus.stream()
                .map(String::trim)
                .filter(sku -> !sku.isBlank())
                .distinct()
                .toList();

        Map<String, Inventory> inventoryBySku = inventoryRepository.findBySkuIn(normalizedSkus).stream()
                .collect(Collectors.toMap(Inventory::getSku, Function.identity()));

        return normalizedSkus.stream()
                .map(sku -> toStockCheck(sku, inventoryBySku.get(sku)))
                .toList();
    }

    @Override
    @Transactional
    public InventoryResponse stockIn(StockInRequest request) {
        Inventory inventory = inventoryRepository.findBySku(request.getSku())
                .orElse(Inventory.builder()
                        .sku(request.getSku())
                        .productName(request.getProductName())
                        .quantity(0)
                        .reservedQuantity(0)
                        .build());

        if (request.getProductName() != null && !request.getProductName().isBlank()) {
            inventory.setProductName(request.getProductName());
        }
        inventory.setQuantity(inventory.getQuantity() + request.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);

        stockMovementRepository.save(StockMovement.builder()
                .sku(request.getSku())
                .movementType(MovementType.STOCK_IN)
                .quantity(request.getQuantity())
                .note(request.getNote())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryResponse stockOut(StockOutRequest request) {
        Inventory inventory = inventoryRepository.findBySkuForUpdate(request.getSku())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "sku", request.getSku()));
        if (inventory.availableQuantity() < request.getQuantity()) {
            throw new BusinessException("Insufficient available quantity for SKU: " + request.getSku());
        }

        inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        Inventory saved = inventoryRepository.save(inventory);
        stockMovementRepository.save(StockMovement.builder()
                .sku(request.getSku())
                .movementType(MovementType.STOCK_OUT)
                .quantity(request.getQuantity())
                .note(request.getNote())
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStock(Integer threshold) {
        return inventoryRepository.findLowStock(threshold).stream()
                .map(this::toResponse)
                .toList();
    }

    private StockCheckResponse toStockCheck(String sku, Inventory inventory) {
        int availableQuantity = inventory != null ? inventory.availableQuantity() : 0;
        return StockCheckResponse.builder()
                .sku(sku)
                .available(availableQuantity > 0)
                .availableQuantity(availableQuantity)
                .build();
    }

    private InventoryResponse toResponse(Inventory inventory) {
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
}
