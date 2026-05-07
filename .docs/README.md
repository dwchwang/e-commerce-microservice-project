# Thư mục `.docs` — Tài liệu Báo cáo Đồ án Tốt nghiệp

> Thư mục này chứa **đề cương báo cáo** và **tài liệu phân tích kiến trúc** dùng làm khung viết Báo cáo DATN.
> Khác với `.report/` (nghiên cứu lý thuyết) và `.guide/` (hướng dẫn vận hành), `.docs/` tập trung vào *cách trình bày báo cáo*.

## Danh mục file

| File | Nội dung | Dùng cho |
|------|----------|----------|
| [00-de-cuong-bao-cao-datn.md](./00-de-cuong-bao-cao-datn.md) | Đề cương đầy đủ 5 chương + phụ lục, có map nguồn `.report/`/`.guide/`/source code cho từng mục | Khung tổng để viết báo cáo |
| [01-phan-tich-kien-truc-microservices.md](./01-phan-tich-kien-truc-microservices.md) | Phân tích chi tiết kiến trúc 13 microservice đã code: bounded context, giao tiếp, các pattern, state machines, sơ đồ cần vẽ | Tư liệu chính cho **Chương 3** (Phân tích & Thiết kế) |

## Quy trình sử dụng

1. **Bước 1** — Đọc `00-de-cuong-bao-cao-datn.md` để có khung 5 chương.
2. **Bước 2** — Khi viết Chương 2 (Cơ sở lý thuyết): mở các file tương ứng trong `.report/` được map ở mỗi mục.
3. **Bước 3** — Khi viết Chương 3 (Thiết kế): dùng `01-phan-tich-kien-truc-microservices.md` làm tư liệu chính, kèm `.guide/` cho cấu hình.
4. **Bước 4** — Khi viết Chương 4 (Cài đặt): mở các file source code được trỏ trong đề cương, copy snippet quan trọng (đừng paste cả file).
5. **Bước 5** — Khi viết Chương 5 (Kiểm thử): chạy `./mvnw test`, `docker compose up`, k6/JMeter (nếu có), thu screenshot, ghi số liệu thực tế.

## Quy ước nguồn tham chiếu

- **`.report/<NN>-...md`** — Lý thuyết (định nghĩa, citation hàn lâm, câu hỏi phản biện).
- **`.guide/<NN>-...md`** — Vận hành (cách chạy, cấu hình, screenshot demo).
- **Đường dẫn source code** dạng `[order-service/.../OrderServiceImpl.java](../order-service/.../OrderServiceImpl.java)` — bằng chứng đã triển khai thực tế.

## Nguyên tắc viết báo cáo (rút từ `.report/22`)

- ✅ Chỉ ghi *"đã triển khai"* khi có code + screenshot + test chứng minh.
- ✅ Số liệu hiệu năng (RPS, p95, p99) — chỉ điền sau khi đo thật, không placeholder.
- ✅ Trích dẫn hàn lâm (Newman, Richardson, Evans, Fowler, RFC) cho mọi định nghĩa lớn.
- ❌ Tránh "nói chay" — mỗi pattern phải gắn số liệu cụ thể của dự án (vd: rate limit `replenish=3, burst=5` thay vì "có rate limit").
- ❌ Tránh paste nguyên file code; chỉ snippet quan trọng (≤ 30 dòng/snippet).
