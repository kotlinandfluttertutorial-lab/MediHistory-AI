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

## In-App AI Agent Features (Post-MVP — F11–F16)

These features add patient-facing AI assistance on top of the reviewed, confirmed data already stored in Room. They use the confirmed `patientValue` fields from OCR review — never raw or unreviewed data.

- F11: **Prescription Understanding Agent** — plain-language explanation of a reviewed prescription
- F12: **Prepare for Doctor Visit Agent** — visit summary card with current medicines and suggested questions
- F13: **Duplicate Detection Explainer Agent** — plain-language explanation of why two medicines were flagged
- F14: **OCR Correction Assistant Agent** — suggests corrections for low-confidence OCR fields (patient must confirm)
- F15: **Medicine History Narrator Agent** — replaces the stub AI summary in Medical History with a real narrative
- F16: **Reminder Advisor Agent** — suggests reminder times based on confirmed dosage instructions

### Agent Feature Principles (apply to F11–F16)

1. **Input is confirmed data only** — agents read REVIEWED fields and confirmed medicines. Never raw OCR output or PENDING fields.
2. **Never infer or fabricate** — missing dose, duration, diagnosis, allergy, or medicine status is never filled in by an agent.
3. **Never give clinical advice** — no agent output says a medicine is safe, unsafe, appropriate, or should be changed.
4. **Always cite the source** — every agent output links to the source prescription document or confirmed field.
5. **Always show a disclaimer** — all agent-generated text carries: *"AI-generated — always verify with your doctor or pharmacist."*
6. **Patient approves every action** — agents suggest; patients confirm. Nothing is applied automatically.
7. **Privacy-first** — on-device processing preferred. No patient data sent to external AI providers without explicit opt-in consent.

### Implementation path

Each agent feature maps to existing infrastructure:

| Feature | Input source | Output destination |
|---|---|---|
| F11 Prescription Understanding | `ExtractedFieldEntity` (REVIEWED) | `PrescriptionDetailScreen` bottom sheet |
| F12 Prepare for Doctor Visit | All REVIEWED prescriptions + medicines | Dashboard card + F07 PDF export |
| F13 Duplicate Explainer | `DuplicateAlert` + `Medicine` rows | `DuplicateAlert.explanation` field + `DuplicateAlertCard` |
| F14 OCR Correction Assistant | Low-confidence `ExtractedFieldEntity` | `SuggestionChip` on `OcrReviewScreen` |
| F15 Medicine History Narrator | All REVIEWED prescriptions | `MedicalHistoryScreen` AI_SUMMARY section |
| F16 Reminder Advisor | `Medicine.dosageInstructions` (from REVIEWED OCR) | Medicine picker dialog in `ReminderScreen` |

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
