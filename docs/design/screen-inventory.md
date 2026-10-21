# Screen Inventory — MediHistory AI

**Version:** 1.0  
**Last updated:** 2025  
**Total screens:** 13

---

## Status Key

| Status | Meaning |
|---|---|
| 🔵 In Progress | Being designed in Figma |
| 🟡 Review | Awaiting stakeholder sign-off |
| 🟢 Ready for Dev | Approved, all states complete, accessibility reviewed |
| 🔴 Blocked | Blocked by open decision or dependency |
| ⚪ Not Started | Design not yet begun |

---

## Screen Inventory Table

| Screen ID | Screen Name | Feature IDs | FR IDs | AC IDs | Figma Page | Stitch Prompt | Status |
|---|---|---|---|---|---|---|---|
| SCR-01-DASHBOARD | Patient Dashboard | F01 | FR-01-01, FR-01-02, FR-01-03, FR-01-04, FR-01-05 | AC-01-01, AC-01-02, AC-01-03, AC-01-04, AC-01-05 | `SCR-01 Dashboard` ([FIGMA_NODE_TBD]) | `01-dashboard-prompt.md` | ⚪ Not Started |
| SCR-02-RX-LIST | Prescription List | F02 | FR-02-01, FR-02-02, FR-02-03, FR-02-04 | AC-02-01, AC-02-02, AC-02-03, AC-02-04 | `SCR-02 Rx List` ([FIGMA_NODE_TBD]) | `02-prescriptions-prompt.md` | ⚪ Not Started |
| SCR-02-RX-DETAIL | Prescription Detail | F02, F03 | FR-02-05, FR-02-06, FR-03-01 | AC-02-05, AC-02-06, AC-03-01 | `SCR-02 Rx Detail` ([FIGMA_NODE_TBD]) | `02-prescriptions-prompt.md` | ⚪ Not Started |
| SCR-02-DOCTOR-GROUP | Doctor Group View | F02 | FR-02-07 | AC-02-07 | `SCR-02 Rx List` ([FIGMA_NODE_TBD]) | `02-prescriptions-prompt.md` | ⚪ Not Started |
| SCR-03-OCR-REVIEW | OCR Extraction Review | F03 | FR-03-02, FR-03-03, FR-03-04, FR-03-05, FR-03-06 | AC-03-02, AC-03-03, AC-03-04, AC-03-05, AC-03-06 | `SCR-03 OCR Review` ([FIGMA_NODE_TBD]) | `02-prescriptions-prompt.md` | ⚪ Not Started |
| SCR-04-MED-EXPLORER | Medicine Explorer | F04 | FR-04-01, FR-04-02, FR-04-03, FR-04-04 | AC-04-01, AC-04-02, AC-04-03, AC-04-04 | `SCR-04 Med Explorer` ([FIGMA_NODE_TBD]) | `03-medicine-explorer-prompt.md` | ⚪ Not Started |
| SCR-04-MED-DETAIL | Medicine Detail | F04, F05 | FR-04-05, FR-04-06, FR-05-01 | AC-04-05, AC-04-06, AC-05-01 | `SCR-04 Med Detail` ([FIGMA_NODE_TBD]) | `03-medicine-explorer-prompt.md` | ⚪ Not Started |
| SCR-05-DUPLICATE-REVIEW | Duplicate Alert Review | F05 | FR-05-02, FR-05-03, FR-05-04, FR-05-05, FR-05-06 | AC-05-02, AC-05-03, AC-05-04, AC-05-05, AC-05-06 | `SCR-05 Duplicate Review` ([FIGMA_NODE_TBD]) | `04-duplicate-checker-prompt.md` | ⚪ Not Started |
| SCR-06-HISTORY | Medical History Summary | F06 | FR-06-01, FR-06-02, FR-06-03, FR-06-04, FR-06-05 | AC-06-01, AC-06-02, AC-06-03, AC-06-04, AC-06-05 | `SCR-06 History` ([FIGMA_NODE_TBD]) | `05-medical-history-prompt.md` | ⚪ Not Started |
| SCR-07-NEW-DOCTOR | New Doctor Mode | F07 | FR-07-01, FR-07-02, FR-07-03, FR-07-04 | AC-07-01, AC-07-02, AC-07-03, AC-07-04 | `SCR-07 New Doctor` ([FIGMA_NODE_TBD]) | `06-new-doctor-mode-prompt.md` | ⚪ Not Started |
| SCR-07-PDF-PREVIEW | PDF Preview and Share | F07 | FR-07-05, FR-07-06 | AC-07-05, AC-07-06 | `SCR-07 PDF Preview` ([FIGMA_NODE_TBD]) | `06-new-doctor-mode-prompt.md` | ⚪ Not Started |
| SCR-08-REMINDERS | Medication Reminders | F08 | FR-08-01, FR-08-02, FR-08-03, FR-08-04 | AC-08-01, AC-08-02, AC-08-03, AC-08-04 | `SCR-08 Reminders` ([FIGMA_NODE_TBD]) | `07-reminders-prompt.md` | ⚪ Not Started |
| SCR-09-SAFETY-CENTER | Safety Center | F09, F10 | FR-09-01, FR-09-02, FR-09-03, FR-10-01 | AC-09-01, AC-09-02, AC-09-03, AC-10-01 | `SCR-09 Safety Center` ([FIGMA_NODE_TBD]) | `08-safety-center-prompt.md` | ⚪ Not Started |

---

## Required States Per Screen

Every screen must have a minimum of four Figma frames: Loading, Empty, Error, and Populated.

| Screen ID | Loading | Empty | Error | Populated | Additional states |
|---|---|---|---|---|---|
| SCR-01-DASHBOARD | ✓ req | ✓ req | ✓ req | ✓ req | Populated — Alert Active |
| SCR-02-RX-LIST | ✓ req | ✓ req | ✓ req | ✓ req | Populated — Doctor Group View |
| SCR-02-RX-DETAIL | ✓ req | ✓ req | ✓ req | ✓ req | — |
| SCR-02-DOCTOR-GROUP | ✓ req | — | ✓ req | ✓ req | — |
| SCR-03-OCR-REVIEW | ✓ req (OCR in progress) | ✓ req (no data) | ✓ req | ✓ req | Populated — Low Confidence |
| SCR-04-MED-EXPLORER | ✓ req | ✓ req | ✓ req | ✓ req | Populated — Search Active |
| SCR-04-MED-DETAIL | ✓ req | ✓ req | ✓ req | ✓ req | — |
| SCR-05-DUPLICATE-REVIEW | ✓ req | ✓ req | ✓ req | ✓ req | Populated — Detail Expanded |
| SCR-06-HISTORY | ✓ req | ✓ req | ✓ req | ✓ req | Populated — Generating |
| SCR-07-NEW-DOCTOR | ✓ req | ✓ req | ✓ req | ✓ req | Populated — Consent Dialog |
| SCR-07-PDF-PREVIEW | ✓ req | — | ✓ req | ✓ req | — |
| SCR-08-REMINDERS | ✓ req | ✓ req | ✓ req | ✓ req | — |
| SCR-09-SAFETY-CENTER | ✓ req | ✓ req | ✓ req | ✓ req | Source Unavailable |

---

## Screen Feature Summary

### SCR-01-DASHBOARD — Patient Dashboard
Shows total prescriptions, doctors, medicine count, and duplicate alert count. Displays recently added prescriptions and a duplicate alert banner when alerts are active. Primary navigation hub to all modules.

**Navigation out:**
- Tap recent prescription card → SCR-02-RX-DETAIL
- Tap duplicate alert banner → SCR-05-DUPLICATE-REVIEW
- Bottom nav → any top-level screen

---

### SCR-02-RX-LIST — Prescription List
Displays all prescriptions as cards. Supports flat list and doctor-grouped views. FAB to add new prescription. Swipe left on a card reveals delete action.

**Navigation out:**
- Tap prescription card → SCR-02-RX-DETAIL
- FAB → add prescription flow → SCR-03-OCR-REVIEW
- Toggle group view → SCR-02-DOCTOR-GROUP

---

### SCR-02-RX-DETAIL — Prescription Detail
Full prescription detail: document thumbnail, doctor information, date, extracted medicines, OCR status, source document link.

**Navigation out:**
- OCR status chip (Needs review / Partially extracted) → SCR-03-OCR-REVIEW
- Medicine item tap → SCR-04-MED-DETAIL
- Source document link → document viewer (full-screen image)

---

### SCR-02-DOCTOR-GROUP — Doctor Group View
Groups prescriptions under each prescribing doctor with expandable sections.

**Navigation out:**
- Prescription item tap → SCR-02-RX-DETAIL
- Add prescription FAB → add flow

---

### SCR-03-OCR-REVIEW — OCR Extraction Review
Shows original document image alongside extracted fields. Each field has a confidence badge. Patient can edit uncertain fields, mark fields as reviewed, and confirm the complete extraction.

**Key behavior:** Never invents missing information. Unextracted fields are explicitly labeled "Not extracted".

**Navigation out:**
- Back → SCR-02-RX-DETAIL or SCR-02-RX-LIST (via add flow)
- Confirm → SCR-02-RX-DETAIL

---

### SCR-04-MED-EXPLORER — Medicine Explorer
Consolidated list of all medicines from all prescriptions. Searchable, filterable (by status, doctor, date range). Lazy-loading for large lists. Medicine cards show duplicate alert icon when applicable.

**Navigation out:**
- Tap medicine card → SCR-04-MED-DETAIL

---

### SCR-04-MED-DETAIL — Medicine Detail
Full detail for one medicine: name, active ingredient, strength, formulation, all prescriptions that include this medicine, dosage instructions per prescription, links to source documents.

**Navigation out:**
- Source prescription link → SCR-02-RX-DETAIL
- Duplicate warning link → SCR-05-DUPLICATE-REVIEW

---

### SCR-05-DUPLICATE-REVIEW — Duplicate Alert Review
Lists all potential duplicate ingredient alerts. Each alert card shows both medicines, active ingredient, strengths, doctors, and dates. Patient can mark as reviewed (not medical clearance) or dismiss.

**Navigation out:**
- Tap medicine name in alert → SCR-04-MED-DETAIL
- Source link in alert → SCR-02-RX-DETAIL

---

### SCR-06-HISTORY — Medical History Summary
AI-generated chronological summary of the patient's medical history. Each statement has a source citation link. Clearly labels AI-generated content, uncertain information, and patient-reported data.

**Navigation out:**
- Source citation chip → SCR-02-RX-DETAIL
- "New Doctor Mode" button → SCR-07-NEW-DOCTOR

---

### SCR-07-NEW-DOCTOR — New Doctor Mode
Allows the patient to select records to include in a shareable summary. Shows a review step, consent toggle, and generate PDF button.

**Navigation out:**
- "Generate PDF" → SCR-07-PDF-PREVIEW
- Back → SCR-06-HISTORY

---

### SCR-07-PDF-PREVIEW — PDF Preview and Share
Shows a preview of the generated PDF. Consent dialog before sharing. Uses Android system share sheet.

**Navigation out:**
- "Share" → ConsentDialog → system share sheet
- Back → SCR-07-NEW-DOCTOR

---

### SCR-08-REMINDERS — Medication Reminders
List of medication reminders with toggle, dose log buttons, and snooze. Accessible from Safety tab and settings.

**Navigation out:**
- Tap reminder row (future: edit reminder) → reminder edit bottom sheet

---

### SCR-09-SAFETY-CENTER — Safety Center
Entry point for interaction checking (when a reference source is configured) and link to duplicate review. Shows clear "source unavailable" message when interaction checking cannot run.

**Navigation out:**
- Interaction result → SCR-04-MED-DETAIL
- Duplicate alert link → SCR-05-DUPLICATE-REVIEW
- Reminders link → SCR-08-REMINDERS

---

## Updating This Document

When a screen is added, renamed, or removed:
1. Update this table
2. Update `screen-to-feature-mapping.csv`
3. Update the corresponding Figma page
4. Update `figma-import-and-handoff.md` frame mapping table
5. Update `navigation-map.md` if the navigation hierarchy changes

---

## Related Documents

- [`screen-to-feature-mapping.csv`](screen-to-feature-mapping.csv)
- [`navigation-map.md`](navigation-map.md)
- [`figma/figma-page-structure.md`](figma/figma-page-structure.md)
- [`figma/figma-import-and-handoff.md`](figma/figma-import-and-handoff.md)
