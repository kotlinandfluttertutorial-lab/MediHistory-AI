# Security Threat Model — MediHistory AI

**Document version:** 1.0  
**Status:** Draft — requires qualified security review before production launch

> **Disclaimer:** This document identifies threats and mitigations to guide engineering decisions. It does not constitute a security certification, compliance audit, or legal opinion. A qualified security professional and legal counsel must review this model and applicable India regulatory requirements (including the DPDP Act 2023 and any sector-specific health data rules) before production launch.

---

## 1. Assets

| Asset | Sensitivity | Location |
|-------|------------|---------|
| Prescription images and PDFs | Very High — original source documents | App-private storage (device); optional cloud backup |
| Extracted medicine data | Very High — structured health data | Room database (device); PostgreSQL (backend if sync enabled) |
| Medical history summaries | Very High — derived health data | Room database; PostgreSQL |
| Patient identity (name, email) | High | Room database; PostgreSQL |
| Auth tokens (JWT access + refresh) | High | Android EncryptedSharedPreferences / Keystore; in-memory only during API calls |
| Backend API keys (OCR, AI providers) | High | Backend environment variables / secret manager only |
| Database credentials | High | Backend environment variables / secret manager only |
| Backup encryption keys | High | Android Keystore (device); patient-controlled |
| Audit log | Medium | PostgreSQL; local Room (summary only) |

---

## 2. Trust boundaries

1. **Device boundary:** Between the Android app and the OS/other apps.
2. **Network boundary:** Between the Android app and the FastAPI backend.
3. **Backend boundary:** Between the FastAPI backend and external AI/OCR services.
4. **Cloud storage boundary:** Between the backend and the optional cloud backup store.
5. **OCR input boundary:** Between the raw prescription image and the AI/OCR processing pipeline.

---

## 3. Threat actors

| Actor | Description | Motivation |
|-------|------------|-----------|
| Unauthorized third party | Attacker with no physical access to device | Data theft, identity theft, ransom |
| Compromised device | Attacker with physical or malware access to the device | Direct file or database access |
| Compromised server | Attacker who gains backend access | Mass patient data exfiltration |
| Malicious OCR input | Prescription image containing prompt injection or adversarial content | Manipulate AI extraction, inject false data |
| Malicious insider | Developer or operator with production access | Data theft or manipulation |
| Other app on device | Malicious app installed alongside MediHistory AI | Intercept data via shared storage or IPC |

---

## 4. STRIDE analysis

### 4.1 Spoofing

| Threat | Component | Mitigation |
|--------|-----------|-----------|
| Attacker authenticates as a different patient | Backend JWT validation | Short-lived access tokens (30 min); patient-scoped authorisation on every endpoint; refresh token rotation on use |
| Attacker forges a JWT | JWT signature validation | JWT signed with a server-side secret (HS256 minimum; RS256 preferred for production); secret stored in managed secret store, never in APK |
| App spoofed by a fake APK | APK signing | Release APK signed with a production keystore stored in secure managed storage; signing key never committed to git |

---

### 4.2 Tampering

| Threat | Component | Mitigation |
|--------|-----------|-----------|
| Prescription file modified after upload | App-private storage | SHA-256 hash stored at upload time; verified on every read (AC-006) |
| Extracted medicine data tampered with in Room | Local database | Database encryption (SQLCipher or equivalent, Android Keystore key) — to be implemented before production (NFR-006); original OCR text stored separately from patient edits |
| Backend database record tampered | PostgreSQL | Patient-scoped authorization; audit log records entity_id and timestamp; restricted DB user (least privilege) |
| OCR result injected with false medical data | OCR input boundary | OCR output treated as untrusted; validated against strict Pydantic schema; confidence scoring; patient review required before use |

---

### 4.3 Repudiation

| Threat | Component | Mitigation |
|--------|-----------|-----------|
| Patient denies adding a prescription | Audit log | AuditLogEntry records ADD_PRESCRIPTION with entity_id and timestamp; original document hash stored |
| Patient denies sharing data | Audit log | AuditLogEntry records SHARE_PDF with destination app and document ID |
| AI generates summary that was never shown to patient | Summary versioning | Each summary stored with generated_at, input_snapshot_hash, and is_current flag |

---

### 4.4 Information Disclosure

| Threat | Component | Mitigation |
|--------|-----------|-----------|
| Other app reads prescription files | App-private storage | Files stored in `Context.filesDir`; no `READ_EXTERNAL_STORAGE` requested; no `FileProvider` exposed |
| Prescription content in logs | Logging layer | Never log prescription text, medicine names, dosage values, or document content (enforced in code review and AC-060) |
| Prescription content in error responses | FastAPI error handlers | Generic error messages; no health content in 4xx/5xx responses |
| API key in APK binary | Android build | No API keys in source code, build scripts, or resources; all provider credentials are server-side only (NFR-019, AC-059) |
| API key in git history | Source control | .gitignore covers .env and credential files; git history scan in CI (truffleHog or equivalent) |
| JWT in log files | Backend logging | JWT values are never logged; only audit metadata (actor_id, action) |
| Cross-patient data exposure | Backend authorisation | `get_current_patient` dependency enforced on all patient-data endpoints; every DB query filtered by `patient_id`; cross-account exposure is a release blocker |

---

### 4.5 Denial of Service

| Threat | Component | Mitigation |
|--------|-----------|-----------|
| Flood of OCR requests | POST .../ocr | Rate limit: 10 requests / hour / patient (NFR-009) |
| Flood of auth requests | POST /auth/login, /auth/register | Rate limit: 20/10 requests per 15 min per IP |
| Large file upload exhausting storage | POST .../prescriptions | File size limit (TBD; suggested 20 MB); content-type validation |
| History generation flood | POST .../history/generate | Rate limit: 5 requests / hour / patient |

---

### 4.6 Elevation of Privilege

| Threat | Component | Mitigation |
|--------|-----------|-----------|
| Patient accesses another patient's data | Backend API | Patient-scoped authorisation enforced at service layer, not just route; reviewed in every endpoint implementation |
| Expired token used to access data | JWT validation | Token expiry enforced server-side; no client-side expiry bypass |
| Malicious OCR input escapes to backend process | OCR input boundary | OCR text is treated as user-controlled string input; never executed or eval'd; passed only to Pydantic-validated extraction schema |

---

## 5. Key mitigations summary

| Mitigation | Status | NFR / AC reference |
|-----------|--------|-------------------|
| Android Keystore for local key management | Design (implement before production) | NFR-006 |
| App-private storage (no shared storage) | Design | AC-006, NFR-016 |
| Room database encryption | TODO — library decision pending (AD-001) | NFR-006 |
| HTTPS / TLS 1.2+ for all API calls | Design | NFR-007 |
| JWT short expiry + refresh token rotation | Design | NFR-008 |
| Patient-scoped authorisation on every endpoint | Design | NFR-008 |
| No API keys in APK or git | Design | NFR-019, AC-059 |
| Rate limiting on all public endpoints | Design | NFR-009 |
| Input validation (Pydantic, strict schemas) | Design | AC-018 |
| Audit logging (metadata only, no health content) | Design | NFR-014, AC-060 |
| OCR output treated as untrusted | Design | AC-017, AC-018 |
| Cloud backup opt-in with visible status | Design | AC-057, NFR-016 |
| Sharing requires explicit consent + preview | Design | AC-049 |
| No diagnosis / prescribing / dose advice | Product boundary | Product principles |
| Synthetic data in all tests and demos | Process | NFR-018 |

---

## 6. Residual risks and open items

| Risk | Severity | Status |
|------|---------|--------|
| Room database is unencrypted until AD-001 is resolved | High | Open — blocks production launch |
| Certificate pinning not implemented | Medium | Deferred to production hardening |
| JWT algorithm is HS256 — RS256 preferred for production | Medium | Open — configure before production |
| Backup cloud provider not selected — encryption approach to be validated | High | Open — required before enabling cloud backup |
| Legal/regulatory review of India DPDP Act applicability | High | Open — blocks production launch |
| Penetration test not conducted | High | Open — required before production launch |
| MED_REFERENCE_PROVIDER not yet selected — licensing and data handling unreviewed | Medium | Open — required before F09 ships |

---

## 7. Cross-account exposure policy

Any vulnerability that allows patient A to access, modify, or enumerate data belonging to patient B is classified as **Critical** and blocks release. This includes:
- API responses returning other patients' records.
- Error messages leaking other patients' IDs.
- Shared database queries not filtered by `patient_id`.

---

*This document must be reviewed and updated when: new features are added that access patient health data, the technology stack changes, external provider integrations are added, or the threat landscape changes significantly. Increment the document version on every substantive change.*
