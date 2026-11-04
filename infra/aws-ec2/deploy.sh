#!/bin/bash
# MediHistory AI — deploy or update staging on EC2
#
# Usage (from your LOCAL machine):
#   chmod +x deploy.sh
#   ./deploy.sh YOUR_EC2_IP YOUR_KEY.pem
#
# What it does:
#   1. Rsync backend source to /opt/medihistory/backend/
#   2. Copies docker-compose.staging.yml to /opt/medihistory/
#   3. Rebuilds the backend Docker image
#   4. Restarts the backend container
#   5. Runs Alembic migrations
#   6. Checks /health endpoint

set -euo pipefail

EC2_IP="${1:?Usage: ./deploy.sh EC2_IP KEY.pem}"
KEY_FILE="${2:?Usage: ./deploy.sh EC2_IP KEY.pem}"
REMOTE="ubuntu@${EC2_IP}"
APP_DIR="/opt/medihistory"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "=== MediHistory AI Staging Deploy ==="
echo "Target: ${EC2_IP}"
echo "Repo:   ${REPO_ROOT}"
echo ""

# ── 1. Sync backend source ────────────────────────────────────────────────────
echo "[1/5] Syncing backend source..."
rsync -avz --progress \
    --exclude=".env" \
    --exclude="__pycache__" \
    --exclude=".pytest_cache" \
    --exclude=".mypy_cache" \
    --exclude=".ruff_cache" \
    --exclude="*.db" \
    --exclude="uploads/" \
    -e "ssh -i ${KEY_FILE} -o StrictHostKeyChecking=no" \
    "${REPO_ROOT}/backend/" \
    "${REMOTE}:${APP_DIR}/backend/"

# ── 2. Sync infra files ───────────────────────────────────────────────────────
echo "[2/5] Syncing docker-compose staging file..."
scp -i "${KEY_FILE}" \
    "${REPO_ROOT}/infra/aws-ec2/docker-compose.staging.yml" \
    "${REMOTE}:${APP_DIR}/docker-compose.staging.yml"

# ── 3. Rebuild Docker image on server ─────────────────────────────────────────
echo "[3/5] Rebuilding Docker image..."
ssh -i "${KEY_FILE}" "${REMOTE}" "cd ${APP_DIR} && docker compose -f docker-compose.staging.yml build backend"

# ── 4. Restart backend container ─────────────────────────────────────────────
echo "[4/5] Restarting backend..."
ssh -i "${KEY_FILE}" "${REMOTE}" "cd ${APP_DIR} && docker compose -f docker-compose.staging.yml up -d backend"

# ── 5. Run migrations ─────────────────────────────────────────────────────────
echo "[5/5] Running database migrations..."
ssh -i "${KEY_FILE}" "${REMOTE}" \
    "cd ${APP_DIR} && docker compose -f docker-compose.staging.yml exec -T backend alembic upgrade head"

# ── Health check ──────────────────────────────────────────────────────────────
echo ""
echo "Checking /health..."
sleep 3
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "https://YOUR_DOMAIN/health" 2>/dev/null || echo "000")
if [ "${HTTP_CODE}" = "200" ]; then
    echo "✅ /health → 200 OK"
else
    echo "⚠️  /health → ${HTTP_CODE} (may still be starting)"
fi

echo ""
echo "=== Deploy complete ==="
echo "Logs: ssh -i ${KEY_FILE} ${REMOTE} 'cd ${APP_DIR} && docker compose -f docker-compose.staging.yml logs -f backend'"
