#!/bin/bash
# scale-down.sh — Dua MOT service ve 1 container voi container_name goc (trang thai truoc khi scale).
#
# Chay TREN EC2, trong /opt/ecommerce.
#   bash aws/scale-down.sh product-service
#   SCALE_FILE=docker-compose.scale-fallback.yml bash aws/scale-down.sh product-service
#
# Cach lam (deterministic, tranh container mo coi):
#   1) Stop + remove TAT CA instance cua service (du dang ten <project>-<svc>-1/-2/-3) bang `rm -sf`,
#      dung bo compose CO file scale (vi cac replica duoc tao voi cau hinh do).
#   2) Tao lai service tu bo compose KHONG co file scale -> quay ve 1 container ten goc
#      `ecommerce-<svc>` voi cau hinh production binh thuong.
#
# Buoc 1 khien service gian doan vai giay (chap nhan duoc — day la thao tac ket thuc demo).
set -euo pipefail

SVC="${1:?Thieu ten service. Vi du: bash aws/scale-down.sh product-service}"

PROJECT_DIR="${PROJECT_DIR:-/opt/ecommerce}"
ENV_FILE="${ENV_FILE:-.env.prod}"
SCALE_FILE="${SCALE_FILE:-docker-compose.scale.yml}"

cd "$PROJECT_DIR"

SCALE_ARGS=(-f docker-compose.yml -f docker-compose.prod.yml)
[ -f "$SCALE_FILE" ] && SCALE_ARGS+=(-f "$SCALE_FILE")

COMPOSE_SCALE=(docker compose --env-file "$ENV_FILE" "${SCALE_ARGS[@]}")
COMPOSE_BASE=(docker compose --env-file "$ENV_FILE"
  -f docker-compose.yml -f docker-compose.prod.yml)

echo "=== Go bo tat ca instance cua ${SVC} ==="
"${COMPOSE_SCALE[@]}" rm -sf "${SVC}" || true

echo ""
echo "=== Tao lai ${SVC} ve 1 container ten goc ==="
"${COMPOSE_BASE[@]}" up -d --no-build "${SVC}"

echo ""
echo "=== Container ${SVC} ==="
"${COMPOSE_BASE[@]}" ps "${SVC}"

APP="$(echo "${SVC}" | tr '[:lower:]' '[:upper:]')"
echo ""
echo "Da ve 1 instance. Kiem tra Eureka chi con 1 dong ${APP}"
echo "va container ten 'ecommerce-${SVC}' o trang thai Up/healthy."
