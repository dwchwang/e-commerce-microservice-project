#!/bin/bash
# verify-scale-eureka.sh — KIEM CHUNG: scale mot service co tao ra >=N instance Eureka voi id DUY NHAT.
#
# Day la script chay TRUOC khi demo (khong phai trong luc demo) de biet phuong an scale nao chay tren EC2.
# No tu thu lan luot cac file scale, voi moi file: scale len N -> doi Eureka -> dem instanceId duy nhat.
# Phuong an dau tien dat >=N la "winner". Cuoi cung dua service ve 1 container.
#
# Chay TREN EC2, trong /opt/ecommerce:
#   bash aws/verify-scale-eureka.sh                 # mac dinh product-service, N=2, thu ca 2 phuong an
#   bash aws/verify-scale-eureka.sh search-service 3
#   SCALE_FILE=docker-compose.scale.yml bash aws/verify-scale-eureka.sh product-service 2   # chi thu 1 file
#
# Ket qua: in PASS/FAIL cho tung phuong an + ket luan nen dung file nao cho scale-up.sh.
set -euo pipefail

SVC="${1:-product-service}"
N="${2:-2}"
APP="$(echo "$SVC" | tr '[:lower:]' '[:upper:]')"

PROJECT_DIR="${PROJECT_DIR:-/opt/ecommerce}"
ENV_FILE="${ENV_FILE:-.env.prod}"
WAIT_SECONDS="${WAIT_SECONDS:-90}"   # toi da cho instance register

cd "$PROJECT_DIR"

# Danh sach file scale se thu: neu SCALE_FILE duoc set thi chi thu file do.
if [ -n "${SCALE_FILE:-}" ]; then
  CANDIDATES=("$SCALE_FILE")
else
  CANDIDATES=(docker-compose.scale.yml docker-compose.scale-fallback.yml)
fi

# Doc Eureka credential tu .env.prod (mac dinh eureka/eureka neu khong khai bao).
read_env() { grep -E "^$1=" "$ENV_FILE" 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '"' | tr -d "'"; }
EUREKA_USER="$(read_env EUREKA_USER)"; EUREKA_USER="${EUREKA_USER:-eureka}"
EUREKA_PASSWORD="$(read_env EUREKA_PASSWORD)"; EUREKA_PASSWORD="${EUREKA_PASSWORD:-eureka}"
EUREKA_BASE="http://${EUREKA_USER}:${EUREKA_PASSWORD}@localhost:8761"

# Dem so instanceId DUY NHAT cua APP tren Eureka. In ra danh sach + tra ve count qua bien COUNT/IDS.
query_eureka() {
  local raw ids
  raw="$(curl -s "${EUREKA_BASE}/eureka/apps/${APP}" 2>/dev/null || true)"
  # Eureka tra XML mac dinh: moi instance co <instanceId>...</instanceId>.
  ids="$(printf '%s' "$raw" | grep -oE '<instanceId>[^<]*</instanceId>' | sed -E 's#</?instanceId>##g' | sort -u)"
  if [ -z "$ids" ]; then
    # Phong khi phien ban khong co instanceId tag: dung ipAddr (luon duy nhat moi container).
    ids="$(printf '%s' "$raw" | grep -oE '<ipAddr>[^<]*</ipAddr>' | sed -E 's#</?ipAddr>##g' | sort -u)"
  fi
  IDS="$ids"
  COUNT="$(printf '%s\n' "$ids" | grep -c . || true)"
}

scale_down_quiet() {
  bash aws/scale-down.sh "$SVC" >/dev/null 2>&1 || true
}

echo "================================================================"
echo " Verify scale Eureka: service=${SVC} target=${N} instance"
echo " Eureka: ${EUREKA_BASE/$EUREKA_PASSWORD/******}/eureka/apps/${APP}"
echo "================================================================"

WINNER=""
for f in "${CANDIDATES[@]}"; do
  if [ ! -f "$f" ]; then
    echo "[skip] khong thay $f"
    continue
  fi
  echo ""
  echo "---- Thu phuong an: $f ----"
  scale_down_quiet
  SCALE_FILE="$f" bash aws/scale-up.sh "$SVC" "$N" >/dev/null
  echo "Da scale -> ${N}. Cho register Eureka (toi da ${WAIT_SECONDS}s)..."

  COUNT=0; IDS=""
  for _ in $(seq 1 "$((WAIT_SECONDS/4))"); do
    query_eureka
    [ "${COUNT:-0}" -ge "$N" ] && break
    sleep 4
  done

  echo "instanceId duy nhat tren Eureka (${COUNT}):"
  printf '%s\n' "$IDS" | sed 's/^/    /'

  if [ "${COUNT:-0}" -ge "$N" ]; then
    echo "PASS: $f -> ${COUNT} instance id khac nhau (>=${N}). Scale that, tai se phan bo."
    WINNER="$f"
    break
  else
    echo "FAIL: $f -> chi ${COUNT} id (mong doi >=${N}). Eureka dang ghi de instance trung id."
  fi
done

echo ""
echo "=== Dua ${SVC} ve 1 container ==="
scale_down_quiet
bash aws/scale-down.sh "$SVC" >/dev/null 2>&1 || true

echo ""
echo "================================================================"
if [ -n "$WINNER" ]; then
  echo " KET LUAN: dung file scale = $WINNER"
  echo " -> Demo: SCALE_FILE=$WINNER bash aws/scale-up.sh ${SVC} 3"
  if [ "$WINNER" != "docker-compose.scale.yml" ]; then
    echo " (Hoac doi mac dinh: trong scale-up.sh/scale-down.sh set SCALE_FILE=$WINNER)"
  fi
else
  echo " KET LUAN: CA HAI phuong an deu khong tao instance-id duy nhat (>=${N})."
  echo " -> Dung PHUONG AN 3 (xem .guide/19-...md, muc 3.4.3): bo han EUREKA_INSTANCE_INSTANCE_ID,"
  echo "    chi de prefer-ip-address=true va dua vao instance-id mac dinh cua Spring Cloud."
  echo "    Neu van that bai: kiem tra Spring co resolve placeholder tu env khong (xem log service)."
fi
echo "================================================================"
