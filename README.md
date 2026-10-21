# MediHistory AI — Android AI-SDLC Documentation

**Purpose:** Help patients manage prescriptions from multiple doctors, identify possible duplicate active ingredients, and show a source-linked medical history to a new doctor.

**Initial users:** Patients managing their own medical records.  
**Storage:** Hybrid local access plus opt-in encrypted cloud backup.  
**Android:** Kotlin, Jetpack Compose, Room, CameraX, WorkManager.  
**Backend proposal:** Python + FastAPI, PostgreSQL, encrypted object storage.

## Included
- Product requirements, user journeys, and open decisions
- Architecture and proposed data model/API
- Privacy and security baseline
- Kiro steering files and reusable skills
- MVP requirements, design, and tasks
- Jira CSV with 20 implementation tickets
- 20 ticket-specific Kiro prompts and a master prompt

## Safety boundary
This is a record organization and clinician-support tool, not a diagnosis or prescribing system. Duplicate findings are potential issues for doctor/pharmacist review. Never advise stopping, starting, combining, or changing medication. Preserve original prescriptions and flag uncertain OCR output.

## Suggested workflow
Review specs → resolve open decisions → import Jira CSV → open repo in Kiro → run master prompt → implement one ticket at a time with tests and evidence.
