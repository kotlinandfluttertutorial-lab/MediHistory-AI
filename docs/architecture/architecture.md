# Proposed Architecture
## Android
Kotlin, Jetpack Compose, ViewModel, repositories, Room, CameraX, Android Photo Picker, WorkManager, Android Keystore where appropriate.
## Backend
FastAPI, typed schemas, authentication/authorization, rate limiting, PostgreSQL, encrypted object storage.
## Services
Document import; OCR adapter; extraction review; medicine normalization; deterministic duplicate matching; New Doctor Mode report generation; opt-in backup/sync; export/deletion/sharing.
## Data flow
1. Save original locally and create metadata.
2. Run OCR locally or via authenticated backend, disclosing external processing.
3. Save fields with confidence/source references.
4. Patient confirms/corrects; preserve raw OCR and review history.
5. Resolve ingredients only through verified reference data.
6. Compare reviewed entries deterministically.
7. Sync only after opt-in with conflict-safe jobs.
8. Generate a report from selected records and preview before sharing.
## Production decisions
Choose OCR provider, medicine dataset, cloud region, encryption/key recovery, account recovery, deletion semantics, retention, and legal review before launch.
