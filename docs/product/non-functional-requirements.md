# Non-Functional Requirements — MediHistory AI

**Document version:** 1.0  
**Status:** Draft

Non-functional requirements (NFRs) define quality attributes and constraints that apply across all features. They are not feature-specific unless noted. Each NFR has a measurable acceptance condition wherever possible.

---

**NFR-001 — App launch performance**  
Category: Performance  
Description: The app must display the dashboard (or onboarding screen for new users) within 2 seconds of cold launch on a mid-range Android device (2 GB RAM, Android 10).  
Acceptance condition: Cold launch time measured from app tap to first meaningful content < 2 000 ms on a Pixel 4a equivalent device. Measured using Android Jetpack Benchmark or equivalent.

---

**NFR-002 — OCR processing time**  
Category: Performance  
Description: OCR and structured extraction for a single prescription page should complete within 30 seconds under normal network conditions. The patient must not be blocked during processing.  
Acceptance condition: 95th percentile end-to-end OCR time (image submitted to result stored) ≤ 30 s on a standard 4G connection. The app remains interactive during the full processing period.

---

**NFR-003 — Medicine explorer load time**  
Category: Performance  
Description: The medicine explorer must load and display the first screen of results within 1 second from navigation tap, even with 100 medicines in the local database.  
Acceptance condition: Time from navigation tap to first medicine list item rendered < 1 000 ms with a synthetic database of 100 PrescriptionMedicine records. Measured using Compose Benchmark.

---

**NFR-004 — Offline availability**  
Category: Reliability / Offline  
Description: All read operations (viewing prescriptions, medicines, alerts, summaries) must work fully offline. Write operations (add prescription, enter manual data, log dose, mark alert reviewed) must be queued offline and synced when connectivity is restored. OCR and AI features that require a network connection must clearly indicate they are unavailable offline rather than failing silently.  
Acceptance condition: All read operations succeed in Airplane Mode with a database containing 10+ prescriptions. Write operations are persisted locally and reflected in the server database after reconnection. Offline OCR unavailability is indicated with a plain-language message.

---

**NFR-005 — Data integrity**  
Category: Data Integrity  
Description: No prescription data, source document, extracted medicine record, or generated summary must be silently corrupted or lost during normal operations, including app crash, network loss, or device rotation. Room database transactions must be used for all multi-table writes.  
Acceptance condition: Monkey test (1 000 random events) does not produce a crash that results in data loss. Rotating the device during an OCR job does not result in a duplicate or missing prescription record.

---

**NFR-006 — Security — data at rest**  
Category: Security  
Description: Before production launch, the local Room database and stored prescription files must be protected using Android Keystore-backed encryption (e.g. SQLCipher with a Keystore-managed key, or EncryptedFile from the Jetpack Security library). The encryption approach must be reviewed against the threat model in docs/architecture/security-threat-model.md.  
Note: Encryption at rest is an evaluation item for pre-production. The engineering decision (library, key rotation, migration) must be documented in the architecture document before implementation.  
Acceptance condition: A qualified security reviewer confirms the encryption approach is appropriate. The approach is documented in the architecture document.

---

**NFR-007 — Security — data in transit**  
Category: Security  
Description: All communication between the Android app and the backend API must use HTTPS with TLS 1.2 or higher. Certificate pinning should be evaluated for the production release. No medical data is transmitted over unencrypted channels.  
Acceptance condition: Network traffic captured during an integration test shows no HTTP requests containing medical data. TLS version is verified as 1.2+.

---

**NFR-008 — Authentication and authorisation**  
Category: Security  
Description: Every backend API endpoint that handles patient data must require a valid JWT and enforce patient-scoped authorisation. No endpoint may return data for a patient other than the authenticated requester. JWT access tokens expire within 30 minutes (configurable). Refresh tokens are invalidated on logout.  
Acceptance condition: Attempting to access any patient data endpoint with a JWT for patient A while requesting patient B's data returns HTTP 403. Expired tokens return HTTP 401. Invalidated refresh tokens return HTTP 401.

---

**NFR-009 — Rate limiting**  
Category: Security  
Description: The backend API must apply rate limiting to all public endpoints (auth, OCR trigger, history generation, export) to prevent abuse. Rate limits are configurable per environment. Exceeded limits return HTTP 429 with a Retry-After header.  
Acceptance condition: Sending 100 requests per second to POST /auth/login from a single IP triggers a 429 response. The Retry-After header is present.

---

**NFR-010 — Accessibility — WCAG 2.1 AA**  
Category: Accessibility  
Description: All screens must meet WCAG 2.1 AA criteria as applicable to native Android Compose UI, including:  
- Minimum touch target size: 48×48 dp.  
- Colour contrast ratio ≥ 4.5:1 for normal text, ≥ 3:1 for large text and UI components.  
- All interactive elements have accessible names (contentDescription or semantics label).  
- Screen reader (TalkBack) can navigate all content in a logical order.  
- Status indicators do not rely on colour alone (include text or icon).  
Note: Full WCAG compliance requires manual testing with assistive technologies and an expert accessibility review. This requirement sets the target; automated tooling (e.g. Accessibility Scanner) is a minimum check only.  
Acceptance condition: Accessibility Scanner reports zero critical issues on all screens. Manual TalkBack test can complete all primary user journeys without getting stuck.

---

**NFR-011 — Battery usage**  
Category: Battery  
Description: Background OCR and sync jobs must use WorkManager with appropriate constraints (network required, battery not low). The app must not hold wake locks unnecessarily. Background work should not add more than 2% battery drain per day under typical usage (one new prescription per day).  
Acceptance condition: Android Battery Historian shows no unexpected wake locks from the app. WorkManager jobs use NetworkType.CONNECTED and BatteryNotLow constraints.

---

**NFR-012 — Storage usage**  
Category: Storage  
Description: The app must store prescription images efficiently. Images captured via CameraX must be compressed to ≤ 2 MB before storage without visible quality loss. The app must display used storage in the settings screen. The patient must be warned before adding a prescription if available storage is below 50 MB.  
Acceptance condition: A camera-captured prescription image is stored at ≤ 2 MB. Used storage display in settings matches the actual size of the app's private directory within ±5%.

---

**NFR-013 — Backup and restore**  
Category: Reliability  
Description: When cloud backup is enabled (opt-in), the patient must be able to restore all prescriptions, medicines, history summaries, and settings on a new device by logging in to their account. Restore must not overwrite local data that is newer than the backup without patient confirmation.  
Acceptance condition: An end-to-end test (add 3 prescriptions on device A → backup → restore on device B) produces identical data on device B. Restoring on a device with newer local data shows a conflict resolution prompt.

---

**NFR-014 — Audit logging**  
Category: Security / Compliance  
Description: The system must maintain an audit log of all significant actions: login, logout, prescription added, prescription deleted, OCR triggered, review state changed, summary generated, PDF exported, data shared, account deletion requested. Audit log entries must contain only: action, entity_id, entity_type, timestamp, actor. Audit log entries must never contain prescription text, medicine names, dosages, or document content.  
Acceptance condition: AC-060 passes. A log retention policy (minimum 90 days for backend logs) is documented and implemented.

---

**NFR-015 — Deletion guarantees**  
Category: Privacy  
Description: Patient-requested deletion must remove all personal health information from local storage within the same session and from the backend within 30 days (or immediately if technically feasible). The patient must receive a confirmation and an explanation of any retention limitations (e.g. backup copies, legal hold periods).  
Acceptance condition: AC-055 passes. Deletion confirmation message accurately describes the retention timeline. A backend job verifies deletion completion within the committed period.

---

**NFR-016 — Privacy by default**  
Category: Privacy  
Description: Cloud backup is off by default. Sharing is always an explicit patient action. No analytics SDK that captures health data or screen content is integrated. The app does not request permissions not required for its stated functions. Location permission is never requested.  
Acceptance condition: Fresh install has no background data upload. Manifest contains no location permission. Privacy policy review confirms no analytics SDK captures PHI.

---

**NFR-017 — Regulatory and legal review**  
Category: Compliance  
Description: Before production launch, the following must be reviewed by qualified legal and security professionals:  
- Applicable India healthcare data protection requirements (including DPDP Act 2023 and any sector-specific rules).  
- Applicability of the Digital Health Data Protection rules to this application.  
- Terms of service and privacy policy requirements for consent-based health data collection.  
- Export control and encryption notification requirements for cloud backup.  
Note: This document identifies the review requirement. It does not constitute a compliance certification or legal opinion.  
Acceptance condition: Qualified legal and security review documented and signed off before release to production.

---

**NFR-018 — Synthetic data in tests and demos**  
Category: Privacy / Testing  
Description: All automated tests, CI pipelines, demo builds, and development fixtures must use synthetic data only. No real patient names, real prescription content, or real medicine dosages from real prescriptions may appear in test fixtures, screenshots, or demo data.  
Acceptance condition: CI review confirms no real patient data in test fixtures. Demo APK build uses a seeded synthetic dataset.

---

**NFR-019 — No secrets in APK or version control**  
Category: Security  
Description: API keys, database credentials, JWT secrets, encryption keys, and storage access keys must never appear in the Android APK, in Android source code, in build scripts committed to version control, or in CI environment variables that are accessible to pull request runs. All such secrets must be stored in server-side configuration or a managed secret store.  
Acceptance condition: AC-059 passes. Git history scan (truffleHog or equivalent) finds no secrets in the repository. Release APK binary analysis finds no credential strings.

---

**NFR-020 — Graceful degradation for AI/OCR services**  
Category: Reliability  
Description: If any AI or OCR service is unavailable, the app must continue to function for all features that do not require those services (viewing existing records, medicine explorer, duplicate alerts for reviewed data, reminders, dose logging). The unavailability must be clearly communicated in the UI. The patient must always be able to enter data manually as a fallback.  
Acceptance condition: With all AI/OCR environment variables unset, the app starts successfully, displays all local records, and shows a clear "OCR unavailable — enter data manually" message on the prescription add screen. No crash occurs.

---

*NFRs are stable once approved. Changes require a version increment to this document and an update to the linked architecture and threat model documents.*
