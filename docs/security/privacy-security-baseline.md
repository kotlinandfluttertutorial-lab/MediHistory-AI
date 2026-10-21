# Privacy and Security Baseline
Medical records, prescriptions, diagnoses, tests, allergies, and medication history are highly sensitive.
- TLS in transit; encryption at rest for database, object storage, and backups.
- App-private local storage; evaluate encrypted files/database and Keystore key handling.
- Authentication and per-object authorization for cloud features.
- Least privilege and separate dev/test/prod environments.
- No medical content in logs, analytics, URLs, notifications, or test fixtures.
- Secrets in managed storage, never embedded in Android.
- Explicit opt-in backup; preview and consent for sharing.
- Export/deletion for metadata, originals, reports, and cloud copies; document backup retention.
- Expiring/revocable links if link sharing is implemented.
- Test cross-account access, IDOR, token leakage, local file exposure, transport security, and deletion.
- Disclose external OCR/AI processing and review retention, training use, region, subprocessors, and contracts.
This is a baseline, not legal advice or certification. Obtain qualified review before public release.
