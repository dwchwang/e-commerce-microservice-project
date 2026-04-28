package com.ecommerce.inventory.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.StockCheckResponse;
import com.ecommerce.inventory.dto.StockInRequest;
import com.ecommerce.inventory.dto.StockOutRequest;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{sku}")
    public ApiResponse<InventoryResponse> getInventory(@PathVariable String sku) {
        return ApiResponse.ok(inventoryService.getInventory(sku));
    }

    @GetMapping("/check")
    public ApiResponse<List<StockCheckResponse>> checkStock(@RequestParam String skus) {
        return ApiResponse.ok(inventoryService.checkStock(Arrays.asList(skus.split(","))));
    }

    @PostMapping("/stock-in")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<InventoryResponse> stockIn(@Valid @RequestBody StockInRequest request) {
        return ApiResponse.ok("Stock-in recorded successfully", inventoryService.stockIn(request));
    }

    @PostMapping("/stock-out")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<InventoryResponse> stockOut(@Valid @RequestBody StockOutRequest request) {
        return ApiResponse.ok("Stock-out recorded successfully", inventoryService.stockOut(request));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<List<InventoryResponse>> getLowStock(@RequestParam(defaultValue = "10") Integer threshold) {
        return ApiResponse.ok(inventoryService.getLowStock(threshold));
    }
}
