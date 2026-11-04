#!/bin/bash
# MediHistory AI — EC2 staging server setup script
# Run once on a fresh Ubuntu 22.04 instance.
#
# Usage:
#   chmod +x setup.sh
#   sudo ./setup.sh
#
# Security:
# - Installs Docker, Nginx, Certbot, UFW
# - Configures UFW firewall (22/80/443 only)
# - Does NOT store any secrets

set -euo pipefail

echo "=== MediHistory AI — Staging Server Setup ==="
echo "Running on: $(lsb_release -d | cut -f2)"

# ── 1. System update ──────────────────────────────────────────────────────────
echo "[1/7] Updating system packages..."
apt-get update -q
apt-get upgrade -y -q

# ── 2. Install Docker ─────────────────────────────────────────────────────────
echo "[2/7] Installing Docker..."
apt-get install -y -q \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -q
apt-get install -y -q docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Add ubuntu user to docker group so we don't need sudo
usermod -aG docker ubuntu

# ── 3. Install Nginx ──────────────────────────────────────────────────────────
echo "[3/7] Installing Nginx..."
apt-get install -y -q nginx

# ── 4. Install Certbot (Let's Encrypt) ───────────────────────────────────────
echo "[4/7] Installing Certbot..."
apt-get install -y -q certbot python3-certbot-nginx

# ── 5. Configure UFW firewall ─────────────────────────────────────────────────
echo "[5/7] Configuring UFW firewall..."
ufw --force reset
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh          # 22
ufw allow 'Nginx Full' # 80 + 443
ufw --force enable
ufw status

# ── 6. Create backup directory ────────────────────────────────────────────────
echo "[6/7] Creating backup directory..."
mkdir -p /home/ubuntu/backups
chown ubuntu:ubuntu /home/ubuntu/backups

# ── 7. Create app directory ───────────────────────────────────────────────────
echo "[7/7] Setting up app directory..."
mkdir -p /opt/medihistory
chown ubuntu:ubuntu /opt/medihistory

# ── systemd service for auto-restart ─────────────────────────────────────────
cat > /etc/systemd/system/medihistory.service << 'EOF'
[Unit]
Description=MediHistory AI Backend
After=docker.service
Requires=docker.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/medihistory
ExecStart=/usr/bin/docker compose -f docker-compose.staging.yml up
ExecStop=/usr/bin/docker compose -f docker-compose.staging.yml down
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable medihistory.service

echo ""
echo "=== Setup complete! ==="
echo ""
echo "Next steps:"
echo "  1. Copy your app files to /opt/medihistory/"
echo "  2. cd /opt/medihistory && cp .env.staging .env && nano .env"
echo "  3. docker compose -f docker-compose.staging.yml up -d"
echo "  4. docker compose -f docker-compose.staging.yml exec backend alembic upgrade head"
echo "  5. sudo certbot --nginx -d your-domain.com"
echo ""
echo "IMPORTANT: Restrict SSH (port 22) to your IP in the EC2 security group!"
