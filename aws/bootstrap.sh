#!/bin/bash
set -euo pipefail

# ============================================================
# bootstrap.sh — Run ONCE on fresh EC2 Ubuntu 24.04 instance.
# Copies itself to EC2 and runs via SSH:
#   scp -i $SSH_KEY_PATH aws/bootstrap.sh ubuntu@$ELASTIC_IP:~/bootstrap.sh
#   ssh -i $SSH_KEY_PATH ubuntu@$ELASTIC_IP 'bash ~/bootstrap.sh'
# ============================================================

echo "=== [1/7] Updating apt packages ==="
sudo apt-get update -y

echo "=== [2/7] Installing core dependencies ==="
sudo apt-get install -y ca-certificates curl gnupg lsb-release git unzip jq

echo "=== [3/7] Installing Docker Engine + Compose plugin ==="
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list
sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io \
  docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu

echo "=== [4/7] Setting up 4GB swap file ==="
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf

echo "=== [5/7] Installing Caddy web server ==="
sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | \
  sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | \
  sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt-get update -y
sudo apt-get install -y caddy

echo "=== [6/7] Creating app directory ==="
sudo mkdir -p /opt/ecommerce
sudo chown ubuntu:ubuntu /opt/ecommerce

echo "=== [7/7] Verifying installations ==="
echo ""
echo "Docker version: $(docker --version 2>/dev/null || echo 'NOT FOUND — logout & login again for group生效')"
echo "Docker Compose: $(docker compose version 2>/dev/null || echo 'NOT FOUND')"
echo "Caddy version: $(caddy version 2>/dev/null || echo 'NOT FOUND')"
echo "RAM: $(free -h | grep Mem | awk '{print $2}')"
echo "Disk: $(df -h / | tail -1 | awk '{print $2}')"
echo "CPU cores: $(nproc)"
echo "Swap: $(free -h | grep Swap | awk '{print $2}')"
echo ""
echo "========================================"
echo " Bootstrap complete!"
echo " Next steps:"
echo "  1. Logout & login again (docker group生效)"
echo "  2. Clone repo to /opt/ecommerce"
echo "  3. Copy .env.prod to /opt/ecommerce/.env.prod"
echo "  4. Copy aws/Caddyfile to /etc/caddy/Caddyfile"
echo "  5. sudo systemctl enable caddy && sudo systemctl start caddy"
echo "  6. Run aws/start-stack.sh"
echo "========================================"
