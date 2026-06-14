package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminOrderController {

    private final OrderRepository orderRepository;

    @GetMapping
    public ApiResponse<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String normalizedQ = q == null ? "" : q.trim().toLowerCase();
        List<OrderResponse> filtered = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(order -> status == null || order.getStatus() == status)
                .filter(order -> userId == null || userId.isBlank()
                        || userId.equals(order.getUserId())
                        || userId.equals(String.valueOf(order.getUserId())))
                .filter(order -> normalizedQ.isBlank()
                        || order.getId().toString().contains(normalizedQ)
                        || safeLower(order.getUserEmail()).contains(normalizedQ)
                        || safeLower(order.getShippingName()).contains(normalizedQ)
                        || safeLower(order.getShippingPhone()).contains(normalizedQ))
                .map(OrderResponse::from)
                .toList();

        int pageSize = Math.max(1, Math.min(size, 200));
        int pageIndex = Math.max(0, page);
        int from = Math.min(pageIndex * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        Page<OrderResponse> result = new PageImpl<>(
                filtered.subList(from, to), PageRequest.of(pageIndex, pageSize), filtered.size());
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable UUID id) {
        return ApiResponse.ok(OrderResponse.from(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id))));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        OrderStatus nextStatus = OrderStatus.valueOf(request.getOrDefault("status", order.getStatus().name()));
        order.setStatus(nextStatus);
        if (nextStatus == OrderStatus.CANCELLED && request.get("note") != null) {
            order.setCancelReason(request.get("note"));
        }
        return ApiResponse.ok(OrderResponse.from(orderRepository.save(order)));
    }

    @GetMapping("/analytics/summary")
    public ApiResponse<SummaryResponse> summary(@RequestParam(defaultValue = "7d") String period) {
        LocalDateTime from = LocalDateTime.now().minusDays(parseDays(period, 7));
        List<Order> orders = orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt() != null && !order.getCreatedAt().isBefore(from))
                .toList();
        BigDecimal revenue = orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long customers = orders.stream().map(Order::getUserId).distinct().count();

        return ApiResponse.ok(SummaryResponse.builder()
                .revenue(revenue)
                .orders(orders.size())
                .customers(customers)
                .conversionRate(orders.isEmpty() ? 0.0 : 100.0)
                .build());
    }

    @GetMapping("/analytics/revenue")
    public ApiResponse<List<RevenuePoint>> revenue(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from) {
        LocalDate start = from != null ? from : LocalDate.now().minusDays(Math.max(1, days) - 1L);
        LocalDate end = start.plusDays(Math.max(1, days) - 1L);
        Map<LocalDate, BigDecimal> byDate = orderRepository.findAll().stream()
                .filter(order -> order.getCreatedAt() != null && order.getStatus() == OrderStatus.CONFIRMED)
                .filter(order -> !order.getCreatedAt().toLocalDate().isBefore(start)
                        && !order.getCreatedAt().toLocalDate().isAfter(end))
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)));

        List<RevenuePoint> points = start.datesUntil(end.plusDays(1))
                .map(date -> new RevenuePoint(date, byDate.getOrDefault(date, BigDecimal.ZERO)))
                .toList();
        return ApiResponse.ok(points);
    }

    @GetMapping("/analytics/status-counts")
    public ApiResponse<List<StatusCount>> statusCounts() {
        Map<OrderStatus, Long> counts = orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        return ApiResponse.ok(counts.entrySet().stream()
                .map(entry -> new StatusCount(entry.getKey().name(), entry.getValue()))
                .sorted(Comparator.comparing(StatusCount::status))
                .toList());
    }

    @GetMapping("/analytics/top-products")
    public ApiResponse<List<TopProduct>> topProducts(@RequestParam(defaultValue = "10") int limit) {
        Map<String, List<OrderItem>> byProduct = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(OrderItem::getProductName));

        List<TopProduct> products = byProduct.entrySet().stream()
                .map(entry -> {
                    int quantity = entry.getValue().stream().mapToInt(OrderItem::getQuantity).sum();
                    BigDecimal revenue = entry.getValue().stream()
                            .map(OrderItem::getSubtotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new TopProduct(entry.getKey(), quantity, revenue);
                })
                .sorted(Comparator.comparing(TopProduct::quantity).reversed())
                .limit(Math.max(1, Math.min(limit, 50)))
                .toList();
        return ApiResponse.ok(products);
    }

    private int parseDays(String period, int fallback) {
        try {
            return Integer.parseInt(period.replace("d", ""));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    @Data
    @Builder
    public static class SummaryResponse {
        private BigDecimal revenue;
        private long orders;
        private long customers;
        private double conversionRate;
    }

    public record RevenuePoint(LocalDate date, BigDecimal revenue) {
    }

    public record StatusCount(String status, long count) {
    }

    public record TopProduct(String productName, int quantity, BigDecimal revenue) {
    }
}
