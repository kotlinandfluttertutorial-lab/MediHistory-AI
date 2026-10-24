# Skill: Medical Data Security for MediHistory AI

## Purpose

Guide consistent, layered security for MediHistory AI. Covers Android encryption at rest (Keystore, EncryptedSharedPreferences, SQLCipher evaluation), encryption in transit, patient-scoped backend authorisation, consent flows, deletion with audit trail, and no-logging rules.

## When to Apply This Skill

- Adding or modifying any storage of sensitive data (tokens, preferences, files, database)
- Implementing or reviewing authentication or authorisation logic
- Building the cloud backup opt-in flow or the sharing flow
- Implementing account or data deletion
- Writing security tests or adding CI security checks
- Performing a threat model review

## Steering Files to Read First

- `.kiro/steering/medical-safety.md`
- `.kiro/steering/tech.md`
- `.kiro/steering/product.md`

---

## Step-by-Step Guidance

### 1. Android: Encrypt All Sensitive Local Data

| Data type | Required storage |
|---|---|
| Session tokens, auth credentials | `EncryptedSharedPreferences` |
| User preferences with sensitive values | `EncryptedSharedPreferences` |
| Document files, exported summaries | `context.getFilesDir()` (app-private) |
| Room database | Evaluate SQLCipher before production; document recommendation with tradeoffs |
| Encryption keys | Android Keystore — never stored as raw bytes in files or preferences |

Never use plaintext `SharedPreferences` for tokens, keys, or any sensitive value.

### 2. Android: Keep Files App-Private

All files written at runtime go to `context.getFilesDir()` or `context.cacheDir()`. Never write to `Environment.getExternalStorageDirectory()` or any path accessible to other apps. Verify with `MODE_PRIVATE` or absence of `READ_EXTERNAL_STORAGE` permission usage.

### 3. Android: Clear Session State on Logout

On logout and session expiry, clear:
- Session token from `EncryptedSharedPreferences`
- Any in-memory state in `ViewModel`s
- Any cached files scoped to the session (not the patient's documents)

### 4. Backend: Authenticate Every Protected Route

Every endpoint except `/auth/login` and `/health` requires a valid JWT. Apply auth middleware at the framework level so new routes are protected by default — not per-endpoint as an afterthought.

### 5. Backend: Patient-Scope Every Query

Every database query touching patient data includes `patient_id == current_patient.id`. Cross-patient access returns HTTP 404 (not 403 — returning 403 leaks that the resource exists).

### 6. Backend: No Secrets in Code

All secrets (JWT signing key, database password, OCR API key, AI provider key) are read from environment variables at runtime. They are never in source files, config files, or committed `.env` files.

### 7. Consent: Cloud Backup Is Opt-In

Cloud backup is disabled by default. Enabling it requires:
1. An explicit confirmation dialog shown to the patient.
2. A clear explanation that data will leave the device.
3. Visible sync status (last sync time, failure state).

Disabling backup must explain cloud retention limitations (data may persist on the server until deletion is confirmed).

### 8. Consent: Sharing Requires Explicit Action and Preview

The patient must explicitly initiate sharing. Before any share action completes, show a preview of exactly what will be shared. Sharing is never triggered automatically or silently.

### 9. Deletion: Remove All Local and Remote Data

Account or data deletion must:
1. Delete all Room database records for the patient.
2. Delete all source document files from app-private storage.
3. Cancel and remove all WorkManager jobs for the patient.
4. Clear all notifications for the patient.
5. Clear `EncryptedSharedPreferences` for the session.
6. If sync is enabled, send a deletion request to the backend.
7. Write an audit log entry: action type, timestamp, entity ID — no content.

### 10. Never Log Sensitive Content

Log only anonymised event types, record IDs, status codes, and timestamps. Never log prescription text, medicine names, doctor names, access tokens, or document contents — in Android Logcat or server logs.

---

## Patterns to Follow

**Android**
- Encryption keys: Android Keystore. Never store raw key material in SharedPreferences or files.
- Sensitive preferences: `EncryptedSharedPreferences`. Never plaintext `SharedPreferences` for tokens or keys.
- File storage: `getFilesDir()` only. Never external shared storage for sensitive data.
- Session tokens: `EncryptedSharedPreferences`. Cleared on logout and session expiry.
- Database: evaluate SQLCipher before production; document recommendation with tradeoffs.

**Backend**
- All endpoints require JWT authentication (except `/auth/login` and `/health`).
- Patient-scoped queries filter by `patient_id`.
- Cross-patient access returns 404 (not 403).
- All secrets are environment variables — never in source code.

**Consent**
- Cloud backup is opt-in; disabled by default.
- Enabling backup requires explicit confirmation dialog.
- Sharing requires explicit patient action and preview of shared content.
- Consent records stored locally — not transmitted to backend.
- Disabling backup explains cloud retention limitations.

**Deletion**
- Deletion removes: Room records, document files, WorkManager jobs, notifications, `EncryptedSharedPreferences`.
- Backend deletion request sent if sync is enabled.
- Audit log: action type, timestamp, entity ID — no content.

**Logging**
- Log only: anonymised event types, timestamps, record IDs, status codes.
- Never log: prescription text, medicine details, access tokens, document contents, patient names in medical context.

## Patterns to Avoid

- Storing raw key bytes in `SharedPreferences` or any file outside Keystore.
- Using plaintext `SharedPreferences` for tokens, keys, or sensitive preferences.
- Writing files to external shared storage.
- Hardcoding secrets (JWT key, DB password, API keys) in any source or config file.
- Returning 403 for cross-patient access (use 404).
- Enabling cloud backup silently or by default.
- Sharing data without an explicit patient action and preview.
- Logging prescription text, medicine names, tokens, or document content.
- Leaving session tokens in storage after logout.
- Omitting the audit log entry on deletion.

---

## Example Code Structure (Patterns Only — Not Full Implementations)

```kotlin
// ── EncryptedSharedPreferences for session token ──────────────────────────────
class SecureSessionStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "session_prefs",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) = prefs.edit().putString(KEY_TOKEN, token).apply()
    fun getToken(): String?       = prefs.getString(KEY_TOKEN, null)
    fun clearToken()              = prefs.edit().remove(KEY_TOKEN).apply()  // call on logout

    companion object { private const val KEY_TOKEN = "session_token" }
}


// ── App-private file storage (never external shared storage) ──────────────────
class DocumentFileStore @Inject constructor(@ApplicationContext private val context: Context) {

    fun documentFile(filename: String): File =
        File(context.getFilesDir(), "documents/$filename")  // app-private only

    // WRONG — never do this:
    // File(Environment.getExternalStorageDirectory(), filename)
}


// ── Audit log entry on deletion (action/timestamp/ID — no content) ────────────
data class AuditLogEntry(
    val action: AuditAction,
    val timestamp: Instant,
    val entityId: String         // record ID only — never content
)

enum class AuditAction { DELETION, EXPORT, SHARE, BACKUP_ENABLED, BACKUP_DISABLED }


// ── Deletion UseCase skeleton ─────────────────────────────────────────────────
class DeleteAccountUseCase @Inject constructor(
    private val prescriptionRepository: PrescriptionRepository,
    private val documentFileStore: DocumentFileStore,
    private val workManager: WorkManager,
    private val sessionStore: SecureSessionStore,
    private val auditRepository: AuditRepository,
    private val syncRepository: SyncRepository,
) {
    suspend fun execute(patientId: String) {
        prescriptionRepository.deleteAllForPatient(patientId)      // Room records
        documentFileStore.deleteAllForPatient(patientId)           // source files
        workManager.cancelAllWorkByTag(patientId)                  // WorkManager jobs
        sessionStore.clearToken()                                  // EncryptedSharedPreferences
        if (syncRepository.isSyncEnabled()) {
            syncRepository.requestBackendDeletion(patientId)       // backend deletion
        }
        auditRepository.log(AuditLogEntry(AuditAction.DELETION, Instant.now(), patientId))
        // Log only event type + ID — no content
        Log.i(TAG, "account.deleted: patientId=$patientId")
    }

    companion object { private const val TAG = "DeleteAccountUseCase" }
}


// ── Cloud backup consent flow ─────────────────────────────────────────────────
// Backup is disabled by default. Show explicit confirmation before enabling.
@Composable
fun EnableBackupDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title   = { Text("Enable cloud backup?") },
        text    = { Text("Your prescription data will be stored on our servers. You can disable backup at any time; note that server copies may take up to 30 days to be fully deleted after you request removal.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Enable backup") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        onDismissRequest = onDismiss
    )
}
```

```python
# ── Backend: patient-scoped query, 404 for cross-patient access ───────────────
@router.delete("/prescriptions/{prescription_id}", status_code=204)
async def delete_prescription(
    prescription_id: str,
    current_patient: Patient = Depends(get_current_patient),
    db: Session = Depends(get_db),
):
    prescription = (
        db.query(Prescription)
        .filter(
            Prescription.id == prescription_id,
            Prescription.patient_id == current_patient.id,   # patient-scoped
        )
        .first()
    )
    if not prescription:
        raise HTTPException(status_code=404)   # 404, never 403
    db.delete(prescription)
    db.commit()
    # Log only anonymised metadata — never content
    logger.info("prescription.deleted", extra={"prescription_id": prescription_id})
```

---

## Key Constraints (from Steering)

- Medical records are highly sensitive: encrypt at rest and in transit.
- Collect only necessary data; store no more than the minimum required.
- Cloud backup is opt-in with visible status, failures, and recovery implications.
- Sharing requires explicit patient action and preview; private by default.
- Never log prescription text, medicine details, tokens, or document contents.
- Cross-account exposure or critical/high security issues block release.
- Obtain qualified legal/security review of applicable India requirements before launch.
- Perform a formal threat model review before production release.
