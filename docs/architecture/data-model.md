# Proposed Data Model
- User: account ID, preferences, consent state.
- MedicalDocument: owner ID, type, original reference, hash, import date, known document date/prescriber label.
- DocumentVersion: immutable original/version metadata and storage reference.
- ExtractedField: field, raw/normalized value, confidence, review status, source page/region.
- MedicationEntry: raw name, verified ingredient ID if known, brand, strength, formulation, dosage text as written, current/past/unknown status, source and review state.
- IngredientReference: identifier, canonical name, source, version/update date.
- ReviewEvent: field/entry, action, timestamp, minimal audit data.
- DuplicateFinding: matched entries, matching basis, evidence, uncertainty, review status.
- Report: selected sections/documents, generation date, export reference.
- SyncJob: status, retries, last sync, conflict state.
- ShareGrant: selected resources and expiry/revocation metadata if links are implemented.
Enforce owner-scoped authorization. Never trust client-supplied owner IDs as authorization.
