# Product Requirements — MediHistory AI

**Document version:** 1.0  
**Status:** Draft — pending stakeholder review  
**Last updated:** 2025

---

## 1. Purpose

This document defines the functional requirements for MediHistory AI. It is the authoritative reference for feature scope, acceptance criteria mapping, and Jira ticket derivation. All implementation work must trace back to a requirement listed here.

---

## 2. Scope

MediHistory AI is a native Android application that:

- Stores and organises prescription images and PDFs from multiple doctors.
- Extracts structured medicine information using OCR and AI, with patient review.
- Detects potential duplicate active ingredients across prescriptions.
- Generates a doctor-ready medical history linked to source documents.
- Operates offline-first with optional encrypted cloud backup.

The application is a **record organisation tool**. It does not diagnose, prescribe, recommend doses, or advise changing medicines.

---

## 3. Intended users

**Primary:** Adult patients managing their own medical records across multiple doctors and specialties.  
**Secondary:** Caregivers managing records on behalf of a family member (future consideration, not in MVP).

---

## 4. Product principles

1. Preserve original prescriptions unchanged — OCR and AI never replace or silently modify source documents.
2. Link every extracted fact to its source document.
3. Treat OCR and AI as fallible — show confidence scores and require patient review for uncertain fields.
4. Separate current, past, and unknown medication status.
5. Use verified active ingredient identity for duplicate matching, not brand text alone.
6. Label duplicate findings as **potential** — never as clinical conclusions.
7. Never advise starting, stopping, or changing medicines.
8. Privacy, consent, export, and deletion are core features, not afterthoughts.
9. Use plain language and accessible Android-native interactions.

---

## 5. Functional requirements

### 5.1 Patient dashboard (F01)

---

**FR-001**  
Feature: F01  
Title: Dashboard summary cards  
Description: On launch (after authentication), the app displays a summary screen showing: total prescriptions stored, number of distinct doctors, total medicines across all prescriptions, count of unreviewed potential duplicate alerts, and count of prescriptions with pending OCR review.  
Rationale: Patients need an at-a-glance status of their records without navigating multiple screens.  
Acceptance criteria: AC-001, AC-002

---

**FR-002**  
Feature: F01  
Title: Recent records list  
Description: The dashboard shows the five most recently added or updated prescriptions, with doctor name, specialty, and date. Each item navigates to the prescription detail screen.  
Rationale: Patients frequently add new records and need quick access to recent entries.  
Acceptance criteria: AC-003

---

**FR-003**  
Feature: F01  
Title: Dashboard navigation entry points  
Description: The dashboard provides clearly labelled navigation to: Prescriptions, Medicine Explorer, Duplicate Alerts, and Medical History. Navigation uses the bottom navigation bar for the four primary destinations.  
Rationale: Mobile navigation patterns require primary destinations to be reachable in one tap.  
Acceptance criteria: AC-004

---

**FR-004**  
Feature: F01  
Title: Empty state guidance  
Description: When no prescriptions have been added, the dashboard displays an empty state with a clear call-to-action to add the first prescription. The empty state uses plain language and does not show zero-value summary cards.  
Rationale: New users need orientation, not a screen full of zeros.  
Acceptance criteria: AC-005

---

### 5.2 Multi-doctor prescription management (F02)

---

**FR-005**  
Feature: F02  
Title: Add prescription via camera capture  
Description: The patient can capture a prescription image using the device camera via CameraX. The capture flow confirms the image is readable before submitting. The original image is stored in app-private storage unchanged.  
Rationale: Many prescriptions are on paper; camera capture is the primary input method.  
Acceptance criteria: AC-006, AC-007

---

**FR-006**  
Feature: F02  
Title: Add prescription via file picker  
Description: The patient can select a prescription image (JPEG, PNG) or PDF from device storage using the Android Photo Picker or system file picker. The original file is stored in app-private storage unchanged.  
Rationale: Patients may have digital prescriptions (e-mails, health apps) that need importing.  
Acceptance criteria: AC-006, AC-008

---

**FR-007**  
Feature: F02  
Title: Doctor profile management  
Description: The patient can create, edit, and delete doctor profiles with: name, specialty (from a predefined list including orthopedics, cardiology, general medicine, neurology, and other), clinic or hospital name, and contact notes. Each prescription is linked to a doctor profile.  
Rationale: Prescriptions from different doctors need to be attributable to their source for meaningful history generation.  
Acceptance criteria: AC-009, AC-010

---

**FR-008**  
Feature: F02  
Title: Prescription metadata entry  
Description: After uploading, the patient enters or confirms: prescription date, linked doctor, and any patient notes. These fields are editable after initial entry.  
Rationale: OCR may not reliably extract dates and doctor names from all prescription formats; patient confirmation is required.  
Acceptance criteria: AC-011

---

**FR-009**  
Feature: F02  
Title: Prescription list and detail  
Description: The prescriptions screen shows a scrollable list of all prescriptions with doctor name, date, and extraction status badge (Pending / Extracted / Needs Review / Error). Each entry opens a detail screen showing the original document image or PDF, extracted medicines list, and metadata.  
Rationale: Patients need to see the full extraction result alongside the original document.  
Acceptance criteria: AC-012, AC-013

---

**FR-010**  
Feature: F02  
Title: Prescription deletion  
Description: The patient can delete a prescription, including its source document and all extracted data. Deletion requires explicit confirmation. Deleting a prescription removes all linked PrescriptionMedicine records and recalculates duplicate alerts. The deletion is reflected in any generated history summaries.  
Rationale: Patients have the right to delete their own records.  
Acceptance criteria: AC-014, AC-055

---

### 5.3 OCR and prescription review (F03)

---

**FR-011**  
Feature: F03  
Title: OCR trigger and status  
Description: OCR processing is triggered automatically after a prescription image or PDF is added, or manually from the prescription detail screen. The prescription shows a processing status indicator while OCR is running. OCR runs as a background WorkManager job and does not block the UI.  
Rationale: OCR may take several seconds; the patient should not wait at a blocking screen.  
Acceptance criteria: AC-015, AC-016

---

**FR-012**  
Feature: F03  
Title: Structured extraction fields  
Description: The extraction pipeline attempts to identify the following fields for each medicine on a prescription: doctor name, clinic information, prescription date, medicine name (brand), active ingredient (when reliably identifiable), strength, formulation, dosage instructions, frequency, route, and duration when documented. Fields not found in the document are left empty and not inferred.  
Rationale: Reliable extraction requires strict field boundaries; invented values are unsafe.  
Acceptance criteria: AC-017, AC-018

---

**FR-013**  
Feature: F03  
Title: Confidence score and review state  
Description: Each extracted field stores a confidence score (0.0–1.0). Each PrescriptionMedicine record has a review state: PENDING_REVIEW, REVIEWED, or REJECTED. Fields below a configurable confidence threshold are highlighted for patient review. The prescription is not considered fully processed until all PENDING_REVIEW items are resolved.  
Rationale: Patients need to know which fields are uncertain and take responsibility for confirming them.  
Acceptance criteria: AC-019, AC-020

---

**FR-014**  
Feature: F03  
Title: Patient review interface  
Description: The review screen shows each extracted field with its confidence indicator. The patient can confirm, edit, or reject each extracted value. Edited values are stored alongside the original OCR output; the OCR output is never overwritten. The review state updates when the patient saves.  
Rationale: Patient review bridges OCR fallibility; both the OCR output and the patient correction must be preserved for auditability.  
Acceptance criteria: AC-021, AC-022

---

**FR-015**  
Feature: F03  
Title: OCR error handling  
Description: If OCR fails (service unavailable, unreadable image, timeout), the prescription is marked with an ERROR status. The patient sees a plain-language explanation and can retry manually or enter medicine details manually. The original document is retained regardless of OCR outcome.  
Rationale: OCR services are not 100% reliable; the app must degrade gracefully.  
Acceptance criteria: AC-023

---

**FR-016**  
Feature: F03  
Title: Manual medicine entry  
Description: The patient can add medicine entries to a prescription manually, without OCR. Manually entered fields are tagged with source = MANUAL. Manual entries follow the same review-state model as extracted entries.  
Rationale: Some prescriptions (e.g. handwritten or low-quality images) may not be processable by OCR; manual entry is the fallback.  
Acceptance criteria: AC-024

---

### 5.4 Consolidated medicine explorer (F04)

---

**FR-017**  
Feature: F04  
Title: Medicine explorer list  
Description: The medicine explorer shows all unique medicines across all prescriptions in a searchable, scrollable list. Each entry shows: medicine name, active ingredient (when known), strength, formulation, most recent prescribing doctor, and most recent prescription date. The list supports lazy loading for large collections.  
Rationale: Patients with many prescriptions need a single view of all their medicines without opening each prescription individually.  
Acceptance criteria: AC-025, AC-026

---

**FR-018**  
Feature: F04  
Title: Medicine search and filter  
Description: The explorer provides a search bar (searches medicine name and active ingredient) and filter options: by doctor, by specialty, by date range, by review state. Active filters are shown as dismissible chips.  
Rationale: A list of 20+ medicines becomes unusable without search and filter.  
Acceptance criteria: AC-027

---

**FR-019**  
Feature: F04  
Title: Medicine detail screen  
Description: Tapping a medicine opens a detail screen showing all prescription occurrences of that medicine (or the same active ingredient across different brands), each with: brand name, strength, formulation, dosage instructions, prescribing doctor, prescription date, and a link to the source prescription. A medicine thumbnail or icon is shown when available.  
Rationale: Patients need to see all occurrences of a medicine to understand their full prescription history for that ingredient.  
Acceptance criteria: AC-028, AC-029

---

**FR-020**  
Feature: F04  
Title: Medicine status grouping  
Description: The explorer separates medicines into three groups: Current (from active/undated prescriptions), Past (from prescriptions marked as historical), and Unknown (status not determinable from available data). Status is patient-assigned, not AI-inferred.  
Rationale: Mixing current and past medicines in one undifferentiated list creates confusion; status must be explicit.  
Acceptance criteria: AC-030

---

**FR-021**  
Feature: F04  
Title: Source document link  
Description: Every medicine entry in the explorer links directly to the source prescription document. The link opens the prescription detail screen with the original document visible.  
Rationale: Traceability to source documents is a core product principle.  
Acceptance criteria: AC-031

---

### 5.5 Potential duplicate ingredient detection (F05)

---

**FR-022**  
Feature: F05  
Title: Duplicate detection engine  
Description: The system compares active ingredients across all reviewed PrescriptionMedicine records. Matching is deterministic, based on normalised active ingredient identity — not brand names. Combination medicines are handled by comparing individual constituent ingredients. Detection runs when new medicines are added or review state changes.  
Rationale: The same active ingredient may appear under different brand names from different doctors; brand-name matching alone misses duplicates and generates false positives.  
Acceptance criteria: AC-032, AC-033

---

**FR-023**  
Feature: F05  
Title: Duplicate alert content  
Description: Each DuplicateAlert shows: the matched active ingredient(s), both medicine names with brands, strengths, formulations, routes, prescribed dosages, prescribing doctors, prescription dates, and links to both source prescriptions. The alert is labelled "Potential duplicate — please review with your doctor or pharmacist."  
Rationale: Explainable alerts give patients the information they need to discuss the issue with a clinician.  
Acceptance criteria: AC-034

---

**FR-024**  
Feature: F05  
Title: Alert review action  
Description: The patient can mark a duplicate alert as Reviewed. This records the timestamp and does not remove the alert from history. The alert is visually distinguished as reviewed versus unreviewed. Marking as reviewed is explicitly not medical clearance; this is stated in the UI.  
Rationale: Patients need to track which alerts they have acted on without the app implying clinical safety.  
Acceptance criteria: AC-035, AC-036

---

**FR-025**  
Feature: F05  
Title: Combination product and formulation handling  
Description: The detection engine accounts for: combination products (e.g. a product containing two active ingredients), different dosage forms (tablet vs. injection), different release formulations (immediate vs. extended release), and different routes of administration. These variations reduce match confidence and are reported in the alert detail.  
Rationale: Naively matching only the active ingredient string ignores clinically relevant distinctions.  
Acceptance criteria: AC-037

---

**FR-026**  
Feature: F05  
Title: Duplicate alert list  
Description: A dedicated screen lists all duplicate alerts, grouped by active ingredient. Each alert shows a summary with both medicine names and an unreviewed/reviewed badge. Alerts are sorted with unreviewed first.  
Rationale: Patients with multiple duplicate alerts need an organised view of all of them.  
Acceptance criteria: AC-038

---

### 5.6 AI medical history summarizer (F06)

---

**FR-027**  
Feature: F06  
Title: History generation  
Description: The patient can trigger generation of a medical history summary. The summary is generated from reviewed and confirmed prescription records only. Unreviewed or rejected extractions are excluded. Each significant statement in the summary links to its source document or record.  
Rationale: A summary built from unreviewed data may contain OCR errors; only patient-reviewed data is fit for handover to a clinician.  
Acceptance criteria: AC-039, AC-040

---

**FR-028**  
Feature: F06  
Title: Summary content structure  
Description: The generated summary follows a chronological structure covering: documented conditions (from prescription context, not AI-inferred), allergies (patient-reported only), medicines with dates and prescribing doctor, any laboratory results attached by the patient, and prescription dates with doctor information. Each section clearly labels its information type: Documented / Patient-reported / Uncertain / AI-generated.  
Rationale: A clinician reading the summary needs to know the provenance of every statement to make safe clinical decisions.  
Acceptance criteria: AC-041

---

**FR-029**  
Feature: F06  
Title: No clinical invention  
Description: The summarisation process never invents diagnoses, allergies, treatment instructions, or clinical history. If a field has no source data, it is omitted or marked "not available." The system prompt for the AI summariser must enforce this rule and the rule must be tested with adversarial inputs.  
Rationale: Invented clinical information in a doctor-handover document is a patient safety risk.  
Acceptance criteria: AC-042, AC-043

---

**FR-030**  
Feature: F06  
Title: Source reference display  
Description: In the summary view, each statement that derives from a source document shows an inline reference indicator. Tapping the indicator opens the linked prescription or document. References use the format [DocID] or a human-readable equivalent.  
Rationale: The doctor must be able to verify any statement by examining the original document.  
Acceptance criteria: AC-044

---

**FR-031**  
Feature: F06  
Title: Summary versioning  
Description: Each generated summary is saved as a separate version with a timestamp and the data snapshot it was generated from. The patient can view and compare previous summaries. Generating a new summary does not delete previous versions.  
Rationale: If the patient adds new prescriptions, the previous summary remains available for comparison.  
Acceptance criteria: AC-045

---

### 5.7 New Doctor Mode (F07)

---

**FR-032**  
Feature: F07  
Title: New Doctor Mode flow  
Description: New Doctor Mode walks the patient through: reviewing the auto-generated summary, selecting which prescriptions and medicines to include, confirming what will be shared, generating a PDF, and sharing via Android share sheet with explicit consent confirmation before sharing. The patient can exit the flow at any point without sending anything.  
Rationale: Sharing medical records must require explicit patient action and informed consent at each step.  
Acceptance criteria: AC-046, AC-047

---

**FR-033**  
Feature: F07  
Title: PDF generation  
Description: The generated PDF includes: patient name (as entered by the patient), summary content with source references, selected medicine list, selected prescription images (as appendices), and a disclaimer stating the document is a patient-compiled record and not a clinical report. PDF is generated locally on device.  
Rationale: The PDF must be self-contained and clearly labelled to avoid being mistaken for a clinical document.  
Acceptance criteria: AC-048

---

**FR-034**  
Feature: F07  
Title: Sharing consent and privacy  
Description: Before sharing via the share sheet, the app shows a preview of the exact content being shared, a plain-language consent statement, and the sharing destination (app name). The patient must tap a distinct confirmation button. The share action is logged in the audit log (destination app name, timestamp, document ID — no document content).  
Rationale: Patients must know exactly what they are sharing and to whom before the share action occurs.  
Acceptance criteria: AC-049, AC-056

---

### 5.8 Medication reminders and adherence (F08)

---

**FR-035**  
Feature: F08  
Title: Optional medication reminders  
Description: The patient can set optional reminders for any reviewed medicine. Reminders are based on the prescription's dosage frequency field. The patient can override the reminder schedule. Reminders are stored locally and use Android notification channels. Reminders are clearly labelled as based on a prescription from a specific date; they are not real-time clinical instructions.  
Rationale: Optional reminders help patients follow prescribed regimens; the prescription-date label prevents stale instructions from being treated as current.  
Acceptance criteria: AC-050

---

**FR-036**  
Feature: F08  
Title: Dose logging  
Description: The patient can log individual doses as Taken, Skipped, or Missed. The log stores the patient's reported action and timestamp, not a clinical record of actual administration. The adherence view shows a summary of logged doses per medicine. Logged doses are distinct from prescribed instructions.  
Rationale: Distinguishing prescribed instructions from actual patient-reported use prevents the app from misrepresenting adherence as confirmed administration.  
Acceptance criteria: AC-051

---

### 5.9 Medication interaction checks (F09)

---

**FR-037**  
Feature: F09  
Title: Interaction check with configured reference  
Description: The interaction check feature is available only when a licensed, appropriately maintained medication reference source is configured via MED_REFERENCE_PROVIDER. The check compares the patient's current medicine list against the reference. Results show the medicines involved, the nature of the reported interaction, and a recommendation to discuss with a doctor or pharmacist.  
Rationale: Interaction checking without a reliable reference source is worse than no checking; it may miss interactions or create false reassurance.  
Acceptance criteria: AC-052

---

**FR-038**  
Feature: F09  
Title: Interaction check unavailable state  
Description: If the reference source is not configured or is unavailable, the feature shows a clear message: "Interaction checking is not available. Please discuss your medicines with your doctor or pharmacist." It does not imply the combination is safe. It does not show partial results.  
Rationale: An incomplete interaction check may create false reassurance that is more dangerous than no check.  
Acceptance criteria: AC-053

---

### 5.10 Privacy and security (F10)

---

**FR-039**  
Feature: F10  
Title: Data export and deletion  
Description: The patient can export all their data (prescription images, extracted data, history summaries) as an encrypted ZIP or JSON package. The patient can request full account deletion, which removes all records, documents, extracted data, reminders, and generated summaries from both local storage and the backend (where sync is enabled). Deletion is confirmed with a plain-language warning. The deletion timeline and any backup retention limitations are explained before confirmation.  
Rationale: Patients have the right to their own data and the right to delete it; these are non-negotiable.  
Acceptance criteria: AC-054, AC-055

---

**FR-040**  
Feature: F10  
Title: Cloud backup opt-in  
Description: Cloud backup is opt-in. The backup status (enabled/disabled, last successful backup, last error) is visible at all times in the settings screen. Enabling backup requires explicit patient action. Disabling backup explains what happens to existing cloud copies. Backup is encrypted before upload using a key derived from a patient-controlled secret.  
Rationale: Medical data must not be sent to a cloud service without explicit informed consent.  
Acceptance criteria: AC-057, AC-058

---

## 6. MVP exclusions

The following capabilities are explicitly excluded from the MVP:

| Excluded feature | Reason |
|-----------------|--------|
| Diagnosis suggestion | Clinical scope; requires regulatory oversight |
| Prescribing or dose recommendations | Clinical scope; requires regulatory oversight |
| Dose optimisation | Clinical scope; requires regulatory oversight |
| Pharmacy ordering or integration | Out of scope for record management tool |
| KYC / identity verification | Not required for personal record organiser |
| Automated medication changes | Contradicts product principle; requires regulatory oversight |
| Caregiver / multi-patient accounts | Deferred to post-MVP |
| Lab result interpretation | Clinical scope |
| Wearable / device integration | Out of scope for MVP |

---

## 7. Out of scope

- Clinical decision support systems (CDSS).
- Integration with hospital electronic health record (EHR) systems.
- Real-time medicine price or availability information.
- Telemedicine or doctor consultation booking.
- Insurance claim processing.

---

*This document must be reviewed and approved before Jira ticket creation. All requirement IDs are stable once approved — do not renumber.*
