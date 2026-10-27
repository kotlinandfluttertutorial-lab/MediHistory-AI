# MediHistory AI — Environment Setup Guide

This guide covers local development setup, CI/CD environment variables, and production deployment.

---

## Local Development Setup

### Prerequisites

| Tool | Required Version | Notes |
|------|-----------------|-------|
| Android Studio | Hedgehog (2023.1.1) or later | Koala or later recommended |
| JDK | 17 (Temurin) | Set `JAVA_HOME` to JDK 17 |
| Python | 3.12 | Use `pyenv` or the official installer |
| Git | Any recent version | |

### Android Setup

1. Clone the repository:
   ```
   git clone https://github.com/kotlinandfluttertutorial-lab/MediHistory-AI.git
   cd MediHistory-AI
   ```

2. Create `android/local.properties` (this file is git-ignored and must never be committed):
   ```properties
   sdk.dir=/path/to/your/Android/Sdk

   # Backend URL — leave empty to run the app in offline-only mode
   backend.base.url=http://10.0.2.2:8000

   # Feature flags — set to true only when the backend is running locally
   enable.cloud.backup=false
   enable.interaction.check=false
   ```

3. Open the `android/` folder in Android Studio. Gradle will sync automatically.

4. Run the app on an emulator or connected device. The app functions offline without a backend.

### Backend Setup

1. Navigate to the backend directory:
   ```
   cd backend
   ```

2. Create a Python virtual environment and install dependencies:
   ```
   python -m venv .venv
   source .venv/bin/activate        # Windows: .venv\Scripts\activate
   pip install -r requirements.txt
   ```

3. Create a `.env` file in `backend/` (git-ignored, never commit):
   ```env
   DATABASE_URL=sqlite:///./medihistory_dev.db
   JWT_SECRET_KEY=local-dev-secret-change-before-production
   ENVIRONMENT=development
   ```
   > **Never use a production `JWT_SECRET_KEY` in local development.**

4. Run database migrations:
   ```
   alembic upgrade head
   ```

5. Start the development server:
   ```
   uvicorn app.main:app --reload --port 8000
   ```

6. The API is available at `http://localhost:8000`. Interactive docs: `http://localhost:8000/docs`.

---

## CI/CD Environment Variables

These variables are configured as GitHub Actions secrets in repository settings. They must never be committed to any source file.

### Android CI Variables

| Variable | Purpose | Example (CI only) |
|----------|---------|------------------|
| `BACKEND_BASE_URL` | Backend base URL injected into BuildConfig | `""` (empty for unit tests) |
| `ENABLE_CLOUD_BACKUP` | Cloud backup feature flag | `"false"` |
| `ENABLE_INTERACTION_CHECK` | Interaction check feature flag | `"false"` |

### Backend CI Variables

| Variable | Purpose | CI Value |
|----------|---------|---------|
| `DATABASE_URL` | Database connection string | `sqlite:///:memory:` |
| `JWT_SECRET` | JWT signing secret for tests | CI-only throwaway value |
| `TESTING` | Enables test mode in the app | `"true"` |

### Production Secrets (GitHub Actions secrets — never in source)

| Secret Name | Purpose |
|-------------|---------|
| `PROD_JWT_SECRET_KEY` | Production JWT signing secret (256-bit minimum) |
| `PROD_DATABASE_URL` | Production PostgreSQL connection string |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded production keystore |
| `ANDROID_KEY_ALIAS` | Keystore key alias |
| `ANDROID_KEY_PASSWORD` | Key password |
| `ANDROID_STORE_PASSWORD` | Keystore password |

---

## Production Deployment Checklist

Before deploying the backend to production:

- [ ] `JWT_SECRET_KEY` is a securely generated random string (minimum 256 bits), stored only in the hosting platform's secret manager
- [ ] `DATABASE_URL` points to a production PostgreSQL instance (not SQLite)
- [ ] HTTPS is enforced — no HTTP in the network security config
- [ ] Rate limiting is reviewed and configured for expected production traffic
- [ ] Alembic migrations have been run against the production database: `alembic upgrade head`
- [ ] Backend health check endpoint (`GET /health`) returns 200
- [ ] Log configuration confirmed: no prescription text, medicine details, or access tokens logged

Before releasing the Android APK/AAB:

- [ ] `local.properties` is absent from the build machine (CI cleans workspace automatically)
- [ ] Production keystore is loaded from the CI secret, not checked into source
- [ ] `BuildConfig.BACKEND_BASE_URL` resolves to the production HTTPS URL
- [ ] `isMinifyEnabled = true` in the release build type and proguard-rules.pro is tested
- [ ] App bundle signed with the production keystore and verified with `apksigner verify`

---

## Backend Environment Variables Reference

Full list of environment variables consumed by the FastAPI backend:

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | — | SQLAlchemy database URL. Use PostgreSQL in production. |
| `JWT_SECRET_KEY` | Yes | — | Secret for signing JWTs. Must be unique per environment. Never share between environments. |
| `JWT_ALGORITHM` | No | `HS256` | JWT signing algorithm. |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | No | `30` | JWT expiry in minutes. |
| `ENVIRONMENT` | No | `production` | One of: `development`, `testing`, `production`. Controls debug behaviour. |
| `RATE_LIMIT_REQUESTS` | No | `100` | Maximum requests per window per client. |
| `RATE_LIMIT_WINDOW_SECONDS` | No | `60` | Rate limit window in seconds. |
| `ALLOWED_ORIGINS` | No | `[]` | Comma-separated CORS origins. Required for browser clients. |
| `TESTING` | No | `false` | Set to `true` in CI to enable test-mode shortcuts (e.g. in-memory DB). |

> **Security note:** Set `ENVIRONMENT=production` in all deployed environments. Never set `TESTING=true` in production.

---

## Encryption Key Management

The Android app uses Android Keystore for all encryption key operations. No raw key material is stored in files or SharedPreferences.

- `EncryptedSharedPreferences`: stores tokens and sensitive preferences; backed by a Keystore-managed key.
- Room database encryption (SQLCipher): evaluation is pending. See `EncryptionDecisionDoc.kt` for the current decision record.
- Key invalidation: keys are invalidated on account deletion. Review the `DeleteAccountUseCase` for the invalidation sequence.

> Obtain qualified security review of the key derivation and invalidation strategy before production release. See `docs/architecture/` for ADRs.
