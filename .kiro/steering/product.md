# MediHistory AI — Product Steering

## Product Purpose
"All prescriptions. All medicines. One doctor-ready medical history."

MediHistory AI helps patients compile, organise, and share their complete medication and prescription history. It is a personal health record tool, not a clinical system.

## Primary Users
Patients managing their own medical records — people who see multiple doctors, take multiple medicines, or need to share their history with a new healthcare provider.

## MVP Scope
Features F01–F10:
- F01: Dashboard and navigation
- F02: Prescription management (upload, camera, list, detail)
- F03: OCR extraction with review flow
- F04: Medicine explorer with search and filters
- F05: Duplicate detection and alert management
- F06: Medical history summarisation
- F07: New Doctor Mode — content selection and PDF export
- F08: Reminders and adherence tracking
- F09: Interaction checking (with external provider — optional in MVP)
- F10: Security, privacy, consent, backup, and account deletion

## MVP Exclusions
The MVP explicitly excludes:
- Diagnosis or differential diagnosis
- Prescribing recommendations
- Dose optimisation
- Pharmacy ordering
- KYC (Know Your Customer) identity verification
- Automated medication changes
- Any feature that constitutes medical advice

## Product Principles
1. **Preserve originals** — OCR never replaces the source document. Original prescriptions are stored unchanged.
2. **Link to source** — Every extracted fact links to the source prescription document.
3. **Show confidence and review state** — OCR confidence scores and review states are always visible. AI is fallible.
4. **Separate medication status** — Current, Past, and Unknown medication statuses are kept separate. Status is patient-assigned only.
5. **Active ingredient identity** — Duplicate matching uses verified active ingredient identity, not brand text alone.
6. **Label as potential duplicates** — Duplicates are labelled "potential duplicate" — never "duplicate medication" or a clinical conclusion.
7. **Never advise on medicines** — The app never advises starting, stopping, or changing medicines.
8. **Privacy and consent are core** — Sharing requires explicit user action and preview. Cloud backup is opt-in. Data is private by default.
9. **Offline access is core** — All core patient data is accessible offline. Sync is a supplement, not a requirement.

## Definition of Done
A ticket is done when:
- All acceptance criteria pass
- Tests exist for all new behaviour and pass in CI
- Privacy impact reviewed (no new data collected without justification)
- Documentation and traceability updated
- No secrets committed to the repository
