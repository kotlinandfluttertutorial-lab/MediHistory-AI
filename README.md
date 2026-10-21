# MediHistory AI

> **"All prescriptions. All medicines. One doctor-ready medical history."**

MediHistory AI is a native Android application that helps patients organise prescriptions from multiple doctors, maintain a consolidated medicine history, detect potential duplicate active ingredients, and generate a doctor-ready medical history linked to the original source documents.

---

## Product objective

Patients see multiple specialists and accumulate paper and digital prescriptions from different clinics. These records are scattered, hard to compare, and difficult to hand over to a new doctor. MediHistory AI solves that by extracting structured information from prescription images and PDFs, normalising medicine names to active ingredients, detecting potentially overlapping prescriptions, and producing a single chronological summary — always traceable back to the original document.

---

## Features

| ID  | Feature | Priority |
|-----|---------|----------|
| F01 | Patient dashboard — summary of records, doctors, medicines, duplicate alerts | High |
| F02 | Multi-doctor prescription management — upload, capture, store, preserve originals | High |
| F03 | OCR and prescription review — extract structured data, show uncertain fields, require review | High |
| F04 | Consolidated medicine explorer — searchable, filterable view across all prescriptions | High |
| F05 | Potential duplicate ingredient detection — explainable cross-prescription alerts | High |
| F06 | AI medical history summarizer — chronological, source-linked doctor-ready summary | High |
| F07 | New Doctor Mode — focused summary, patient-reviewed, PDF export, explicit consent sharing | Medium |
| F08 | Medication reminders and adherence — optional reminders, dose logging | Medium |
| F09 | Medication interaction checks — only when a licensed reference source is configured | Low |
| F10 | Privacy and security — encryption, access control, consent, data minimisation, deletion | High |

---

## Technology stack

| Layer | Technology |
|-------|-----------|
| Android UI | Kotlin, Jetpack Compose, Material 3 |
| Android architecture | MVVM, Repository pattern, ViewModel |
| Local database | Room / SQLite |
| Camera / document capture | CameraX, Android Photo Picker |
| Background jobs | WorkManager |
| Key management | Android Keystore |
| Backend API | Python, FastAPI (typed schemas, JWT auth) |
| Backend database | PostgreSQL (optional sync; local-only mode supported) |
| AI / OCR | Provider-independent interfaces — OCR, structured extraction, normalization, summarization |
| Authentication | Secure JWT auth with patient-scoped authorization on every endpoint |
| Document storage | App-private local storage; optional encrypted cloud backup (opt-in) |
| Testing | Android unit tests, Compose UI tests, pytest, API integration tests, CI quality checks |

---

## Repository layout

```
MediHistory-AI/
├── android/          # Android application source (Kotlin / Compose)
├── backend/          # FastAPI backend source (Python)
├── docs/
│   ├── product/      # Requirements, feature catalog, glossary, acceptance criteria, NFRs
│   ├── architecture/ # Architecture doc, data model, API contracts, threat model, AI safety
│   ├── design/       # Design system, navigation map, screen inventory, Stitch prompts, Figma specs
│   └── planning/     # Jira CSV, ticket-to-prompt mapping, implementation plan
└── .kiro/
    ├── specs/        # Kiro spec files (one per major feature)
    ├── steering/     # Persistent steering rules
    └── skills/       # Reusable Kiro skills
```

---

## Safety boundary

MediHistory AI is a **record organisation and clinician-support tool**, not a diagnosis or prescribing system.

- Original prescription documents are **never modified** by extraction or AI processing.
- Duplicate ingredient findings are labelled **potential** — they require doctor or pharmacist review.
- The application **never advises** starting, stopping, combining, or changing any medication.
- Uncertain OCR extractions are always flagged and queued for patient review before use.
- AI-generated summaries clearly distinguish documented facts, patient-reported information, uncertain extractions, and AI-generated text.

---

## Getting started

```bash
# 1. Clone the repository
git clone <repository-url>
cd MediHistory-AI

# 2. Review the product specifications
#    Start at docs/product/product-requirements.md
#    Then docs/architecture/architecture.md

# 3. Resolve any open decisions marked TODO in the docs

# 4. Copy the environment template and fill in values
cp .env.example .env
# Never commit .env to git

# 5. Import the Jira CSV to your project board
#    File: docs/planning/jira-import.csv

# 6. Open the repository in Kiro and run the master prompt
#    File: .kiro/specs/master-prompt.md

# 7. Implement one Jira ticket at a time using the corresponding Kiro prompt
#    Files: .kiro/specs/ticket-prompts/
```

---

## Contribution notes

- Use **synthetic data only** in all tests and demos. Never commit real patient data.
- **No secrets in the APK** — API keys and credentials are stored server-side or in managed secret storage.
- **No secrets in git** — `.env` and credential files are in `.gitignore`.
- Every extracted fact must link to its source document.
- OCR confidence and review state must be stored and displayed.
- Match existing code style; run `./gradlew lint test` (Android) and `pytest` (backend) before committing.

---

## Licence

See `LICENSE` file (to be added before public release).
