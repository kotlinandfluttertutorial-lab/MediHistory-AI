# MediHistory AI — Technical Steering

## Android Stack
- Language: Kotlin (no Java)
- UI: Jetpack Compose with Material 3
- Architecture: MVVM with Repository pattern and UseCases for business logic
- Dependency injection: Hilt
- Local database: Room (evaluate SQLCipher encryption before production)
- Background tasks: WorkManager
- Camera: CameraX
- Photo/file import: Android Photo Picker and system file picker
- Async: Kotlin Coroutines and Flow
- Storage: app-private storage only (getFilesDir() / getExternalFilesDir()). Never write to shared external storage.
- Keys: Android Keystore for all encryption keys. Evaluate key derivation and invalidation strategy before production.
- Preferences: EncryptedSharedPreferences for sensitive values. Never plaintext SharedPreferences for tokens or keys.
- Dependency versions: ALL Gradle dependencies must use exact pinned versions in libs.versions.toml. No `+`, no range operators.

## Backend Stack
- Framework: FastAPI
- Schemas: Pydantic typed schemas for all request and response bodies
- Validation: validate all inputs; reject malformed or oversized inputs
- Authentication: JWT-based. Auth middleware applied to every protected route
- Authorisation: patient-scoped on every object. A patient can never access another patient's data. Cross-patient access returns HTTP 404 (not 403, to avoid leaking resource existence)
- Rate limiting: apply to all public and authenticated endpoints
- Least privilege: service accounts and DB roles have minimum required permissions
- Migrations: Alembic for all schema changes. No manual SQL migrations.
- API contracts: all endpoints documented with OpenAPI annotations
- Secrets: ALL secrets (API keys, DB passwords, JWT secret) are server-side environment variables. Never embed secrets in the Android APK or source code.

## OCR / AI Rules
- Document text is untrusted input — sanitise and validate all OCR output
- Validate structured outputs from AI models before persisting
- NEVER infer missing dose, duration, diagnosis, allergy, or medicine status
- Store confidence score, review state, and source reference for every extracted field
- Deterministic duplicate matching is separate from AI-generated summaries
- If a field is missing or unclear, record it as absent — do not fabricate a value

## Security Rules
- No API keys, tokens, or secrets in any Kotlin source file, resource file, or committed config file
- Backend base URL and feature flags via BuildConfig from local.properties (git-ignored) or CI environment variables
- Never log prescription text, medicine details, access tokens, or document contents in Logcat or server logs
- Encrypt data in transit (HTTPS only) and at rest (Keystore-derived keys)
- Perform a formal threat model review before production release
