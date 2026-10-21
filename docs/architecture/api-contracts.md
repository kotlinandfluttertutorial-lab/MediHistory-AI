# API Contracts — MediHistory AI Backend

**Document version:** 1.0  
**Status:** Draft — schemas are informative until the backend is implemented

All endpoints use JSON request and response bodies unless noted. All timestamps are ISO 8601 UTC. All IDs are UUIDs. Authentication is JWT Bearer unless marked as public. Rate limits are per-IP unless noted.

Base URL: `https://<configured-backend-host>/api/v1`

---

## Auth endpoints

---

### POST /auth/register

Auth required: No (public)  
Rate limit: 10 requests / 15 minutes / IP  
Description: Register a new patient account.

Request body:
```json
{
  "email": "patient@example.com",
  "password": "...",
  "display_name": "Jane Doe"
}
```

Response 201:
```json
{
  "patient_id": "uuid",
  "email": "patient@example.com",
  "display_name": "Jane Doe",
  "created_at": "2024-01-01T00:00:00Z"
}
```

Error codes:
- 422: Validation error (invalid email format, password too short)
- 409: Email already registered
- 429: Rate limit exceeded

---

### POST /auth/login

Auth required: No (public)  
Rate limit: 20 requests / 15 minutes / IP  
Description: Authenticate and obtain access + refresh tokens.

Request body:
```json
{
  "email": "patient@example.com",
  "password": "..."
}
```

Response 200:
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "expires_in": 1800,
  "refresh_token": "eyJ..."
}
```

Error codes:
- 401: Invalid credentials
- 422: Validation error
- 429: Rate limit exceeded

---

### POST /auth/refresh

Auth required: Refresh token in request body  
Rate limit: 30 requests / hour / patient  
Description: Exchange a valid refresh token for a new access token.

Request body:
```json
{
  "refresh_token": "eyJ..."
}
```

Response 200:
```json
{
  "access_token": "eyJ...",
  "token_type": "bearer",
  "expires_in": 1800
}
```

Error codes:
- 401: Invalid or expired refresh token
- 429: Rate limit exceeded

---

### POST /auth/logout

Auth required: Bearer token  
Rate limit: 60 requests / hour / patient  
Description: Invalidate the current refresh token.

Request body: empty  
Response 204: No content  
Error codes: 401 (not authenticated)

---

## Patient endpoints

---

### GET /patients/me

Auth required: Bearer token  
Rate limit: 120 requests / hour / patient  
Description: Retrieve the authenticated patient's profile.

Response 200:
```json
{
  "id": "uuid",
  "email": "patient@example.com",
  "display_name": "Jane Doe",
  "created_at": "2024-01-01T00:00:00Z",
  "updated_at": "2024-01-01T00:00:00Z"
}
```

Error codes: 401

---

### PUT /patients/me

Auth required: Bearer token  
Rate limit: 30 requests / hour / patient  
Description: Update the authenticated patient's profile (display name only; email change requires a separate verification flow not in MVP).

Request body:
```json
{
  "display_name": "Jane Smith"
}
```

Response 200: Updated patient profile (same schema as GET)  
Error codes: 401, 422

---

## Doctor endpoints

---

### GET /patients/me/doctors

Auth required: Bearer token  
Rate limit: 120 requests / hour / patient  
Description: List all doctor profiles for the authenticated patient.

Response 200:
```json
{
  "doctors": [
    {
      "id": "uuid",
      "name": "Dr. A",
      "specialty": "Cardiology",
      "specialty_custom": null,
      "clinic_name": "Heart Clinic",
      "contact_notes": null,
      "created_at": "...",
      "updated_at": "..."
    }
  ],
  "total": 1
}
```

Error codes: 401

---

### POST /patients/me/doctors

Auth required: Bearer token  
Rate limit: 60 requests / hour / patient  
Description: Create a new doctor profile.

Request body:
```json
{
  "name": "Dr. A",
  "specialty": "Cardiology",
  "specialty_custom": null,
  "clinic_name": "Heart Clinic",
  "contact_notes": null
}
```

Response 201: Doctor object  
Error codes: 401, 422

---

### GET /patients/me/doctors/{doctor_id}

Auth required: Bearer token  
Response 200: Doctor object  
Error codes: 401, 404 (doctor not found or not owned by this patient)

---

### PUT /patients/me/doctors/{doctor_id}

Auth required: Bearer token  
Request body: Same as POST (all fields optional — PATCH semantics)  
Response 200: Updated doctor object  
Error codes: 401, 404, 422

---

### DELETE /patients/me/doctors/{doctor_id}

Auth required: Bearer token  
Description: Soft-delete the doctor profile. Prescriptions linked to this doctor retain their data; the doctor reference is set to null.  
Response 204: No content  
Error codes: 401, 404

---

## Prescription endpoints

---

### GET /patients/me/prescriptions

Auth required: Bearer token  
Rate limit: 120 requests / hour / patient  
Query params: `page` (int, default 1), `page_size` (int, default 20, max 100), `ocr_status` (filter), `doctor_id` (filter)

Response 200:
```json
{
  "prescriptions": [
    {
      "id": "uuid",
      "doctor_id": "uuid",
      "doctor_name": "Dr. A",
      "prescription_date": "2024-01-15",
      "ocr_status": "REVIEWED",
      "patient_notes": null,
      "medicine_count": 3,
      "created_at": "...",
      "updated_at": "..."
    }
  ],
  "total": 12,
  "page": 1,
  "page_size": 20
}
```

Note: `source_document_path` is not returned in list responses. Use the detail endpoint.  
Error codes: 401

---

### POST /patients/me/prescriptions

Auth required: Bearer token  
Rate limit: 60 requests / hour / patient  
Content-Type: multipart/form-data  
Description: Upload a prescription document and create a prescription record.

Form fields:
- `file`: The prescription image or PDF (required)
- `prescription_date`: ISO 8601 date string (optional; confirmed by patient after OCR)
- `doctor_id`: UUID (optional)
- `patient_notes`: String (optional)

Response 201:
```json
{
  "id": "uuid",
  "ocr_status": "PENDING",
  "created_at": "..."
}
```

Error codes: 401, 413 (file too large, limit TBD), 415 (unsupported media type), 422, 429

---

### GET /patients/me/prescriptions/{prescription_id}

Auth required: Bearer token  
Description: Full prescription detail including all extracted medicines.

Response 200:
```json
{
  "id": "uuid",
  "doctor_id": "uuid",
  "doctor_name": "Dr. A",
  "prescription_date": "2024-01-15",
  "ocr_status": "REVIEWED",
  "document_url": "/patients/me/prescriptions/{id}/document",
  "patient_notes": null,
  "medicines": [ /* array of PrescriptionMedicine objects */ ],
  "created_at": "...",
  "updated_at": "..."
}
```

Error codes: 401, 404

---

### GET /patients/me/prescriptions/{prescription_id}/document

Auth required: Bearer token  
Description: Stream the original prescription document (image or PDF).  
Response 200: Binary file stream with appropriate Content-Type  
Error codes: 401, 404

---

### PUT /patients/me/prescriptions/{prescription_id}

Auth required: Bearer token  
Description: Update editable metadata (prescription_date, doctor_id, patient_notes). Does not replace the source document.  
Request body: Partial update (any of the editable fields)  
Response 200: Updated prescription object  
Error codes: 401, 404, 422

---

### DELETE /patients/me/prescriptions/{prescription_id}

Auth required: Bearer token  
Description: Delete a prescription, its document, all extracted medicines, and associated duplicate alerts.  
Response 204: No content  
Error codes: 401, 404

---

### POST /patients/me/prescriptions/{prescription_id}/ocr

Auth required: Bearer token  
Rate limit: 10 requests / hour / patient  
Description: Trigger (or re-trigger) OCR processing for a prescription.

Response 202:
```json
{
  "prescription_id": "uuid",
  "ocr_status": "PROCESSING",
  "job_id": "uuid"
}
```

Error codes: 401, 404, 409 (already processing), 429

---

### PUT /patients/me/prescriptions/{prescription_id}/medicines/{medicine_id}

Auth required: Bearer token  
Description: Update patient review of a single PrescriptionMedicine record (confirm, edit, or reject extracted fields).

Request body:
```json
{
  "brand_name": "Edited value or null",
  "active_ingredient": "Confirmed or edited value",
  "strength": "500 mg",
  "formulation": "tablet",
  "dosage": "1 tablet twice daily",
  "frequency": "twice daily",
  "route": "oral",
  "duration": "30 days",
  "review_state": "REVIEWED",
  "medicine_status": "CURRENT"
}
```

Response 200: Updated PrescriptionMedicine object  
Error codes: 401, 404, 422

---

## Medicine endpoints

---

### GET /patients/me/medicines

Auth required: Bearer token  
Rate limit: 120 requests / hour / patient  
Query params: `search` (string), `doctor_id`, `medicine_status` (CURRENT/PAST/UNKNOWN), `page`, `page_size`

Response 200:
```json
{
  "medicines": [
    {
      "active_ingredient": "metformin",
      "generic_name": "metformin hydrochloride",
      "occurrences": [
        {
          "prescription_medicine_id": "uuid",
          "brand_name": "Glucophage",
          "strength": "500 mg",
          "formulation": "tablet",
          "dosage": "1 tablet twice daily",
          "doctor_name": "Dr. A",
          "prescription_date": "2024-01-15",
          "prescription_id": "uuid",
          "medicine_status": "CURRENT"
        }
      ]
    }
  ],
  "total": 10,
  "page": 1,
  "page_size": 20
}
```

Error codes: 401

---

### GET /patients/me/medicines/{active_ingredient_slug}

Auth required: Bearer token  
Description: Detail for one active ingredient, all occurrences.  
Response 200: Single medicine group (same structure as one item from the list)  
Error codes: 401, 404

---

## Duplicate alert endpoints

---

### GET /patients/me/duplicates

Auth required: Bearer token  
Rate limit: 120 requests / hour / patient  
Query params: `reviewed` (bool), `page`, `page_size`

Response 200:
```json
{
  "alerts": [
    {
      "id": "uuid",
      "matched_active_ingredient": "metformin",
      "match_type": "EXACT",
      "match_confidence": 0.95,
      "medicine_a": { /* PrescriptionMedicine summary */ },
      "medicine_b": { /* PrescriptionMedicine summary */ },
      "reviewed_by_patient": false,
      "reviewed_at": null,
      "formulation_notes": null
    }
  ],
  "total": 3,
  "unreviewed_count": 2
}
```

Error codes: 401

---

### GET /patients/me/duplicates/{alert_id}

Auth required: Bearer token  
Response 200: Full alert detail including all medicine fields and source prescription links  
Error codes: 401, 404

---

### PUT /patients/me/duplicates/{alert_id}/reviewed

Auth required: Bearer token  
Description: Mark a duplicate alert as reviewed by the patient. Does not imply clinical clearance.

Request body: `{}` (empty)  
Response 200:
```json
{
  "id": "uuid",
  "reviewed_by_patient": true,
  "reviewed_at": "2024-01-15T10:30:00Z"
}
```

Error codes: 401, 404, 409 (already reviewed)

---

## Medical history endpoints

---

### POST /patients/me/history/generate

Auth required: Bearer token  
Rate limit: 5 requests / hour / patient  
Description: Generate a new medical history summary from all REVIEWED PrescriptionMedicine records.

Request body: `{}` (no parameters in MVP; future: record selection)  
Response 202:
```json
{
  "summary_id": "uuid",
  "status": "GENERATING",
  "job_id": "uuid"
}
```

Error codes: 401, 409 (generation already in progress), 429

---

### GET /patients/me/history

Auth required: Bearer token  
Description: List all generated history summary versions.

Response 200:
```json
{
  "summaries": [
    {
      "id": "uuid",
      "version_number": 3,
      "generated_at": "2024-01-20T12:00:00Z",
      "is_current": true,
      "medicine_count": 8,
      "prescription_count": 5
    }
  ],
  "total": 3
}
```

Error codes: 401

---

### GET /patients/me/history/{summary_id}

Auth required: Bearer token  
Description: Full summary content with source references.

Response 200:
```json
{
  "id": "uuid",
  "version_number": 3,
  "generated_at": "2024-01-20T12:00:00Z",
  "is_current": true,
  "summary": {
    "sections": [
      {
        "type": "medicines",
        "label": "Documented",
        "content": "...",
        "source_refs": ["ref-001", "ref-002"]
      }
    ],
    "source_references": [
      {
        "ref_id": "ref-001",
        "prescription_id": "uuid",
        "prescription_date": "2024-01-15",
        "doctor_name": "Dr. A"
      }
    ]
  }
}
```

Error codes: 401, 404

---

## Export endpoints

---

### POST /patients/me/export/pdf

Auth required: Bearer token  
Rate limit: 5 requests / hour / patient  
Description: Generate a PDF export of the selected summary and prescriptions.

Request body:
```json
{
  "summary_id": "uuid",
  "include_prescription_ids": ["uuid1", "uuid2"],
  "purpose": "new_doctor_consultation"
}
```

Response 202:
```json
{
  "export_id": "uuid",
  "status": "GENERATING"
}
```

Error codes: 401, 422, 429

---

### GET /patients/me/export/{export_id}

Auth required: Bearer token  
Description: Poll export status or download the generated PDF.

Response 200 (pending):
```json
{ "status": "GENERATING" }
```

Response 200 (ready):
```json
{ "status": "READY", "download_url": "/patients/me/export/{id}/download" }
```

Error codes: 401, 404

---

### GET /patients/me/export/{export_id}/download

Auth required: Bearer token  
Description: Download the generated PDF. URL is valid for 1 hour.  
Response 200: Binary PDF stream  
Error codes: 401, 404, 410 (expired)

---

## Reminder endpoints

---

### GET /patients/me/reminders

Auth required: Bearer token  
Response 200: `{ "reminders": [...], "total": N }`  
Error codes: 401

---

### POST /patients/me/reminders

Auth required: Bearer token  
Request body:
```json
{
  "prescription_medicine_id": "uuid",
  "reminder_times": ["08:00", "20:00"],
  "frequency_display": "Twice daily"
}
```

Response 201: Reminder object  
Error codes: 401, 422

---

### PUT /patients/me/reminders/{reminder_id}

Auth required: Bearer token  
Request body: Partial update (is_active, reminder_times, frequency_display)  
Response 200: Updated reminder object  
Error codes: 401, 404, 422

---

### DELETE /patients/me/reminders/{reminder_id}

Auth required: Bearer token  
Response 204: No content  
Error codes: 401, 404

---

## Interaction check endpoint

---

### POST /patients/me/interactions/check

Auth required: Bearer token  
Rate limit: 10 requests / hour / patient  
Description: Check for reported interactions among the patient's current medicines. Only available when MED_REFERENCE_PROVIDER is configured.

Request body: `{}` (uses patient's CURRENT medicines automatically)  
Response 200 (provider available):
```json
{
  "checked": true,
  "medicines_checked": ["metformin", "lisinopril"],
  "interactions": [
    {
      "medicines_involved": ["med-a", "med-b"],
      "description": "...",
      "severity": "moderate",
      "review_note": "Discuss with your doctor or pharmacist."
    }
  ],
  "disclaimer": "This check uses [provider name]. It is not a substitute for professional medical advice."
}
```

Response 200 (provider unavailable):
```json
{
  "checked": false,
  "reason": "unavailable",
  "message": "Interaction checking is not available. Please discuss your medicines with your doctor or pharmacist."
}
```

Error codes: 401, 429

---

## Common error response format

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Human-readable description",
    "details": { }
  }
}
```

Standard HTTP codes used:
- 200 OK, 201 Created, 202 Accepted, 204 No Content
- 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found
- 409 Conflict, 410 Gone, 413 Request Entity Too Large, 415 Unsupported Media Type
- 422 Unprocessable Entity (validation), 429 Too Many Requests
- 500 Internal Server Error (no health content in error body)

---

*API version is v1. Breaking changes require a new version prefix. All changes to this document must be reflected in Pydantic schemas before implementation.*
