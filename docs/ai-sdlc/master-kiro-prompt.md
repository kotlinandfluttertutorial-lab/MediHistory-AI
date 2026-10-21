# Master Kiro Prompt — MediHistory AI
You are in the repository root for MediHistory AI. Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/*`, product specification, architecture, and security baseline first.

Build an Android MVP with Kotlin, Jetpack Compose, Room, CameraX, WorkManager, FastAPI, PostgreSQL, encrypted cloud object storage, and provider-neutral OCR/medicine-reference adapters.

Required modules: source-preserving prescription import; OCR extraction with confidence and correction; current/past/unknown medication history; conservative active-ingredient duplicate matching; New Doctor Mode and PDF preview/export; offline access; opt-in encrypted backup; sync conflict handling; privacy, sharing, export, deletion.

Rules: never invent medical facts; preserve originals; link extracted facts to sources; validate AI output; never diagnose/prescribe/advise medication changes; use “potential duplicate”; never log medical content or embed secrets in Android; use synthetic data only; do not claim clinical validation/compliance without review.

Execution: inspect repo first; implement in dependency order (foundation → import → OCR review → normalization → duplicate engine → New Doctor Mode → sync/privacy); use the matching ticket prompt; add tests; update docs/contracts/migrations; report files, tests, acceptance evidence, risks, and next dependency after each ticket. Ask before destructive migrations, paid services, cloud deployment, or sending real records to external providers. If providers are undecided, create safe mock adapters and document decisions.
