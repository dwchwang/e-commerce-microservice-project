# Yêu Cầu Hệ Thống (Prerequisites)

## Phần Mềm Bắt Buộc

### 1. Java 21

Kiểm tra:
```bash
java -version
# Phải hiện: openjdk version "21.x.x" hoặc java version "21.x.x"
```

Cài đặt (nếu chưa có):
- **macOS**: `brew install openjdk@21`
- **Ubuntu/Debian**: `sudo apt install openjdk-21-jdk`
- **Windows**: Tải từ https://adoptium.net (Eclipse Temurin 21)

### 2. Docker Desktop

Kiểm tra:
```bash
docker --version       # Docker version 24.x+ trở lên
docker compose version # Docker Compose version v2.x+
```

> **Lưu ý quan trọng**: Hệ thống dùng `docker compose` (V2), KHÔNG phải `docker-compose` (V1).

Cài đặt:
- **macOS / Windows**: Tải Docker Desktop từ https://www.docker.com/products/docker-desktop
- **Linux**: Tải Docker Engine + Docker Compose Plugin

**Cấu hình tài nguyên khuyến nghị cho Docker Desktop:**
| Tài nguyên | Tối thiểu | Khuyến nghị |
|---|---|---|
| CPU | 4 cores | 6+ cores |
| RAM | 8 GB | 12+ GB |
| Disk | 20 GB | 30+ GB |

> Vào Docker Desktop → Settings → Resources để điều chỉnh.

### 3. Maven (tùy chọn)

Dự án đã có **Maven Wrapper** (`./mvnw`) nên KHÔNG cần cài Maven riêng. Wrapper tự tải về Maven 3.9+ khi cần.

Kiểm tra Maven Wrapper hoạt động:
```bash
./mvnw --version
# Phải hiện: Apache Maven 3.9.x
```

## Yêu Cầu Hệ Điều Hành

| Hệ điều hành | Hỗ trợ |
|---|---|
| macOS 12+ | Được khuyến nghị |
| Ubuntu 20.04+ | Hỗ trợ đầy đủ |
| Windows 10/11 với WSL2 | Hỗ trợ, cần WSL2 |

## Kiểm Tra Nhanh Trước Khi Bắt Đầu

Chạy lệnh sau để kiểm tra tất cả:
```bash
echo "=== Java ===" && java -version
echo "=== Docker ===" && docker --version
echo "=== Docker Compose ===" && docker compose version
echo "=== Maven Wrapper ===" && ./mvnw --version
```

## Lưu Ý Về Tài Nguyên

Hệ thống bao gồm **26 service/container** khi chạy đầy đủ. Elasticsearch và Kafka tiêu tốn nhiều RAM nhất. Nếu máy yếu, có thể giảm bộ nhớ Elasticsearch trong `docker-compose.yml`:

```yaml
elasticsearch:
  environment:
    - "ES_JAVA_OPTS=-Xms128m -Xmx128m"  # Giảm từ 256m xuống 128m
```
