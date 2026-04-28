package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.StockCheckResponse;
import com.ecommerce.inventory.dto.StockInRequest;
import com.ecommerce.inventory.dto.StockOutRequest;

import java.util.List;

public interface InventoryService {

    InventoryResponse getInventory(String sku);

    List<StockCheckResponse> checkStock(List<String> skus);

    InventoryResponse stockIn(StockInRequest request);

    InventoryResponse stockOut(StockOutRequest request);

    List<InventoryResponse> getLowStock(Integer threshold);
}
