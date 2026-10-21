# Engineering Steering
## Android
Kotlin, Jetpack Compose, ViewModel, repository interfaces, Room, CameraX, Photo Picker, WorkManager. Use app-private storage; evaluate encrypted local files/database and Android Keystore key handling before production. Preserve unsent edits and show sync status.
## Backend
FastAPI typed schemas, validation, authentication, authorization on every object, rate limiting, least privilege, migrations, documented API contracts. Keep secrets server-side, never in the APK.
## OCR/AI
Treat document text as untrusted input. Validate structured outputs. Never infer missing dose, duration, diagnosis, allergy, or medicine status. Store confidence, review state, and source references. Keep deterministic duplicate matching separate from generated summaries.
## Quality
Test OCR uncertainty, duplicates, differing strengths, combination products, offline access, sync conflicts, deletion, sharing consent, and accessibility. Use synthetic fixtures only.
