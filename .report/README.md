# Bo Tai Lieu .report De Viet Bao Cao DATN

Folder `.report` la bo ghi chu nghien cuu va bien tap noi dung cho bao cao do an. Sau Phase 10-14, folder nay nen duoc dung theo 3 lop:

1. **Lop ly thuyet/pattern**: file `01` den `21`, dung de viet Chuong 2 va giai thich cac quyet dinh thiet ke.
2. **Lop phase moi va bang chung**: file `22` va `23`, dung de dua phase 10-14, so lieu k6, smoke test, CI/CD, AWS va cac han che vao Chuong 5-6.
3. **Lop cau truc bao cao**: file `24`, dung de map toan bo noi dung truoc do vao 6 chuong cua bao cao chinh thuc.

## Cach Doc Nhanh

| Nhu cau | Doc file |
|---|---|
| Viet Chuong 1: tong quan va dong gop | `00`, `01`, `22`, `24` |
| Viet Chuong 2: co so ly thuyet | `01` den `21`, uu tien `09`, `10`, `11`, `12`, `13`, `14` |
| Viet Chuong 3: giai phap cong nghe | `02`, `03`, `04`, `05`, `06`, `08`, `14`, `15`, `16`, `17`, `18`, `19`, `21`, `22` |
| Viet Chuong 4: phan tich va thiet ke | `01`, `08`, `09`, `10`, `12`, `13`, `14`, `24` |
| Viet Chuong 5: cai dat va trien khai | `05`, `10`, `12`, `13`, `14`, `17`, `18`, `21`, `22` |
| Viet Chuong 6: kiem thu va danh gia | `20`, `22`, `23` |
| Chuan bi bao ve | `00`, cac muc "Cau hoi phan bien", `22`, `23`, `24` |

## Diem Moi Sau Phase 10-14

| Phase | Ket qua nen dua vao bao cao | Nguon bang chung |
|---|---|---|
| 10 - AWS Infrastructure | He thong deploy single-host EC2 bang Docker Compose production override, Caddy/HTTPS, CORS env-driven, JVM heap cap | `.codex/plan/phase-10-aws-infrastructure.md`, `docker-compose.prod.yml`, `api-gateway/src/main/resources/application.yml` |
| 11 - CI/CD | GitHub Actions build/push GHCR va deploy len EC2 qua workflow manual | `.codex/plan/phase-11-cicd-github-actions.md`, `.github/workflows/build-and-push.yml`, `.github/workflows/deploy.yml` |
| 12 - Storefront FE | Next.js storefront cho catalog/search/cart/checkout/orders/flash-sale, deploy chung frontend container | `.codex/plan/phase-12-frontend-nextjs.md`, `frontend/` |
| 13 - Performance/Resilience | Co so lieu k6 that tren AWS: catalog soak, checkout stress, flash-sale spike, resilience scripts | `.test/results/SUMMARY.md`, `.docs/08-testing-and-evaluation.md`, `.test/load/`, `.test/chaos/` |
| 14 - Admin Panel | Admin FE la phan con lai can hoan thien; backend/admin endpoint va ROLE_ADMIN la nen tang tich hop | `.codex/plan/phase-14-admin-panel.md` |

## Nguyen Tac Copy Vao Bao Cao

- Chi ghi "da dat" khi co artifact trong `.test/results`, workflow, source code hoac screenshot.
- Khong copy so lieu neu khong co raw file di kem. File `23-bang-chung-kiem-thu-va-so-lieu.md` la danh muc kiem tra truoc khi dua vao Chuong 6.
- Cac muc con thieu bang chung phai ghi la "chua ket luan", "inconclusive", hoac "huong bo sung".
- Tranh noi chung chung. Moi pattern quan trong nen co 3 phan: ly thuyet, code ap dung, ket qua/thuc nghiem.

## Thu Tu Bien Tap Khuyen Nghi

1. Viet khung 6 chuong theo `.docs/00-de-cuong-bao-cao-datn.md` va `.report/24-cau-truc-bao-cao-datn.md`.
2. Dua ly thuyet tu `01` den `21` vao Chuong 2, chi giu cac phan lien quan truc tiep den he thong.
3. Dua AWS/CI/CD/Frontend tu `22` vao Chuong 5.
4. Dua so lieu k6, smoke test va resilience tu `23` vao Chuong 6.
5. Dua cac han che trung thuc: single-host, Kafka outbox replay chua verify, Redis cart probe inconclusive, Grafana/Zipkin screenshot con can capture, admin FE con lai.
