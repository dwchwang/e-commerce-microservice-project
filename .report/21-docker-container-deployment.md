# 21. Docker, Docker Compose & Container Deployment

## 1. Mục Tiêu Nghiên Cứu

- Hiểu container vs VM
- Hiểu Docker image, layer, registry
- Hiểu Dockerfile multi-stage build
- Hiểu Docker Compose — dev/test orchestration
- Phân biệt Compose vs Kubernetes

---

## 2. Lý Thuyết Cốt Lõi

### 2.1. Container vs Virtual Machine

| | VM | Container |
|---|----|-----------|
| Kernel | Riêng (full OS) | Share host kernel |
| Boot | Phút | Giây |
| Image size | GB | MB |
| Isolation | Cao (hypervisor) | Process-level (namespaces, cgroups) |
| Density | Vài VM/host | Hàng trăm container/host |

→ Container nhẹ hơn, deploy nhanh hơn — phù hợp microservice.

### 2.2. Docker Architecture

```
┌─────────────────────┐
│   Docker CLI        │   docker run, build, ps, ...
└──────────┬──────────┘
           │ REST API
┌──────────▼──────────┐
│  Docker Daemon      │   manage container lifecycle
│  (containerd, runc)  │
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   Linux Kernel      │   namespaces (PID, NET, MNT) + cgroups (CPU, RAM)
└─────────────────────┘
```

### 2.3. Image, Layer, Registry

- **Image**: Read-only template (filesystem snapshot + metadata)
- **Layer**: Each Dockerfile instruction creates a layer. Cached, shared
- **Container**: Running instance of image + writable layer
- **Registry**: Storage for images (Docker Hub, GCR, ECR, ...)

### 2.4. Dockerfile Best Practices

```dockerfile
# Multi-stage build for Java
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Best practices**:
- Multi-stage build → image production nhỏ (chỉ JRE, không JDK + Maven)
- `.dockerignore` — exclude `target/`, `.git/`, `node_modules/`
- Order layer: ít thay đổi trước, nhiều thay đổi sau (cache)
- Non-root user trong production
- Pin version: `eclipse-temurin:21-jre-alpine` not `latest`
- HEALTHCHECK directive

### 2.5. Docker Compose

**YAML định nghĩa multi-container app**:
```yaml
services:
  postgres:
    image: postgres:17-alpine
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck: {...}
  
  order-service:
    build: ./order-service
    depends_on:
      postgres: {condition: service_healthy}
      kafka: {condition: service_healthy}
    environment:
      SPRING_PROFILES_ACTIVE: docker
    ports:
      - "8086:8086"
volumes:
  postgres-data:
networks:
  default:
    driver: bridge
```

### 2.6. Networking

Docker network mặc định bridge. Trong network, container resolve nhau qua **service name** (DNS):
- `order-service` → `postgres` thông qua hostname `postgres`
- Kafka broker advertise `kafka:29092` cho internal, `localhost:9092` cho external

→ Trong dự án có 1 network `default`, 13 service + Postgres + Redis + Kafka + Elasticsearch + Keycloak + Prometheus + Grafana + Zipkin đều trong network này.

### 2.7. Volume

- **Bind mount**: `- ./host-path:/container-path` — sync với host (config files)
- **Named volume**: `- postgres-data:/var/lib/...` — managed by Docker (data persistence)
- **tmpfs**: in-memory only

→ Postgres, Kafka, Elasticsearch dùng named volume để persist data qua restart.

### 2.8. Compose vs Kubernetes

| | Docker Compose | Kubernetes |
|---|---------------|-----------|
| Scope | 1 host | Cluster nhiều node |
| Scaling | Manual (`--scale`) | Auto (HPA, KEDA) |
| Self-healing | Restart policy | Rolling, liveness/readiness |
| Networking | Single bridge | CNI plugins |
| Service mesh | n/a | Istio, Linkerd |
| Phù hợp | Dev/local/CI | Production |

→ Đồ án dùng Compose vì:
- Single host (laptop/server đơn)
- Đơn giản, demo nhanh
- Không cần overhead K8s

---

## 3. Cách Áp Dụng Trong Dự Án

### 3.1. docker-compose.yml structure

```
infrastructure:
  - postgres:5432
  - keycloak-db (postgres riêng)
  - keycloak:8180
  - redis:6379
  - kafka:9092 (KRaft mode)
  - elasticsearch:9200
  - mailpit:8025
  - zipkin:9411
  - prometheus:9090
  - grafana:3000

spring services:
  - config-server:8888 (start FIRST)
  - discovery-server:8761
  - api-gateway:8080  (the only exposed business endpoint)
  - 13 business services (8081-8093)
```

### 3.2. Healthcheck & depends_on

```yaml
order-service:
  depends_on:
    postgres: {condition: service_healthy}
    kafka: {condition: service_healthy}
    config-server: {condition: service_healthy}
    discovery-server: {condition: service_healthy}
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8086/actuator/health/liveness"]
    interval: 10s
    retries: 5
    start_period: 60s
```

→ Service chỉ bắt đầu sau khi dependencies healthy. `start_period` tránh false-positive trong khi service đang khởi động.

### 3.3. Environment & secrets

`.env` ở repo root, không commit (`.gitignore`):
```
POSTGRES_PASSWORD=...
REDIS_PASSWORD=...
KEYCLOAK_ADMIN_PASSWORD=...
VNPAY_TMN_CODE=...
VNPAY_HASH_SECRET=...
JWT_ISSUER_URI=http://keycloak:8080/realms/ecommerce
```

`.env.example` commit (template public).

### 3.4. Build & deploy

```bash
./mvnw clean package -DskipTests              # build all jars
docker compose up -d --build                   # start all
docker compose ps                              # status
docker compose logs -f order-service           # tail logs
docker compose down                            # stop all
docker compose down -v                         # + xóa volumes
```

### 3.5. Resource constraints (recommended)

```yaml
order-service:
  deploy:
    resources:
      limits:
        memory: 512M
        cpus: "0.5"
      reservations:
        memory: 256M
```

→ Tránh 1 service tham lam, OOMKill các container khác.

---

## 4. Production-Grade Tweaks (cho mục "Đề xuất phát triển")

- **Kubernetes**: Mỗi service Helm chart, autoscaling
- **Service Mesh**: Istio cho mTLS, traffic shifting, observability
- **CI/CD**: GitHub Actions / GitLab CI build image, push registry, deploy K8s
- **Secrets**: HashiCorp Vault, K8s sealed secrets
- **Image scanning**: Trivy, Snyk
- **Multi-arch image**: ARM (Mac M1) + x86

---

## 5. Từ Khóa Nghiên Cứu

```
- docker container vs vm
- dockerfile multi stage build
- docker compose dependency healthcheck
- kubernetes vs docker compose
- helm chart microservice
- service mesh istio linkerd
- twelve factor app processes
- container image security scanning
- distroless image gcr
- buildkit cache mount
```

---

## 6. Câu Hỏi Phản Biện

**Q1: Container có an toàn như VM không?**
→ Container share kernel → kernel vulnerability ảnh hưởng tất cả. VM isolation cao hơn nhưng nặng. Solution production: gVisor, Kata Containers (lightweight VM).

**Q2: Vì sao chọn Compose mà không Kubernetes?**
→ Đồ án single-host demo, K8s overhead không cần thiết. Compose đủ thực hành microservice patterns. Báo cáo có thể nêu K8s là future work.

**Q3: Multi-stage build có lợi gì?**
→ Image production chỉ cần JRE (~100MB) thay vì JDK + Maven (~700MB). Nhỏ hơn → push/pull nhanh hơn, attack surface ít hơn.

**Q4: Tại sao dùng Alpine?**
→ Alpine Linux ~5MB, eclipse-temurin:21-jre-alpine khoảng 200MB total. Trade-off: musl libc khác glibc, đôi khi gặp incompatibility (vd: native libraries cần glibc).

**Q5: Em mount source code vào container không?**
→ Production: KHÔNG. Build vào image. Dev: có thể bind mount để hot reload. Đồ án em build production-style.

**Q6: Networking giữa container hoạt động thế nào?**
→ Bridge network với DNS internal. Container `order-service` resolve `postgres` thành IP nội bộ qua `embedded-dns 127.0.0.11`. Không expose ports khi không cần.

**Q7: Volume khác bind mount?**
→ Volume managed by Docker, portable. Bind mount link tới host filesystem cụ thể. Volume cho data persistence (Postgres). Bind mount cho config file (Prometheus YAML).

---

## 7. Tài Liệu Tham Khảo

### Documentation
- docs.docker.com
- docs.docker.com/compose
- The Twelve-Factor App — 12factor.net

### Sách
- Adrian Mouat, *Using Docker*, O'Reilly
- Liz Rice, *Container Security*, O'Reilly
- Sébastien Goasguen, *Docker Cookbook*

### Best practices
- "Best practices for writing Dockerfiles" — Docker docs
- Google "distroless" base images project
- Sysdig Container Security Best Practices
