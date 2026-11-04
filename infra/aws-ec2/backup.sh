#!/bin/bash
# MediHistory AI — PostgreSQL backup script
# Run on the EC2 instance (or via cron).
#
# Cron example (daily at 2 AM):
#   0 2 * * * /opt/medihistory/backup.sh >> /var/log/medihistory-backup.log 2>&1
#
# Security: backup files contain patient data — store securely,
# do not copy to public storage without encryption.

set -euo pipefail

APP_DIR="/opt/medihistory"
BACKUP_DIR="/home/ubuntu/backups"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/medihistory_${DATE}.sql.gz"

# Keep last 7 days of backups
KEEP_DAYS=7

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Starting backup..."

# Dump PostgreSQL from running container, compress immediately
docker compose -f "${APP_DIR}/docker-compose.staging.yml" exec -T db \
    pg_dump -U medihistory_user medihistory_staging \
    | gzip > "${BACKUP_FILE}"

echo "[$(date)] Backup saved to: ${BACKUP_FILE}"
ls -lh "${BACKUP_FILE}"

# Remove backups older than KEEP_DAYS
find "${BACKUP_DIR}" -name "medihistory_*.sql.gz" -mtime "+${KEEP_DAYS}" -delete
echo "[$(date)] Cleaned up backups older than ${KEEP_DAYS} days"

echo "[$(date)] Backup complete."
