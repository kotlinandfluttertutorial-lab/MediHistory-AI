# Acceptance Criteria — MediHistory AI

**Document version:** 1.0  
**Status:** Draft

Each entry specifies: ID, linked functional requirement(s), feature ID, screen ID, description, test type, pass condition, and fail condition. Criteria cover positive cases, negative cases, edge cases, and security cases.

Test types: **Unit** | **Integration** | **UI** | **Security** | **Manual**

---

## F01 — Patient Dashboard

---

**AC-001**  
FRs: FR-001 | Feature: F01 | Screen: SCR-01  
Description: Dashboard summary cards display accurate counts.  
Test type: Integration  
Pass: Each summary card (total prescriptions, distinct doctors, total medicines, unreviewed duplicate alerts, pending OCR review) shows the correct count matching the database state for the authenticated patient.  
Fail: Any card shows an incorrect count, a count belonging to a different patient, or a stale value after a new prescription is added.

---

**AC-002**  
FRs: FR-001 | Feature: F01 | Screen: SCR-01  
Description: Dashboard counts update after adding a new prescription.  
Test type: UI  
Pass: After adding a new prescription, the total prescriptions count on the dashboard increments by one without requiring a manual refresh.  
Fail: Count does not update, updates only after app restart, or shows a count from another patient's session.

---

**AC-003**  
FRs: FR-002 | Feature: F01 | Screen: SCR-01  
Description: Recent records list shows up to five most recent prescriptions.  
Test type: UI  
Pass: The list shows the five most recently added or updated prescriptions in reverse chronological order, each with doctor name, specialty, and date. Tapping any item navigates to the prescription detail screen.  
Fail: List is empty when prescriptions exist, shows more or fewer than five items, shows items in wrong order, or does not navigate on tap.

---

**AC-004**  
FRs: FR-003 | Feature: F01 | Screen: SCR-01  
Description: Bottom navigation bar is accessible and navigates correctly.  
Test type: UI  
Pass: The bottom navigation bar shows four destinations (Prescriptions, Medicine Explorer, Duplicate Alerts, Medical History) with visible labels. Tapping each navigates to the correct screen. Active destination is visually indicated.  
Fail: Any destination is missing, navigates to the wrong screen, or the active state indicator is absent.

---

**AC-005**  
FRs: FR-004 | Feature: F01 | Screen: SCR-01  
Description: Empty state is shown when no prescriptions exist.  
Test type: UI  
Pass: A new account with no prescriptions sees a plain-language empty state with a call-to-action button. No zero-value summary cards are shown.  
Fail: Empty state is absent, shows zero-value cards, or shows data belonging to another patient.

---

## F02 — Multi-Doctor Prescription Management

---

**AC-006**  
FRs: FR-005, FR-006 | Feature: F02 | Screen: SCR-03  
Description: Original document is preserved unchanged after upload or capture.  
Test type: Integration  
Pass: A SHA-256 hash of the stored source document matches the hash of the original file at the time of upload. The stored file is in app-private storage and is not accessible by other apps.  
Fail: Hash mismatch (file was modified), file is stored in shared storage, or file is missing.

---

**AC-007**  
FRs: FR-005 | Feature: F02 | Screen: SCR-03  
Description: Camera capture provides a readability confirmation step.  
Test type: UI  
Pass: After capturing an image, the patient sees a preview with a "Use this image" and "Retake" option before the document is stored. The image cannot be submitted without the patient explicitly accepting it.  
Fail: Image is stored automatically without confirmation, or the retake option is absent.

---

**AC-008**  
FRs: FR-006 | Feature: F02 | Screen: SCR-03  
Description: File picker accepts JPEG, PNG, and PDF; rejects unsupported formats.  
Test type: Integration  
Pass: Files with .jpg, .jpeg, .png, .pdf extensions are accepted and stored. Files with other extensions (e.g. .docx, .xls) are rejected with a plain-language error message.  
Fail: Unsupported file types are accepted, supported types are rejected, or no error is shown for unsupported types.

---

**AC-009**  
FRs: FR-007 | Feature: F02 | Screen: SCR-05  
Description: Doctor profile can be created, edited, and deleted.  
Test type: Integration  
Pass: A doctor profile can be created with all required fields (name, specialty, clinic). An existing profile can be edited and changes persist. Deleting a profile shows a confirmation dialog. After deletion, the profile is removed; prescriptions linked to the deleted doctor retain their data but show "Doctor removed" as the doctor reference.  
Fail: Profile creation fails, edits do not persist, deletion removes linked prescriptions, or no confirmation is shown before deletion.

---

**AC-010**  
FRs: FR-007 | Feature: F02 | Screen: SCR-05  
Description: Specialty list includes all required specialties plus Other.  
Test type: UI  
Pass: The specialty selector includes: Orthopedics, Cardiology, General Medicine, Neurology, Dermatology, Psychiatry, and Other. The list is scrollable if more specialties are added. Selecting Other allows free text entry.  
Fail: Required specialties are absent, the list is not scrollable with many entries, or Other does not allow text entry.

---

**AC-011**  
FRs: FR-008 | Feature: F02 | Screen: SCR-03  
Description: Prescription metadata is required before processing.  
Test type: UI  
Pass: The prescription cannot be saved without a prescription date and a linked doctor. Both fields show validation errors when empty. Pre-filled values from OCR are accepted but may be edited before saving.  
Fail: Prescription saves without a date or doctor, validation errors are not shown, or OCR-pre-filled values cannot be edited.

---

**AC-012**  
FRs: FR-009 | Feature: F02 | Screen: SCR-02  
Description: Prescription list shows extraction status badge for each prescription.  
Test type: UI  
Pass: Each prescription in the list shows one of: Pending (OCR not started), Extracting (in progress), Needs Review (extraction complete, unreviewed fields), Reviewed (all fields reviewed), or Error (OCR failed). Badge is visually distinct and accessible (not colour-only).  
Fail: Badge is absent, uses only colour to convey status, shows incorrect status, or is not updated when status changes.

---

**AC-013**  
FRs: FR-009 | Feature: F02 | Screen: SCR-04  
Description: Prescription detail shows source document and extracted medicines side by side or in tabs.  
Test type: UI  
Pass: The detail screen shows the original document (image or PDF) viewable at full resolution, and the extracted medicines list. Both are accessible from the same screen (tabs or scroll sections). Medicine entries link to the medicine detail screen.  
Fail: Source document is not viewable, extracted medicines are on a separate screen with no link from the detail, or document is displayed at unusable resolution.

---

**AC-014**  
FRs: FR-010 | Feature: F02 | Screen: SCR-04  
Description: Prescription deletion removes source document and extracted data.  
Test type: Integration  
Pass: After confirming deletion, the prescription record, its source document file, all linked PrescriptionMedicine records, and associated duplicate alerts are deleted from local storage. The action cannot be undone after confirmation. The deletion is reflected in all summary counts.  
Fail: Source document remains after deletion, PrescriptionMedicine records persist, duplicate alerts are not recalculated, or the patient can delete without confirmation.

---

## F03 — OCR and Prescription Review

---

**AC-015**  
FRs: FR-011 | Feature: F03 | Screen: SCR-06  
Description: OCR runs as a background job without blocking the UI.  
Test type: Integration  
Pass: After adding a prescription, the app remains responsive. The prescription shows a processing indicator. The patient can navigate to other screens while OCR runs. Results appear when processing completes (notification or status update on next view).  
Fail: UI freezes or blocks during OCR, or the patient cannot navigate away from the adding screen.

---

**AC-016**  
FRs: FR-011 | Feature: F03 | Screen: SCR-06  
Description: OCR can be manually triggered from the prescription detail screen.  
Test type: UI  
Pass: A "Run OCR" or "Re-extract" button is visible on the prescription detail screen when OCR has not been run or previously failed. Tapping it queues a new OCR job and updates the status indicator.  
Fail: No manual trigger is available, tapping has no effect, or the status indicator does not update.

---

**AC-017**  
FRs: FR-012 | Feature: F03 | Screen: SCR-07  
Description: Extraction populates only fields present in the source document.  
Test type: Unit  
Pass: For a synthetic prescription that contains only medicine name, strength, and dosage frequency, the extracted record contains only those fields. Fields for active ingredient, route, duration, and diagnosis are empty (not inferred).  
Fail: Any field is populated with a value not present in the source document, including inferred or default values.

---

**AC-018**  
FRs: FR-012 | Feature: F03 | Screen: SCR-07  
Description: Extraction result is rejected if structured output fails validation.  
Test type: Unit  
Pass: If the AI extraction service returns an output that fails schema validation (missing required field, wrong type, out-of-range confidence score), the extraction is rejected and the prescription is set to ERROR status. No partial extraction is stored.  
Fail: Invalid extraction output is stored, the prescription is shown as Extracted with invalid data, or partial results are used without flagging.

---

**AC-019**  
FRs: FR-013 | Feature: F03 | Screen: SCR-07  
Description: Confidence score is stored for every extracted field.  
Test type: Unit  
Pass: Every PrescriptionMedicine record stores a confidence score (0.0–1.0) for each extracted field. Fields with confidence below the configured threshold (default: 0.7) are flagged in the review interface.  
Fail: Any field lacks a confidence score, confidence is not persisted, or low-confidence fields are not flagged.

---

**AC-020**  
FRs: FR-013 | Feature: F03 | Screen: SCR-07  
Description: Review state transitions follow the defined state machine.  
Test type: Unit  
Pass: A new extracted record starts in PENDING_REVIEW. Confirming the value transitions to REVIEWED. Rejecting transitions to REJECTED. A REVIEWED record can be returned to PENDING_REVIEW by the patient. No other transitions are permitted.  
Fail: Invalid state transitions occur, a record skips PENDING_REVIEW, or the state machine allows disallowed transitions.

---

**AC-021**  
FRs: FR-014 | Feature: F03 | Screen: SCR-07  
Description: Patient edit is stored alongside original OCR output, not replacing it.  
Test type: Integration  
Pass: When a patient edits an extracted field, the original OCR value, the patient's edited value, the confidence score, and the review state are all stored in the database. Querying the record returns both values.  
Fail: The OCR value is overwritten and lost, only the edited value is stored, or the audit trail is incomplete.

---

**AC-022**  
FRs: FR-014 | Feature: F03 | Screen: SCR-07  
Description: Review interface is accessible and usable on a 360dp wide screen.  
Test type: UI  
Pass: All review form fields, confidence indicators, confirm/edit/reject actions are fully visible and operable on a device with a 360dp wide screen. No content is clipped or requires horizontal scrolling.  
Fail: Content is clipped, horizontally scrollable, or action buttons are not reachable without scrolling.

---

**AC-023**  
FRs: FR-015 | Feature: F03 | Screen: SCR-06  
Description: OCR failure shows plain-language error and retry option.  
Test type: Integration  
Pass: When OCR fails, the prescription is marked ERROR. The patient sees a message explaining that automatic reading failed, with a "Try again" button and a "Enter manually" option. The original document is still accessible.  
Fail: The error is shown as a technical code, no retry is offered, no manual entry path is offered, or the document is inaccessible after failure.

---

**AC-024**  
FRs: FR-016 | Feature: F03 | Screen: SCR-08  
Description: Manual medicine entries are tagged as MANUAL source and follow review-state model.  
Test type: Unit  
Pass: A manually entered PrescriptionMedicine record has source = MANUAL and starts in REVIEWED state (since the patient entered it directly). Manual entries appear in all downstream processing (medicine explorer, duplicate detection) alongside extracted entries.  
Fail: Manual entries lack source tag, start in PENDING_REVIEW without reason, or are excluded from downstream processing.

---

## F04 — Consolidated Medicine Explorer

---

**AC-025**  
FRs: FR-017 | Feature: F04 | Screen: SCR-09  
Description: Medicine explorer lists all medicines from all prescriptions.  
Test type: Integration  
Pass: The explorer shows one entry per unique normalised active ingredient (or per medicine name when active ingredient is unknown). Multiple prescription occurrences of the same medicine are grouped. The count matches the database. Lazy loading is applied when the list exceeds 20 items.  
Fail: Medicines from any prescription are missing, duplicates are not grouped, or the screen freezes with 50+ medicines.

---

**AC-026**  
FRs: FR-017 | Feature: F04 | Screen: SCR-09  
Description: Each medicine entry shows required fields.  
Test type: UI  
Pass: Each list item shows: medicine name, active ingredient (or "Unknown" if not identified), strength, most recent prescribing doctor, and most recent prescription date. Fields absent from the source document are shown as "Not recorded" rather than empty.  
Fail: Required fields are absent, blank/null values are shown without a label, or the layout breaks with long medicine names.

---

**AC-027**  
FRs: FR-018 | Feature: F04 | Screen: SCR-09  
Description: Search and filter return correct results.  
Test type: Integration  
Pass: Searching "metformin" returns all medicines with "metformin" in the medicine name or active ingredient field. Filtering by doctor returns only medicines from that doctor's prescriptions. Filtering by date range returns only medicines from prescriptions within that range. Active filters are shown as dismissible chips.  
Fail: Search returns incorrect results, filter has no effect, chips are absent, or dismissing a chip does not update the list.

---

**AC-028**  
FRs: FR-019 | Feature: F04 | Screen: SCR-10  
Description: Medicine detail screen shows all prescription occurrences.  
Test type: Integration  
Pass: The detail screen for a medicine lists every prescription occurrence of that active ingredient. Each occurrence shows: brand name (if different from active ingredient), strength, formulation, dosage instructions, prescribing doctor, prescription date, and a tap target linking to the source prescription.  
Fail: Any occurrence is missing, the source prescription link is absent, or the screen shows data from a different patient.

---

**AC-029**  
FRs: FR-019 | Feature: F04 | Screen: SCR-10  
Description: Medicine thumbnail or icon is shown when available.  
Test type: UI  
Pass: A medicine thumbnail or system icon is displayed for each medicine entry. When no image is available, a generic medicine icon placeholder is shown. No broken image states are visible.  
Fail: Broken image states appear, the layout breaks when the image is absent, or no placeholder is shown.

---

**AC-030**  
FRs: FR-020 | Feature: F04 | Screen: SCR-09  
Description: Medicines are grouped by Current / Past / Unknown status.  
Test type: UI  
Pass: The explorer shows three sections: Current, Past, and Unknown. Each medicine appears in exactly one section. Section membership is determined by the patient-assigned status, not inferred by the system. The sections are collapsible. Section headers show the item count.  
Fail: A medicine appears in more than one section, sections are absent, or status is inferred without patient input.

---

**AC-031**  
FRs: FR-021 | Feature: F04 | Screen: SCR-09, SCR-10  
Description: Every medicine entry links to its source prescription.  
Test type: Integration  
Pass: Tapping the source link on any medicine list item or detail screen navigates to the correct prescription detail screen, showing the original document and the specific medicine's extracted data.  
Fail: The source link is absent, navigates to a wrong prescription, or opens a blank screen.

---

## F05 — Potential Duplicate Ingredient Detection

---

**AC-032**  
FRs: FR-022 | Feature: F05 | Screen: SCR-11  
Description: Duplicate detection runs after review state changes and new medicines are added.  
Test type: Integration  
Pass: When a PrescriptionMedicine record transitions to REVIEWED, the duplicate detection engine runs within one background job cycle. New alerts are created for newly detected matches. Existing alerts are updated if a reviewed record is rejected (alert is voided).  
Fail: Detection does not run after review state changes, alerts are stale, or detection runs on unreviewed records.

---

**AC-033**  
FRs: FR-022 | Feature: F05 | Screen: SCR-11  
Description: Duplicate matching uses normalised active ingredient, not brand name.  
Test type: Unit  
Pass: A synthetic test with two prescriptions containing the same active ingredient under different brand names produces one duplicate alert. A synthetic test with two prescriptions containing different active ingredients with similar brand names produces no alert.  
Fail: Brand-name matching produces false positives or misses active ingredient matches.

---

**AC-034**  
FRs: FR-023 | Feature: F05 | Screen: SCR-12  
Description: Duplicate alert contains all required fields.  
Test type: Integration  
Pass: Each alert detail screen shows: matched active ingredient(s), both medicine names and brands, both strengths, both formulations, both routes, both prescribed dosages, both doctors, both prescription dates, and links to both source prescriptions. The label "Potential duplicate — please review with your doctor or pharmacist" is displayed prominently.  
Fail: Any required field is missing, the label is absent, or source prescription links are broken.

---

**AC-035**  
FRs: FR-024 | Feature: F05 | Screen: SCR-12  
Description: Marking an alert as reviewed records a timestamp and does not delete the alert.  
Test type: Integration  
Pass: After tapping "Mark as reviewed," the alert's reviewed_at timestamp is set and the alert status changes to Reviewed. The alert remains in the alert list with a "Reviewed" badge. The reviewed timestamp is shown in the alert detail.  
Fail: Alert is deleted after review, timestamp is not stored, or the badge does not update.

---

**AC-036**  
FRs: FR-024 | Feature: F05 | Screen: SCR-12  
Description: Reviewed alert shows disclaimer that reviewed status is not medical clearance.  
Test type: UI  
Pass: The alert detail screen shows, adjacent to the "Mark as reviewed" button: "Marking as reviewed means you have seen this alert. It does not mean this combination has been approved by a doctor." The text is visible without scrolling.  
Fail: Disclaimer is absent, hidden below the fold, or uses language that implies clinical clearance.

---

**AC-037**  
FRs: FR-025 | Feature: F05 | Screen: SCR-12  
Description: Combination medicines are handled correctly in duplicate detection.  
Test type: Unit  
Pass: A synthetic test with a combination product (A+B) and a single-ingredient product (A) produces a reduced-confidence alert noting the partial match. The alert content specifies which ingredient matched and that the other prescription contains a combination product.  
Fail: No alert is generated for a partial active ingredient match, or the alert incorrectly claims a full match.

---

**AC-038**  
FRs: FR-026 | Feature: F05 | Screen: SCR-11  
Description: Duplicate alert list sorts unreviewed alerts first.  
Test type: UI  
Pass: The alert list shows unreviewed alerts above reviewed alerts. Within each group, alerts are sorted by most recent prescription date. Reviewed alerts show a "Reviewed" badge. The list header shows the total count and unreviewed count separately.  
Fail: Unreviewed and reviewed alerts are interleaved without grouping, or counts are absent.

---

## F06 — AI Medical History Summarizer

---

**AC-039**  
FRs: FR-027 | Feature: F06 | Screen: SCR-15  
Description: History generation uses only REVIEWED records.  
Test type: Integration  
Pass: A generated summary includes only data from PrescriptionMedicine records in REVIEWED state. Prescriptions with all medicines in PENDING_REVIEW are excluded. The summary generation API returns a list of excluded records with the reason "Not reviewed."  
Fail: PENDING_REVIEW or REJECTED records appear in the generated summary, or the exclusion list is absent.

---

**AC-040**  
FRs: FR-027 | Feature: F06 | Screen: SCR-15  
Description: Every statement in the summary links to a source document.  
Test type: Integration  
Pass: Each section of the generated summary that references a medicine, condition, or prescription contains a source reference (DocID or equivalent). Tapping a reference navigates to the correct prescription detail screen.  
Fail: Any statement lacks a source reference, or a reference navigates to a wrong or missing document.

---

**AC-041**  
FRs: FR-028 | Feature: F06 | Screen: SCR-14  
Description: Summary sections are labelled by information type.  
Test type: UI  
Pass: Each section of the summary is labelled with one of: Documented (from source documents), Patient-reported (entered by the patient), Uncertain (low-confidence extraction), or AI-generated (narrative produced by the AI). Labels are visually distinct and accessible.  
Fail: Any section lacks a label, labels are colour-only, or an AI-generated section is unlabelled.

---

**AC-042**  
FRs: FR-029 | Feature: F06 | Screen: SCR-14  
Description: Summarizer does not invent diagnoses or allergies.  
Test type: Unit  
Pass: Given synthetic prescription data with no diagnoses or allergies documented, the generated summary does not contain any diagnosis or allergy statements. An adversarial prompt that attempts to inject a diagnosis into the AI summariser is rejected or sanitised.  
Fail: The summary contains a diagnosis or allergy not present in the source data, or the adversarial prompt produces an invented clinical claim.

---

**AC-043**  
FRs: FR-029 | Feature: F06 | Screen: SCR-14  
Description: Absent fields are omitted or marked "not available," never inferred.  
Test type: Unit  
Pass: For a synthetic prescription with no documented conditions, the conditions section reads "No conditions documented" or is omitted entirely. The section does not contain inferred or assumed conditions.  
Fail: An absent field produces an inferred or default value in the summary.

---

**AC-044**  
FRs: FR-030 | Feature: F06 | Screen: SCR-14  
Description: Inline source reference indicator is tappable.  
Test type: UI  
Pass: Each inline reference indicator in the summary detail screen is a tappable element with a minimum touch target of 48×48 dp. Tapping navigates to the correct source prescription.  
Fail: Reference indicators are not tappable, touch target is smaller than 48 dp, or navigation leads to the wrong prescription.

---

**AC-045**  
FRs: FR-031 | Feature: F06 | Screen: SCR-13  
Description: Previous summary versions are retained and accessible.  
Test type: Integration  
Pass: After generating a second summary, the history list shows both versions with distinct timestamps. Tapping either version opens its content. The newer summary does not overwrite the older one in the database.  
Fail: Previous summaries are deleted when a new one is generated, or versions are not listed with timestamps.

---

## F07 — New Doctor Mode

---

**AC-046**  
FRs: FR-032 | Feature: F07 | Screen: SCR-16  
Description: New Doctor Mode flow requires explicit step-by-step confirmation.  
Test type: UI  
Pass: The flow has distinct steps: Review Summary → Select Records → Confirm Selection → Generate PDF → Share. The patient cannot skip directly to Share. Each step shows a Back option. The flow can be exited at any point without sharing.  
Fail: A step can be skipped, the Back option is absent, or exiting the flow triggers a share action.

---

**AC-047**  
FRs: FR-032 | Feature: F07 | Screen: SCR-16  
Description: Patient can deselect individual prescriptions and medicines from the summary.  
Test type: UI  
Pass: The Select Records step shows all prescriptions and medicines with checkboxes (or equivalent). Each item is individually selectable. Deselecting an item removes it from the generated PDF. The Generate PDF button is disabled if no items are selected.  
Fail: Items cannot be individually deselected, deselection has no effect on the PDF, or Generate PDF is enabled with no items selected.

---

**AC-048**  
FRs: FR-033 | Feature: F07 | Screen: SCR-17  
Description: Generated PDF includes required content and disclaimer.  
Test type: Integration  
Pass: The generated PDF contains: patient name, the selected summary sections with source references, the selected medicine list, any selected prescription images as appendices, and the disclaimer "This document is a patient-compiled record and is not a clinical report." The PDF is generated on-device without transmitting content to an external service unless explicitly configured.  
Fail: Disclaimer is absent, required sections are missing, or PDF generation requires network access when offline.

---

**AC-049**  
FRs: FR-034 | Feature: F07 | Screen: SCR-18  
Description: Share action requires explicit consent confirmation.  
Test type: UI  
Pass: Before the Android share sheet opens, the patient sees a preview of the shared content, the consent statement, and the name of the sharing destination (resolved from the selected app). A distinct "Share now" button must be tapped. Dismissing the confirmation sheet cancels the share.  
Fail: The share sheet opens without the consent screen, the preview is absent, the consent statement is absent, or dismissal does not cancel the share.

---

## F08 — Medication Reminders and Adherence

---

**AC-050**  
FRs: FR-035 | Feature: F08 | Screen: SCR-20  
Description: Reminders show the prescription date they are based on.  
Test type: UI  
Pass: Each active reminder shows the medicine name, dosage frequency from the prescription, and the prescription date it is based on. The label reads "Based on prescription dated [date]." The patient can edit or disable the reminder without changing the prescription record.  
Fail: Reminder does not show the prescription date, uses language that implies the reminder is a current clinical instruction, or editing the reminder modifies the prescription record.

---

**AC-051**  
FRs: FR-036 | Feature: F08 | Screen: SCR-21  
Description: Dose log entries are labelled as patient-reported.  
Test type: Unit  
Pass: Each dose log entry has a patient_reported flag set to true. The dose log view shows "Patient-reported" label on each entry. Dose log data is not included in the medical history summary without explicit labelling as patient-reported.  
Fail: Dose log entries lack the patient_reported flag, the label is absent in the UI, or dose log data appears in the history summary as documented fact.

---

## F09 — Medication Interaction Checks

---

**AC-052**  
FRs: FR-037 | Feature: F09 | Screen: SCR-22  
Description: Interaction check is available only when reference source is configured.  
Test type: Integration  
Pass: With MED_REFERENCE_PROVIDER set to a valid provider, the interaction check feature is accessible and returns results for a synthetic medicine list. With MED_REFERENCE_PROVIDER empty or unset, the feature screen shows the unavailable state (AC-053).  
Fail: The interaction check runs without a configured provider, or the feature is accessible but returns no results without an error.

---

**AC-053**  
FRs: FR-038 | Feature: F09 | Screen: SCR-22  
Description: Unavailable state does not imply the combination is safe.  
Test type: UI  
Pass: When the reference source is unavailable, the screen shows exactly: "Interaction checking is not available. Please discuss your medicines with your doctor or pharmacist." No partial results, no "no interactions found" message, and no reassuring language.  
Fail: The unavailable state shows "no interactions found," shows partial results, or uses language that implies the combination is safe.

---

## F10 — Privacy and Security

---

**AC-054**  
FRs: FR-039 | Feature: F10 | Screen: SCR-25  
Description: Data export produces a complete, accessible package.  
Test type: Integration  
Pass: Triggering an export produces a downloadable package (encrypted ZIP or JSON) containing all prescriptions (images and metadata), extracted medicine data, history summaries, and audit log metadata. The patient can open the exported package on a separate device using a provided key.  
Fail: Export is missing any category of data, the package is not readable by a decryption tool, or export fails silently.

---

**AC-055**  
FRs: FR-010, FR-039 | Feature: F10 | Screen: SCR-25  
Description: Full account deletion removes all data from local and backend storage.  
Test type: Integration  
Pass: After confirming deletion, all local Room records, source document files, WorkManager jobs, and notifications are deleted. If sync is enabled, a deletion request is sent to the backend and acknowledged. The patient sees a confirmation that deletion is complete or pending (for backend propagation). After deletion, the app returns to the onboarding screen.  
Fail: Any local record, file, or notification persists after deletion, the backend is not notified when sync is enabled, or the patient is not shown a completion confirmation.

---

**AC-056**  
FRs: FR-034 | Feature: F07 | Screen: SCR-18  
Description: Sharing audit log records action metadata only — no document content.  
Test type: Security  
Pass: The audit log entry for a share action contains: action = "SHARE_PDF", entity_id (document ID), entity_type = "MedicalHistorySummary", timestamp, and destination app name. It does not contain: any prescription text, medicine names, or summary content.  
Fail: The audit log contains any patient health information, prescription content, or medicine details.

---

**AC-057**  
FRs: FR-040 | Feature: F10 | Screen: SCR-26  
Description: Cloud backup is disabled by default and requires explicit opt-in.  
Test type: Integration  
Pass: On a fresh installation, CLOUD_BACKUP_ENABLED is false and no data is uploaded. Enabling backup requires the patient to navigate to Settings, tap "Enable cloud backup," read an explanation, and confirm. Backup status is visible at all times on the settings screen.  
Fail: Backup is enabled by default, data is uploaded before opt-in, the status is not visible, or no explanation is shown before enabling.

---

**AC-058**  
FRs: FR-040 | Feature: F10 | Screen: SCR-26  
Description: Disabling backup explains retention limitations.  
Test type: UI  
Pass: When the patient disables cloud backup, the app shows: "Disabling backup stops new uploads. Existing cloud copies may remain for up to [N] days. Contact support to request immediate deletion." The patient must acknowledge this before the setting is applied.  
Fail: Disabling backup happens without explanation, the retention message is absent, or the acknowledgment step is skipped.

---

**AC-059**  
FRs: FR-039 | Feature: F10 | Screen: SCR-24  
Description: No API keys or secrets are present in the APK.  
Test type: Security  
Pass: Static analysis of the release APK (using apktool or equivalent) finds no strings matching known API key patterns (e.g. 32+ character hex strings, JWTs, URIs containing credentials). The backend URL is configurable, not hardcoded.  
Fail: Any API key, JWT secret, database credential, or storage key is found in the APK binary or resources.

---

**AC-060**  
FRs: FR-039 | Feature: F10  
Description: Audit log never contains prescription text or medicine details.  
Test type: Security  
Pass: All audit log entries created during an end-to-end test (add prescription, run OCR, review, generate summary, share, delete) contain only: action, entity_id, entity_type, timestamp, actor. No prescription text, medicine name, dosage, or document content appears in any log entry.  
Fail: Any audit log entry contains patient health information.

---

*IDs AC-001 through AC-060 are stable once this document is approved. New criteria must use IDs AC-061 onwards.*
