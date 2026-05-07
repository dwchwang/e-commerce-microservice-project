# Kafka Topics — Bản Đồ Sự Kiện

## Tổng Quan

Kafka KRaft (không Zookeeper) chạy tại port `9092` (external) / `29092` (internal Docker network).

**Kafka tự động tạo topics** khi producer gửi message lần đầu, ngoại trừ topic `flash-sale-order-requested` được khai báo rõ ràng với 3 partitions.

---

## Bản Đồ Đầy Đủ: 11 Nhóm Sự Kiện / 13 Topic Vật Lý

Ghi chú: bảng dưới có 11 nhóm nghiệp vụ. Nhóm `product-created` / `product-updated` / `product-deleted` gồm 3 topic vật lý riêng, nên tổng số topic chính là 13.

### 1. `user-registered`
| | |
|---|---|
| **Producer** | identity-service (sau khi tạo user trong Keycloak) |
| **Consumer** | user-service → tạo User profile trong user_db |
| **Kích hoạt khi** | User đăng ký tài khoản mới |

---

### 2. `product-created` / `product-updated` / `product-deleted`
| | |
|---|---|
| **Producer** | product-service (sau mỗi thao tác CRUD sản phẩm) |
| **Consumer** | search-service → index/update/delete trong Elasticsearch |
| **Kích hoạt khi** | Admin tạo, sửa, hoặc xóa sản phẩm |

> Đây là 3 topics riêng biệt, search-service lắng nghe cả 3.

---

### 3. `order-created`
| | |
|---|---|
| **Producer** | order-service (sau khi tạo đơn hàng) |
| **Consumer** | inventory-service → RESERVE tồn kho |
| **Kích hoạt khi** | User đặt đơn hàng thành công |
| **Payload quan trọng** | orderId, userId, danh sách items (productId + quantity) |

---

### 4. `inventory-updated`
| | |
|---|---|
| **Producer** | inventory-service (sau khi reserve tồn kho thành công) |
| **Consumer** | order-service → update status → `STOCK_RESERVED`, set `reservation_expired_at` |
| **Kích hoạt khi** | Inventory đã reserve hàng thành công |

---

### 5. `inventory-failed`
| | |
|---|---|
| **Producer** | inventory-service (khi không đủ tồn kho) |
| **Consumer** | order-service → update status → `CANCELLED` |
| **Kích hoạt khi** | Sản phẩm hết hàng khi đặt đơn |

---

### 6. `order-confirmed`
| | |
|---|---|
| **Producer** | order-service (khi order status chuyển sang `CONFIRMED` — COD hoặc VNPAY success) |
| **Consumer** | inventory-service → CONFIRM reservation (trừ hẳn tồn kho) |
| **Consumer** | notification-service → gửi email xác nhận đơn hàng |
| **Kích hoạt khi** | Đơn COD được xác nhận, hoặc thanh toán VNPAY thành công |

---

### 7. `payment-requested`
| | |
|---|---|
| **Producer** | order-service (sau khi stock reserved, với COD) |
| **Consumer** | payment-service → tạo Payment COD COMPLETED, publish `payment-success` |
| **Kích hoạt khi** | Đơn COD đã reserve tồn kho và cần hoàn tất payment saga tự động |

---

### 8. `payment-success`
| | |
|---|---|
| **Producer** | payment-service (COD completed hoặc VNPAY IPN thành công) |
| **Consumer** | order-service → update status `CONFIRMED`, publish `order-confirmed` |
| **Kích hoạt khi** | Thanh toán COD/VNPAY thành công |

---

### 9. `payment-failed`
| | |
|---|---|
| **Producer** | payment-service (VNPAY callback thất bại, hoặc timeout) |
| **Consumer** | order-service → update status `CANCELLED`, publish `order-cancelled` |
| **Kích hoạt khi** | Thanh toán VNPAY thất bại hoặc hết thời gian |

---

### 10. `order-cancelled`
| | |
|---|---|
| **Producer** | order-service (khi hủy đơn — do payment fail, timeout, inventory fail, hoặc reservation expiry) |
| **Consumer** | inventory-service → RELEASE reserved tồn kho |
| **Consumer** | notification-service → gửi email đơn bị hủy |
| **Kích hoạt khi** | Đơn hàng bị hủy vì bất kỳ lý do gì |

---

### 11. `flash-sale-order-requested` *(3 partitions)*
| | |
|---|---|
| **Producer** | flash-sale-service (sau khi deduct Redis stock thành công) |
| **Consumer** | order-service → tạo đơn hàng flash sale (bypass normal cart flow) |
| **Kích hoạt khi** | User mua flash sale thành công (còn slot) |
| **Đặc biệt** | Topic duy nhất được định nghĩa rõ ràng với 3 partitions (high concurrency) |

---

## Sơ Đồ Luồng Kafka Tổng Hợp

```
identity-service ──[user-registered]──────────────────► user-service

product-service  ──[product-created/updated/deleted]──► search-service (Elasticsearch index)

                 ┌─[order-created]────────────────────► inventory-service
                 │                                         │
                 │                                         ├─[inventory-updated]──► order-service (STOCK_RESERVED)
                 │                                         │                          │
                 │                                         │                     COD ─┤─[payment-requested]──► payment-service
                 │                                         │                          │                           │
                 │                                         │                          │      VNPAY REST + IPN ────┤
                 │                                         │                          │                           ├─[payment-success]──► order-service
order-service ───┤                                         │                          │                           └─[payment-failed]───► order-service
                 │                                         │
                 │                                    [inventory-failed]──► order-service (CANCELLED)
                 │
                 └─[order-confirmed]──────────────────► inventory-service (confirm deduction)
                 │                                    ► notification-service (confirmed email)
                 │
                 └─[order-cancelled]──────────────────► inventory-service (release stock)
                                                      ► notification-service (cancelled email)

flash-sale-service──[flash-sale-order-requested]──────► order-service (create flash order)
```

---

## Kiểm Tra Topics Đang Chạy

```bash
# Liệt kê tất cả topics đang có trong Kafka
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 --list

# Xem chi tiết một topic
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic order-created

# Xem messages trong topic (từ đầu)
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --from-beginning \
  --max-messages 5
```

---

## Outbox Pattern (Đảm Bảo Delivery)

Các service quan trọng (order-service, payment-service) dùng **Outbox Pattern**:

1. Khi tạo đơn hàng, service lưu event vào bảng `outbox` **trong cùng một database transaction** với business data
2. `OutboxPoller` (chạy mỗi 1 giây) đọc bảng outbox và publish lên Kafka
3. Sau khi publish thành công → đánh dấu event là `PUBLISHED`

**Kết quả**: Dù service crash ngay sau khi lưu DB, event vẫn được publish sau khi restart — không mất message.

## Processed Events (Chống Xử Lý Trùng)

Mỗi consumer service có bảng `processed_events` lưu UUID của event đã xử lý.
Trước khi xử lý, consumer check bảng này — nếu event_id đã tồn tại thì bỏ qua (idempotent).

**Kết quả**: Dù Kafka deliver lại message (at-least-once), logic nghiệp vụ chỉ chạy đúng **một lần**.
