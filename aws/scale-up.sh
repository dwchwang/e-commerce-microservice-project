#!/bin/bash
# scale-up.sh — Scale ngang MOT service bang tay de demo tai phan bo lai khi nhin Grafana.
#
# Chay TREN EC2, trong /opt/ecommerce.
#   bash aws/scale-up.sh product-service 3
#   bash aws/scale-up.sh search-service 2
#   SCALE_FILE=docker-compose.scale-fallback.yml bash aws/scale-up.sh product-service 3   # phuong an 2
#
# Yeu cau service phai duoc khai bao trong file scale (mac dinh docker-compose.scale.yml): bo
# container_name + instance-id duy nhat. Neu khong, Docker bao trung container_name hoac Eureka chi
# thay 1 instance.
#
# Ghi chu: dung --force-recreate de ca N replica deu chay cung cau hinh scale (instance-id duy nhat).
# Service co ~5-10s gian doan luc recreate -> binh thuong, con minh hoa duoc recovery tren Grafana.
# Cac service KHAC khong bi dung.
set -euo pipefail

SVC="${1:?Thieu ten service. Vi du: bash aws/scale-up.sh product-service 3}"
N="${2:-3}"

PROJECT_DIR="${PROJECT_DIR:-/opt/ecommerce}"
ENV_FILE="${ENV_FILE:-.env.prod}"
SCALE_FILE="${SCALE_FILE:-docker-compose.scale.yml}"

cd "$PROJECT_DIR"

if [ ! -f "$SCALE_FILE" ]; then
  echo "ERROR: khong thay $SCALE_FILE trong $PROJECT_DIR. Deploy file scale len EC2 truoc." >&2
  exit 1
fi

COMPOSE=(docker compose --env-file "$ENV_FILE"
  -f docker-compose.yml -f docker-compose.prod.yml -f "$SCALE_FILE")

# Canh bao neu service chua duoc khai bao (active, khong bi comment) trong file scale.
if ! grep -qE "^[[:space:]]{2}${SVC}:[[:space:]]*$" "$SCALE_FILE"; then
  echo "CANH BAO: '${SVC}' chua duoc bat trong $SCALE_FILE (co the dang bi comment)." >&2
  echo "          Scale van chay nhung instance-id co the trung -> Eureka chi thay 1 instance." >&2
fi

echo "=== Scale ${SVC} -> ${N} replica (scale file: $SCALE_FILE) ==="
"${COMPOSE[@]}" up -d --no-build --force-recreate --scale "${SVC}=${N}" "${SVC}"

echo ""
echo "=== Container ${SVC} ==="
"${COMPOSE[@]}" ps "${SVC}"

APP="$(echo "${SVC}" | tr '[:lower:]' '[:upper:]')"
echo ""
echo "Cho ~30-60s cho cac instance moi register Eureka, sau do kiem chung:"
echo "  - Eureka:     http://\$(hostname -I | awk '{print \$1}'):8761  (phai thay ${N} dong ${APP})"
echo "  - Prometheus: http://<ec2-ip>:9090/targets  (phai thay ${N} target ${SVC})"
echo "  - Grafana:    Spring Boot Overview, tach 'by (instance)' -> tai chia cho ${N}, p95 giam"
echo "  - Tu dong:    bash aws/verify-scale-eureka.sh ${SVC} ${N}"
echo ""
echo "Khi xong demo, dua ve 1 container: bash aws/scale-down.sh ${SVC}"
