package com.medihistory.ai.data.security

/**
 * ADR-007: SQLCipher / Database Encryption Evaluation
 *
 * STATUS: DEFERRED — evaluate before production release.
 *
 * DECISION
 * --------
 * The Room database is NOT encrypted in the current development build.
 * Full database encryption evaluation is required before any production
 * release per NFR-006 and the release checklist.
 *
 * REQUIREMENTS FOR PRODUCTION EVALUATION
 * ----------------------------------------
 * 1. Evaluate SQLCipher-android vs Room + Android Keystore transparent encryption.
 * 2. Determine key derivation strategy:
 *    - Option A: Keystore-only key (no user PIN, transparent, revoked on device reset).
 *    - Option B: User-PIN-derived key (stronger, requires PIN UI and re-entry flows).
 * 3. Assess key invalidation behaviour on biometric enrolment change.
 * 4. Measure performance impact on target devices (especially query latency with SQLCipher).
 * 5. Design migration strategy from existing unencrypted databases for existing users.
 *
 * CURRENT MITIGATIONS (DEVELOPMENT ONLY)
 * ----------------------------------------
 * - Android full-disk / file-based encryption (FBE) provides a hardware-level baseline.
 * - App-private storage (getFilesDir()) prevents other apps from reading database files.
 * - EncryptedSharedPreferences protects all sensitive key-value data (see [SecurePreferences]).
 * - Authentication tokens stored exclusively in [SecurePreferences], never plaintext.
 * - No sensitive data is written to Logcat or server logs.
 *
 * BLOCKER
 * --------
 * The SQLCipher evaluation and implementation decision BLOCKS production release.
 * This is tracked in the release checklist and must be resolved before any store submission.
 *
 * REFERENCES
 * -----------
 * - ADR-007 in docs/architecture/
 * - NFR-006 (security non-functional requirements)
 * - Release checklist in docs/release/
 * - tech.md steering: "evaluate SQLCipher encryption before production"
 */
object EncryptionDecisionDoc
