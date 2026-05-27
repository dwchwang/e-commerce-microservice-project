#!/bin/bash
set -euo pipefail

# ============================================================
# start-stack.sh — Start the full e-commerce stack on EC2.
# Deploy to /opt/ecommerce/aws/start-stack.sh
# Run: bash aws/start-stack.sh
#
# Prerequisites:
#   - .env.prod exists in /opt/ecommerce/
#   - docker + docker compose plugin installed
#   - ghcr.io images are accessible (public or logged in)
# ============================================================

cd /opt/ecommerce

if [ ! -f .env.prod ]; then
  echo "ERROR: .env.prod not found in /opt/ecommerce/"
  echo "Copy .env.prod.example to .env.prod and fill real values first."
  exit 1
fi

echo "=== Pulling latest images from GHCR ==="
docker compose --env-file .env.prod \
  -f docker-compose.yml -f docker-compose.prod.yml \
  pull

echo ""
echo "=== Starting all services ==="
docker compose --env-file .env.prod \
  -f docker-compose.yml -f docker-compose.prod.yml \
  up -d --no-build

echo ""
echo "=== Waiting for services to initialize (30s) ==="
sleep 30

echo ""
echo "=== Container status ==="
docker compose ps

echo ""
echo "========================================"
echo " Stack started."
echo " Health check: curl -s https://api.\${ELASTIC_IP_DASHED}.nip.io/actuator/health"
echo " Eureka:       http://\$(hostname -I | awk '{print \$1}'):8761"
echo " Keycloak:     https://auth.\${ELASTIC_IP_DASHED}.nip.io"
echo "========================================"
