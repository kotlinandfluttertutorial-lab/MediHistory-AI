# Data Model — MediHistory AI

**Document version:** 1.0  
**Status:** Draft — open decisions on encryption and sync ID strategy noted

All entities are described in terms of both the Android Room schema (local) and the PostgreSQL backend schema (sync, when enabled). Field names use snake_case. Primary keys are UUIDs generated on the client for offline-first support; the backend uses the same UUID as the canonical ID.

---

## 1. Entity relationship overview

```
Patient ──< Prescription ──< PrescriptionMedicine >── Medicine
Patient ──< Doctor
Prescription >── Doctor
PrescriptionMedicine >── DuplicateAlert (N:M via junction implied)
Patient ──< MedicalHistorySummary ──< SourceReference >── Prescription
Patient ──< MedicationReminder >── PrescriptionMedicine
Patient ──< DoseLog >── MedicationReminder
Patient ──< AuditLogEntry
```

---

## 2. Entity definitions

---

### 2.1 Patient

Represents the account holder. In MVP, one user = one patient.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | Client-generated UUID |
| email | VARCHAR(320) | UNIQUE NOT NULL | Used for authentication |
| display_name | VARCHAR(200) | NOT NULL | As entered by patient; shown in UI and PDF export |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | Account creation time |
| updated_at | TIMESTAMPTZ | NOT NULL | Updated on every write |
| deleted_at | TIMESTAMPTZ | NULL | Soft delete timestamp; hard delete follows after retention period |

Indexes: `email` (unique), `deleted_at` (partial for active records).

Room entity: `PatientEntity`. Backend ORM: `PatientModel`.

---

### 2.2 Doctor

A doctor profile created and managed by the patient.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | Client-generated UUID |
| patient_id | UUID | FK → Patient.id NOT NULL | Patient-scoped |
| name | VARCHAR(300) | NOT NULL | As entered by patient |
| specialty | VARCHAR(100) | NOT NULL | From predefined list (see glossary) or "Other" |
| specialty_custom | VARCHAR(200) | NULL | Free text when specialty = "Other" |
| clinic_name | VARCHAR(300) | NULL | |
| contact_notes | TEXT | NULL | Patient's free-text notes |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL | |
| deleted_at | TIMESTAMPTZ | NULL | Soft delete |

Indexes: `patient_id`.

---

### 2.3 Prescription

The top-level record for an uploaded prescription document.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | Client-generated UUID |
| patient_id | UUID | FK → Patient.id NOT NULL | Patient-scoped |
| doctor_id | UUID | FK → Doctor.id NULL | NULL if doctor was deleted |
| prescription_date | DATE | NULL | May be null if not on document; confirmed by patient |
| source_document_path | VARCHAR(500) | NOT NULL | Relative path within app-private storage |
| source_document_hash | VARCHAR(64) | NOT NULL | SHA-256 hex digest of original file |
| source_document_mime_type | VARCHAR(100) | NOT NULL | "image/jpeg", "image/png", "application/pdf" |
| ocr_status | ENUM | NOT NULL | PENDING, PROCESSING, EXTRACTED, NEEDS_REVIEW, REVIEWED, ERROR |
| ocr_error_message | TEXT | NULL | Plain-language error; no health content |
| patient_notes | TEXT | NULL | Patient's own notes |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL | |
| deleted_at | TIMESTAMPTZ | NULL | Soft delete; hard delete cascades to PrescriptionMedicine |

Indexes: `patient_id`, `doctor_id`, `prescription_date`, `ocr_status`.

Room: foreign key `doctor_id` uses `onDelete = SET_NULL`.  
Room: foreign key `patient_id` uses `onDelete = CASCADE`.

---

### 2.4 PrescriptionMedicine

A single medicine entry extracted from or manually entered for a prescription.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| prescription_id | UUID | FK → Prescription.id NOT NULL CASCADE | |
| patient_id | UUID | FK → Patient.id NOT NULL | Denormalized for patient-scoped queries |
| medicine_id | UUID | FK → Medicine.id NULL | Linked after normalization |
| source | ENUM | NOT NULL | OCR, MANUAL |
| brand_name | VARCHAR(300) | NULL | As extracted or entered |
| brand_name_ocr_raw | VARCHAR(300) | NULL | Original OCR text before patient edit |
| brand_name_confidence | FLOAT | NULL | 0.0–1.0 |
| active_ingredient | VARCHAR(300) | NULL | Normalized if possible |
| active_ingredient_ocr_raw | VARCHAR(300) | NULL | |
| active_ingredient_confidence | FLOAT | NULL | |
| strength | VARCHAR(100) | NULL | e.g. "500 mg", "5 mg/mL" |
| strength_ocr_raw | VARCHAR(100) | NULL | |
| strength_confidence | FLOAT | NULL | |
| formulation | VARCHAR(100) | NULL | e.g. "tablet", "capsule", "injection" |
| formulation_confidence | FLOAT | NULL | |
| dosage | VARCHAR(200) | NULL | e.g. "1 tablet twice daily" |
| dosage_confidence | FLOAT | NULL | |
| frequency | VARCHAR(100) | NULL | e.g. "twice daily", "BD" |
| frequency_confidence | FLOAT | NULL | |
| route | VARCHAR(100) | NULL | e.g. "oral", "IV", "topical" |
| route_confidence | FLOAT | NULL | |
| duration | VARCHAR(100) | NULL | e.g. "7 days", "ongoing" |
| duration_confidence | FLOAT | NULL | |
| review_state | ENUM | NOT NULL | PENDING_REVIEW, REVIEWED, REJECTED |
| reviewed_at | TIMESTAMPTZ | NULL | |
| medicine_status | ENUM | NOT NULL DEFAULT 'UNKNOWN' | CURRENT, PAST, UNKNOWN — patient-assigned |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

Indexes: `prescription_id`, `patient_id`, `review_state`, `active_ingredient` (for duplicate detection).

---

### 2.5 Medicine

Normalized medicine catalogue. One entry per unique canonical active ingredient identity. Populated by the normalization service; not user-created directly.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| active_ingredient | VARCHAR(300) | NOT NULL UNIQUE | Canonical normalised form |
| generic_name | VARCHAR(300) | NULL | INN name if available |
| brand_names | TEXT[] / JSON | NULL | Array of known brand names |
| is_combination | BOOLEAN | NOT NULL DEFAULT false | True if multiple active ingredients |
| combination_ingredients | TEXT[] / JSON | NULL | For combination products |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

Note: In Room, arrays are stored as JSON strings (no native array type in SQLite). In PostgreSQL, use `TEXT[]`.

---

### 2.6 DuplicateAlert

A potential duplicate ingredient match detected between two PrescriptionMedicine records.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| patient_id | UUID | FK → Patient.id NOT NULL | |
| medicine_a_id | UUID | FK → PrescriptionMedicine.id NOT NULL | |
| medicine_b_id | UUID | FK → PrescriptionMedicine.id NOT NULL | |
| matched_active_ingredient | VARCHAR(300) | NOT NULL | The ingredient that matched |
| match_type | ENUM | NOT NULL | EXACT, PARTIAL_COMBINATION, REDUCED_CONFIDENCE |
| match_confidence | FLOAT | NOT NULL | 0.0–1.0 |
| formulation_notes | TEXT | NULL | Notes on dosage form or route differences |
| reviewed_by_patient | BOOLEAN | NOT NULL DEFAULT false | |
| reviewed_at | TIMESTAMPTZ | NULL | |
| voided | BOOLEAN | NOT NULL DEFAULT false | True if one medicine was deleted or rejected |
| voided_at | TIMESTAMPTZ | NULL | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

Indexes: `patient_id`, `reviewed_by_patient`, `voided`.

---

### 2.7 MedicalHistorySummary

A generated doctor-ready summary, versioned.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| patient_id | UUID | FK → Patient.id NOT NULL | |
| version_number | INTEGER | NOT NULL | Increments per patient; starts at 1 |
| generated_at | TIMESTAMPTZ | NOT NULL | |
| summary_json | TEXT | NOT NULL | Structured summary with inline source reference IDs |
| input_snapshot_hash | VARCHAR(64) | NOT NULL | SHA-256 of the reviewed records used as input |
| ai_provider | VARCHAR(100) | NULL | Logged for auditability (provider name, no keys) |
| ai_model_version | VARCHAR(100) | NULL | |
| is_current | BOOLEAN | NOT NULL DEFAULT false | Most recently generated version |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

Note: `summary_json` contains structured content, not prescription text. Source references are IDs (Prescription IDs), not document content.

---

### 2.8 SourceReference (embedded in MedicalHistorySummary)

Source references are stored as a JSON array within `summary_json` rather than as a separate table, to keep the summary self-contained. Each reference contains:

```json
{
  "ref_id": "ref-001",
  "prescription_id": "uuid",
  "prescription_date": "2024-01-15",
  "doctor_name": "Dr. Smith",
  "field_referenced": "medicine_name",
  "statement_key": "medicines.metformin.first_prescribed"
}
```

---

### 2.9 MedicationReminder

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| patient_id | UUID | FK → Patient.id NOT NULL | |
| prescription_medicine_id | UUID | FK → PrescriptionMedicine.id NOT NULL | |
| medicine_name_display | VARCHAR(300) | NOT NULL | Denormalized for display without JOIN |
| frequency_display | VARCHAR(200) | NOT NULL | From prescription; patient may override |
| prescription_date_display | DATE | NOT NULL | Shown in reminder label ("Based on prescription dated...") |
| reminder_times | TEXT | NOT NULL | JSON array of daily times ["08:00", "20:00"] |
| is_active | BOOLEAN | NOT NULL DEFAULT true | |
| notification_channel_id | VARCHAR(200) | NOT NULL | Android notification channel |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

---

### 2.10 DoseLog

Patient-reported dose administration record.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| patient_id | UUID | FK → Patient.id NOT NULL | |
| reminder_id | UUID | FK → MedicationReminder.id NOT NULL | |
| prescription_medicine_id | UUID | FK → PrescriptionMedicine.id NOT NULL | |
| logged_at | TIMESTAMPTZ | NOT NULL | When the patient logged the dose |
| scheduled_for | TIMESTAMPTZ | NULL | The scheduled time this log corresponds to |
| status | ENUM | NOT NULL | TAKEN, SKIPPED, MISSED |
| patient_reported | BOOLEAN | NOT NULL DEFAULT true | Always true; included for explicit labelling |
| notes | TEXT | NULL | Patient's optional notes |

---

### 2.11 AuditLogEntry

Metadata-only audit trail. No health content ever.

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | UUID | PK | |
| actor_patient_id | UUID | NOT NULL | The patient who performed the action |
| action | VARCHAR(100) | NOT NULL | e.g. LOGIN, ADD_PRESCRIPTION, DELETE_PRESCRIPTION, OCR_TRIGGERED, REVIEW_STATE_CHANGED, SUMMARY_GENERATED, PDF_EXPORTED, SHARE_PDF, ACCOUNT_DELETION_REQUESTED |
| entity_type | VARCHAR(100) | NULL | e.g. Prescription, PrescriptionMedicine, MedicalHistorySummary |
| entity_id | UUID | NULL | ID of the affected entity |
| destination_app | VARCHAR(200) | NULL | For SHARE_PDF: target app package name |
| timestamp | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

**Critical constraint:** No column in AuditLogEntry may store prescription text, medicine names, dosage values, summary content, or any patient health information. Violations of this constraint are a critical security issue (see security-threat-model.md).

Indexes: `actor_patient_id`, `timestamp`, `action`.

---

## 3. Enumeration types

```sql
-- OCR processing state
CREATE TYPE ocr_status AS ENUM ('PENDING', 'PROCESSING', 'EXTRACTED', 'NEEDS_REVIEW', 'REVIEWED', 'ERROR');

-- Extraction review state
CREATE TYPE review_state AS ENUM ('PENDING_REVIEW', 'REVIEWED', 'REJECTED');

-- Medicine status (patient-assigned)
CREATE TYPE medicine_status AS ENUM ('CURRENT', 'PAST', 'UNKNOWN');

-- Data source
CREATE TYPE data_source AS ENUM ('OCR', 'MANUAL');

-- Duplicate match type
CREATE TYPE match_type AS ENUM ('EXACT', 'PARTIAL_COMBINATION', 'REDUCED_CONFIDENCE');

-- Dose log status
CREATE TYPE dose_status AS ENUM ('TAKEN', 'SKIPPED', 'MISSED');
```

Room equivalent: String-backed `@TypeConverter` for each enum.

---

## 4. Open data model decisions

| ID | Decision | Impact |
|----|---------|--------|
| DM-001 | Room encryption library (SQLCipher vs. EncryptedFile) | All entity definitions; key schema |
| DM-002 | Sync conflict resolution strategy | SyncWorker logic; may require a `sync_version` column on mutable entities |
| DM-003 | Array field storage in Room (JSON string vs. relation table) | Medicine.brand_names, MedicalHistorySummary.source_references |
| DM-004 | Audit log retention period | AuditLogEntry cleanup job; backend storage costs |

---

*Schema versions: increment this document's version number when any entity definition changes after Jira tickets have been created against it.*
