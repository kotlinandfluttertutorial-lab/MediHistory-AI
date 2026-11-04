# MediHistory AI — Lightweight AWS EC2 Staging Deployment

**Target:** EC2 t3.micro + Docker Compose + RDS t3.micro PostgreSQL  
**Estimated cost:** ~$5–15/month (Free Tier eligible for 12 months on new accounts)  
**Use case:** Developer staging only — NOT for real patient data until security review passes

## Architecture

```
Internet
    │ 443 (HTTPS)
    ▼
EC2 t3.micro (Ubuntu 22.04)
  ├── Nginx (reverse proxy, Let's Encrypt SSL)
  │     └── proxy_pass → localhost:8000
  ├── FastAPI container (port 8000, internal)
  └── PostgreSQL 16 container (port 5432, internal only)

Security group: 80, 443, 22 inbound only
```

> **Free Tier note:** t3.micro is NOT Free Tier eligible — use t2.micro (also 1 vCPU / 1 GB RAM) for Free Tier.

---

## Prerequisites

- AWS account with CLI configured (`aws configure`)
- A domain name pointing to the EC2 public IP (or use the EC2 public DNS for HTTP-only testing)
- SSH key pair created in EC2 console

---

## Step-by-step deployment

### 1. Launch EC2 instance

```bash
# From your local machine
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \  # Ubuntu 22.04 LTS (us-east-1) — update for your region
  --instance-type t3.micro \
  --key-name YOUR_KEY_PAIR_NAME \
  --security-group-ids YOUR_SG_ID \
  --subnet-id YOUR_SUBNET_ID \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=medihistory-staging}]'
```

Or launch from the AWS Console:
- AMI: Ubuntu Server 22.04 LTS (HVM)
- Instance type: t3.micro (or t2.micro for Free Tier)
- Storage: 20 GB gp3
- Security group: see `security-group.json`

### 2. Configure security group

Allow inbound:
| Port | Protocol | Source | Purpose |
|------|----------|--------|---------|
| 22   | TCP | Your IP only | SSH |
| 80   | TCP | 0.0.0.0/0 | HTTP (redirects to HTTPS) |
| 443  | TCP | 0.0.0.0/0 | HTTPS |

### 3. SSH in and run setup script

```bash
# Copy files to server
scp -r infra/aws-ec2/ ubuntu@YOUR_EC2_IP:~/medihistory-deploy/
scp backend/ ubuntu@YOUR_EC2_IP:~/medihistory-deploy/backend/

# SSH in
ssh -i your-key.pem ubuntu@YOUR_EC2_IP

# Run setup
cd ~/medihistory-deploy
chmod +x setup.sh
./setup.sh
```

### 4. Set environment variables

```bash
cd ~/medihistory-deploy
cp .env.staging .env
nano .env  # Fill in all CHANGE_ME values
```

### 5. Start services

```bash
docker compose -f docker-compose.staging.yml up -d
```

### 6. Run database migrations

```bash
docker compose -f docker-compose.staging.yml exec backend alembic upgrade head
```

### 7. Configure SSL with Let's Encrypt

```bash
sudo certbot --nginx -d your-domain.com
```

### 8. Verify

```bash
curl https://your-domain.com/health
# Expected: {"status":"ok"}
```

---

## Maintenance

### View logs
```bash
docker compose -f docker-compose.staging.yml logs -f backend
```

### Update deployment
```bash
git pull
docker compose -f docker-compose.staging.yml build backend
docker compose -f docker-compose.staging.yml up -d backend
docker compose -f docker-compose.staging.yml exec backend alembic upgrade head
```

### Backup database
```bash
./backup.sh  # Saves to ~/backups/medihistory_YYYYMMDD.sql
```

---

## Security reminders for staging

- [ ] Use synthetic test data only — no real patient records
- [ ] Rotate `JWT_SECRET_KEY` before any real use
- [ ] Security group limits SSH to your IP only
- [ ] Access logs disabled in Dockerfile (`--no-access-log`)
- [ ] PostgreSQL not exposed to internet (internal Docker network only)
- [ ] Upload directory on Docker volume (not accessible from internet)
