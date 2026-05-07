# 19. Tích Hợp Cổng Thanh Toán VNPAY

## 1. Mục Tiêu Nghiên Cứu

- Hiểu mô hình hoạt động của payment gateway
- Hiểu HMAC-SHA512 secure hash, vì sao chống giả mạo
- Hiểu IPN (Instant Payment Notification) và Return URL
- Hiểu PCI-DSS đại cương (đồ án không touch card data)

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Payment Gateway là gì?

Bên thứ 3 đứng giữa **merchant** (cửa hàng) và **acquirer** (ngân hàng) để xử lý thanh toán.

Trong VN: VNPAY, Momo, ZaloPay, Onepay, ...
Quốc tế: Stripe, PayPal, Adyen, ...

**Lợi ích merchant**:
- Không cần PCI-DSS Level 1 (compliance khắc nghiệt cho card data)
- Hỗ trợ nhiều phương thức (ATM, credit, QR, ví)
- Outsource fraud detection, chargeback

### 2.2. PCI-DSS đại cương

Payment Card Industry Data Security Standard:
- 12 requirement: encryption, network seg, access control, logging, ...
- 4 levels theo volume giao dịch
- **Hosted payment** (redirect/iframe) → merchant **không touch card data** → giảm scope PCI

→ Dự án dùng VNPAY hosted payment → user nhập card trên trang VNPAY → tránh PCI scope.

### 2.3. Mô hình giao dịch VNPAY

```
1. User chọn VNPAY khi checkout
2. Merchant (payment-service) tạo payment URL với secure hash
3. Redirect user đến VNPAY URL
4. User nhập thông tin thẻ trên trang VNPAY (TLS, secure)
5. VNPAY xử lý với ngân hàng
6. VNPAY callback merchant qua 2 channel:
   - Return URL: redirect user về merchant (có thể bị skip nếu user đóng tab)
   - IPN: server-to-server, bắt buộc, retry nếu không nhận ACK
7. Merchant verify hash → cập nhật order
```

### 2.4. HMAC-SHA512 Secure Hash

VNPAY ký mọi tham số bằng `vnp_HashSecret` (chỉ merchant biết):
```
queryString = "vnp_TmnCode=ABC123&vnp_Amount=1000000&...&vnp_TxnRef=ORDER123"
hash = HMAC_SHA512(hashSecret, queryString)
url = baseUrl + "?" + queryString + "&vnp_SecureHash=" + hash
```

→ Bất kỳ ai modify tham số → hash sai → server reject.

→ Đây là **integrity protection**, không phải confidentiality (HTTPS lo confidentiality).

### 2.5. Return URL vs IPN

| | Return URL | IPN |
|---|-----------|-----|
| Trigger | User redirect | VNPAY server call |
| Reliability | Có thể skip (user đóng tab) | Retry đến khi 200 OK |
| Mục đích | UX — show kết quả | **Source of truth** cho update DB |
| Security | Verify hash + check IP whitelist | Verify hash + IP whitelist |

→ **KHÔNG dựa Return URL** để cập nhật trạng thái thanh toán. Chỉ IPN là tin được.

### 2.6. Idempotency

VNPAY có thể retry IPN nếu merchant không response 200. → IPN handler **phải idempotent**:
- Check `vnp_TxnRef` (mã giao dịch) đã processed chưa
- Nếu đã COMPLETED, return 200 ngay (không xử lý lại)

---

## 3. Cách Áp Dụng Trong Dự Án

> Phạm vi hiện tại của đồ án là **VNPAY sandbox**. Không viết là payment production/thanh toán thật nếu chưa có merchant production credentials và giao dịch production.

### 3.1. Flow tổng thể

```
1. order-service chuyển đơn sang STOCK_RESERVED sau khi inventory reserve thành công
2. Với COD:
   - order-service publish "payment-requested"
   - payment-service tạo Payment COMPLETED và publish payment-success
3. Với VNPAY:
   - client gọi REST endpoint tạo payment URL
   - payment-service tạo Payment record (PENDING, expires_at = NOW + 30 min)
   - build VNPAY URL với HMAC hash
4. Client redirect → VNPAY login + nhập thẻ
5. VNPAY xử lý → callback:
   a) GET /api/payments/vnpay/return?vnp_*=*  ← user redirect
   b) POST /api/payments/vnpay/ipn body=...    ← server callback
6. payment-service verify hash + cập nhật Payment
7. Publish Kafka payment-success / payment-failed
8. order-service update Order CONFIRMED / CANCELLED
```

### 3.2. Build payment URL

```java
public String buildPaymentUrl(Order order) {
  Map<String, String> params = new TreeMap<>();
  params.put("vnp_Version", "2.1.0");
  params.put("vnp_Command", "pay");
  params.put("vnp_TmnCode", config.getTmnCode());
  params.put("vnp_Amount", String.valueOf(order.getTotal().multiply(100).longValue()));  // VND × 100
  params.put("vnp_CurrCode", "VND");
  params.put("vnp_TxnRef", order.getId().toString());
  params.put("vnp_OrderInfo", "Order " + order.getId());
  params.put("vnp_ReturnUrl", config.getReturnUrl());
  params.put("vnp_CreateDate", formatDate());
  params.put("vnp_IpAddr", clientIp);
  params.put("vnp_Locale", "vn");
  
  String query = buildQueryString(params);
  String hash = hmacSHA512(config.getHashSecret(), query);
  return config.getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + hash;
}
```

### 3.3. Verify hash callback

```java
public boolean verifyHash(Map<String, String> params) {
  String receivedHash = params.remove("vnp_SecureHash");
  params.remove("vnp_SecureHashType");
  
  TreeMap<String, String> sorted = new TreeMap<>(params);
  String query = buildQueryString(sorted);
  String calculatedHash = hmacSHA512(config.getHashSecret(), query);
  
  return calculatedHash.equalsIgnoreCase(receivedHash);
  // Production hardening: có thể đổi sang constant-time comparison.
}
```

### 3.4. IPN handler

```java
@PostMapping("/api/payments/vnpay/ipn")
public Map<String, String> handleIpn(@RequestParam Map<String, String> params) {
  if (!verifyHash(params)) {
    return Map.of("RspCode", "97", "Message", "Invalid signature");
  }
  
  String txnRef = params.get("vnp_TxnRef");
  String responseCode = params.get("vnp_ResponseCode");
  
  Payment payment = paymentRepo.findByOrderId(txnRef);
  
  if (payment.getStatus() == COMPLETED) {
    return Map.of("RspCode", "00", "Message", "Already confirmed");  // idempotent
  }
  
  if ("00".equals(responseCode)) {
    payment.setStatus(COMPLETED);
    publishOutbox("payment-success", payment);
  } else {
    payment.setStatus(FAILED);
    publishOutbox("payment-failed", payment);
  }
  paymentRepo.save(payment);
  
  return Map.of("RspCode", "00", "Message", "Confirmed");
}
```

### 3.5. PaymentTimeoutScheduler

Nếu user mở VNPAY rồi đóng tab → không có IPN → Payment stuck PENDING.
Scheduler 60s tìm Payment có `expires_at < NOW` và status PENDING:
- Mark TIMEOUT
- Publish `payment-failed`
- order-service cancel order, inventory release

### 3.6. Gateway public 2 endpoint

VNPAY callback không có JWT → Gateway phải để public:
```yaml
filters:
  - id: vnpay-public
    uri: lb://payment-service
    predicates:
      - Path=/api/payments/vnpay/return,/api/payments/vnpay/ipn
```

Bảo mật: HMAC verify + IP whitelist (production).

---

## 4. Đặc Tính An Toàn

| Threat | Phòng |
|--------|-------|
| Tampering URL params | HMAC verify |
| Replay attack (same txnRef nhiều lần) | Idempotency check |
| IP spoofing | Whitelist VNPAY IP (production) |
| Race (return URL trước IPN) | Lock per txnRef hoặc dùng version |
| Timing attack on hash compare | Có thể harden bằng constant-time compare trong production |

---

## 5. Từ Khóa Nghiên Cứu

```
- vnpay api documentation
- payment gateway integration pattern
- hmac sha512 message authentication code
- idempotency key payment
- pci dss scope reduction hosted payment
- ipn instant payment notification reliability
- timing attack constant time comparison
- payment state machine
- 3d secure 3ds authentication
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Vì sao tách payment-service riêng?**
→ Tách để (1) compliance — payment có rules đặc biệt; (2) scale độc lập; (3) thay provider khác (Momo) không ảnh hưởng order-service; (4) audit log riêng.

**Q2: Tại sao tin IPN hơn Return URL?**
→ Return URL có thể bị user skip (đóng tab, mạng lỗi). VNPAY guarantee gọi IPN với retry → chỉ IPN là source of truth.

**Q3: HMAC vs digital signature?**
→ HMAC: symmetric key (cả 2 bên cùng biết secret). Đơn giản, nhanh. Digital signature (RSA): asymmetric — bên ký giữ private key, bên verify dùng public. VNPAY chọn HMAC vì merchant ↔ VNPAY trust nhau và share secret qua kênh ngoài.

**Q4: Em xử lý double IPN thế nào?**
→ Idempotency: kiểm tra `payment.status == COMPLETED` → return 200 OK ngay, không reprocess. Cũng có version field để optimistic lock.

**Q5: PaymentTimeoutScheduler có race với IPN không?**
→ Có thể: Scheduler đang mark TIMEOUT, IPN đến mark COMPLETED. Giải bằng:
- Optimistic lock (`@Version`)
- Atomic update WHERE status = PENDING
- Nếu race, ưu tiên IPN (latest update) — nhưng hiếm khi trùng vì timeout 30 phút >> IPN delay

**Q6: Có dùng 3DS không?**
→ Đồ án dùng VNPAY sandbox không 3DS. Production: VNPAY có thể yêu cầu 3DS — UI flow dài hơn, nhưng API merchant không khác.

**Q7: Refund thì sao?**
→ Đồ án scope không có refund. Production: VNPAY có API `vnp_Command=refund` với same hash mechanism. Cần state machine thêm REFUNDED.

---

## 7. Tài Liệu Tham Khảo

### VNPAY
- Documentation: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
- Sandbox test cards

### Standards
- PCI-DSS v4.0 — pcisecuritystandards.org
- HMAC RFC 2104, FIPS 198-1
- 3D Secure 2.0 — EMVCo

### Books
- Adam Shostack, *Threat Modeling: Designing for Security*
- Stripe Engineering Blog — payment best practices
- Paypal Developer Documentation — IPN reliability
