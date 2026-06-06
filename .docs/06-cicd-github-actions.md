# 5.x. CI/CD Pipeline với GitHub Actions

Tài liệu này mô tả quy trình CI/CD tự động build 16 Spring Boot service images, build frontend image, push lên GitHub Container Registry (GHCR), và deploy lên AWS EC2 bằng một click manual.

---

## 5.x.1. Tổng quan pipeline

```
┌─────────────────────────────────────────────────────────────┐
│  Developer Push to main                                     │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  GitHub Actions: build-and-push.yml                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Job 1: build-maven                                  │   │
│  │  • Maven build all services (parallel -T 1C)        │   │
│  │  • Upload 16 JAR artifacts                          │   │
│  └──────────────────────────────────────────────────────┘   │
│                 │                                            │
│                 ▼                                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Job 2: build-images (matrix 16x)                   │   │
│  │  • Download JAR artifacts                           │   │
│  │  • Docker build with Dockerfile.prod                │   │
│  │  • Push to ghcr.io/owner/service:sha,latest        │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  GHCR: ghcr.io/<owner>/<service>:latest                      │
└────────────────┬────────────────────────────────────────────┘
                 │
                 │ (Manual trigger)
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  GitHub Actions: deploy.yml                                 │
│  • Requires approval (production environment)               │
│  • SSH to EC2                                               │
│  • git pull latest compose files                            │
│  • docker compose pull (with IMAGE_TAG input)               │
│  • docker compose up -d (rolling restart)                   │
└────────────────┬────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────┐
│  EC2: Stack running with new images                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 5.x.2. Workflow files

### 5.x.2.1. build-and-push.yml

**Trigger:**
- Push to `main` branch với path filter (`**/pom.xml`, `**/src/**`, `**/Dockerfile.prod`)
- Manual `workflow_dispatch`

**Jobs:**

**Job 1: build-maven**
- Setup Java 21 (Temurin) với Maven cache
- Build tất cả services: `./mvnw -B -T 1C clean package -DskipTests`
- Upload 16 JAR artifacts với retention 1 day

**Job 2: build-images (matrix 16x)**
- Download JAR artifacts từ job 1
- Login GHCR với `secrets.GITHUB_TOKEN`
- Build Docker image với `Dockerfile.prod` (pre-built JAR pattern)
- Push với 3 tags: `<sha-short>`, `main`, `latest`
- Cache Docker layers với GitHub Actions cache

**Cache strategy:**
- Maven dependencies: `~/.m2` cached by `actions/setup-java@v4`
- Docker layers: `type=gha,scope=${{ matrix.service }}`

**Build time:**
- Cold build (no cache): ~15 phút
- Warm build (with cache): ~5 phút

---

### 5.x.2.2. deploy.yml

**Trigger:**
- Manual `workflow_dispatch` với input `image_tag` (default: `latest`)

**Environment:**
- `production` environment với required reviewer approval
- Deployment branch restriction: `main` only

**Steps:**
1. SSH vào EC2 qua `appleboy/ssh-action@v1`
2. `git reset --hard origin/main` để pull compose files mới nhất
3. Login GHCR với `GHCR_PULL_TOKEN`
4. `docker compose pull` với `IMAGE_TAG` từ input
5. `docker compose up -d --no-build --remove-orphans` (rolling restart)
6. `docker image prune -af --filter "until=72h"` cleanup old images
7. Wait 30s và report `docker compose ps`

**Rollback:**
- Trigger deploy.yml với `image_tag=<previous-sha>`
- Compose sẽ pull image cũ và restart

---

### 5.x.2.3. build-frontend.yml

**Trigger:**
- Push to `main` với path filter `frontend/**`
- Manual `workflow_dispatch`

**Build args:**
- `NEXT_PUBLIC_API_BASE_URL` từ GitHub Secret (baked at build time)

**Note:** Workflow này chỉ active sau Phase 12 khi `frontend/` directory tồn tại.

---

## 5.x.3. GitHub Secrets & Environment

### 5.x.3.1. Required Secrets

| Secret | Value | Purpose |
|---|---|---|
| `EC2_HOST` | Elastic IP từ `aws/config.env` (`ELASTIC_IP`) | SSH target cho deploy |
| `EC2_SSH_KEY` | Private key content (`~/.ssh/aws-ecommerce`) | SSH authentication |
| `GHCR_USERNAME` | GitHub username hoặc organization owner | Docker login trên EC2 |
| `GHCR_PULL_TOKEN` | PAT classic với scope `read:packages` | Pull images từ GHCR trên EC2 |
| `NEXT_PUBLIC_API_BASE_URL` | `https://api.<ip>.nip.io` | Frontend build arg |

**Built-in secrets:**
- `secrets.GITHUB_TOKEN` — auto-generated, dùng cho push GHCR trong workflow (không cần tạo thủ công)

### 5.x.3.2. Environment "production"

**Protection rules:**
- Required reviewers: 1 reviewer (`<reviewer-or-owner>`)
- Deployment branches: `main` only

**Workflow:**
1. Developer trigger deploy.yml
2. GitHub pause và gửi notification cho reviewer
3. Reviewer approve/reject trên GitHub UI
4. Nếu approve → workflow tiếp tục SSH vào EC2

---

## 5.x.4. Docker image tags strategy

Mỗi push tạo 3 tags:

| Tag | Format | Purpose |
|---|---|---|
| SHA short | `a1b2c3d` | Immutable, dùng cho rollback chính xác |
| Branch | `main` | Latest trên branch main |
| Latest | `latest` | Alias cho main branch |

**Deploy strategy:**
- Default: `image_tag=latest` (luôn pull bản mới nhất)
- Rollback: `image_tag=<sha>` (pull bản cụ thể)

**Ví dụ:**
```bash
# Deploy latest
gh workflow run deploy.yml -f image_tag=latest

# Rollback về commit a1b2c3d
gh workflow run deploy.yml -f image_tag=a1b2c3d
```

---

## 5.x.5. Production Dockerfile pattern

**Dockerfile.prod** (khác với `Dockerfile` local dev):

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Khác biệt với Dockerfile local:**
- Không chạy Maven trong container
- Expect JAR đã build sẵn trong `target/`
- Thêm `wget` cho healthcheck (Alpine base không có)
- Nhẹ hơn, build nhanh hơn

**Local dev:** `docker compose build` dùng `Dockerfile` (multi-stage với Maven)  
**CI/CD:** GitHub Actions dùng `Dockerfile.prod` (single-stage, pre-built JAR)

---

## 5.x.6. Xử lý sự cố thường gặp

| Vấn đề | Nguyên nhân | Cách khắc phục |
|---|---|---|
| Maven build OOM trên runner | Default heap quá nhỏ | Thêm `MAVEN_OPTS: "-Xmx2g"` vào workflow env |
| Docker rate limit khi pull base image | Anonymous pull limit 100/6h | Login Docker Hub trước build: `docker login` |
| SSH timeout khi deploy | EC2 stopped hoặc Security Group chặn | Verify instance running, SG port 22 mở cho GitHub Actions IPs |
| Image pull fail trên EC2 | `GHCR_PULL_TOKEN` expired hoặc sai scope | Regenerate PAT với scope `read:packages`, update secret |
| Deploy stuck "Waiting for approval" | Reviewer chưa approve | Check GitHub Actions tab → pending deployment → Review |
| Compose pull wrong tag | `IMAGE_TAG` env không set | Verify workflow input `image_tag` passed correctly |

---

## 5.x.7. Monitoring & Logs

**GitHub Actions UI:**
- Repo → Actions tab → chọn workflow run
- View logs real-time, download artifacts

**EC2 logs:**
```bash
ssh ubuntu@<elastic-ip>
docker compose logs -f --tail=100 api-gateway
docker compose ps  # Check container status
```

**GHCR packages:**
```bash
gh api /users/<owner>/packages/container/<service>/versions
# List all image versions với tags
```

---

## 5.x.8. Best practices

1. **Luôn test local trước khi push:**
   ```bash
   ./mvnw clean package -DskipTests
   docker build -f api-gateway/Dockerfile.prod -t test api-gateway/
   ```

2. **Commit message conventions:**
   - `feat:` → trigger build
   - `fix:` → trigger build
   - `docs:` → skip build (không match path filter)

3. **Deploy trong giờ thấp điểm:**
   - Tránh deploy giữa giờ cao điểm demo
   - Có rollback plan sẵn

4. **Monitor sau deploy:**
   - Check Eureka dashboard: tất cả services UP
   - Check API health: `curl https://api.<ip>.nip.io/actuator/health`
   - Check Grafana metrics: latency, error rate

---

## 5.x.9. Danh sách file liên quan

| File | Purpose |
|---|---|
| `.github/workflows/build-and-push.yml` | Build Maven + push 16 images |
| `.github/workflows/deploy.yml` | Manual deploy lên EC2 |
| `.github/workflows/build-frontend.yml` | Build frontend image (Phase 12) |
| `*/Dockerfile.prod` | Production Dockerfile (16 files) |
| `aws/config.env` | AWS resource IDs (gitignored) |
| `.env.prod` | Production env vars trên EC2 (gitignored) |

---

## 5.x.10. Verification checklist

- [ ] GitHub CLI installed: `gh auth status`
- [ ] PAT created với scope `read:packages`
- [ ] Environment `production` configured với reviewer
- [ ] 5 secrets configured: `gh secret list`
- [ ] Push code → build-and-push.yml runs successfully
- [ ] 16 images pushed to GHCR: `gh api /users/<owner>/packages`
- [ ] Manual deploy → approval required → SSH success → stack running
- [ ] Rollback test: deploy với old SHA → stack reverted
