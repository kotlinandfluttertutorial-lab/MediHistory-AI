# Requirements — Medication Safety

**Spec version:** 1.0
**Status:** Draft
**Feature(s):** F05, F08, F09
**Jira tickets:** MHA-041, MHA-042, MHA-043, MHA-044, MHA-045, MHA-046, MHA-047, MHA-059, MHA-060, MHA-061, MHA-062, MHA-063, MHA-064, MHA-065, MHA-066, MHA-067
**Functional requirements:** FR-022, FR-023, FR-024, FR-025, FR-026, FR-035, FR-036, FR-037, FR-038
**Dependencies:** medicine-intelligence spec (NormalizedIngredient, MedicineEntity), prescription-management spec (reviewed PrescriptionMedicine records)

---

## 1. Purpose

This spec covers all features that help patients notice potential medication concerns and manage their adherence:

- **Duplicate detection algorithm** — deterministic, active-ingredient-based, separate from AI summarisation
- **Duplicate alert entity** — stores alerts with full context for patient review
- **Alert list and detail screens** — grouped by ingredient, unreviewed first
- **Mark-as-reviewed** — records timestamp, does not remove alert, includes mandatory disclaimer
- **Combination product handling** — decompose combination medicines into constituents; partial matches reported separately
- **Alerts backend API** — read and update endpoints with patient-scoped authorisation
- **Reminders** — optional, locally stored, based on prescription dosage frequency
- **Dose logging** — patient-reported Taken / Skipped / Missed with timestamp
- **Adherence tracking** — summary view of logged doses per medicine
- **Interaction checking service** — available only when a licensed reference is configured via `MED_REFERENCE_PROVIDER`
- **Interaction check UI** — shows results with a recommendation to consult a doctor; unavailable state is explicit

**Critical safety rules — these are non-negotiable:**
1. The app must never indicate that a medicine combination is safe.
2. Every duplicate alert and interaction check result must include a recommendation to discuss with a doctor or pharmacist.
3. Duplicate alerts are informational only — they do not block the patient from recording medicines or generating a summary.
4. Marking an alert as reviewed explicitly is not clinical clearance; this is stated in the UI on every mark-as-reviewed action.

---

## 2. Functional requirements

### FR-022 — Duplicate detection engine
Compares active ingredients across all `REVIEWED` `PrescriptionMedicine` records using normalised `NormalizedIngredient` IDs. Matching is deterministic — same input always produces the same output. Combination medicines are decomposed to constituent ingredients before comparison. Detection runs when new medicines are added or review state changes.

### FR-023 — Duplicate alert content
Each `DuplicateAlert` shows: matched active ingredient(s), both medicine names with brands, strengths, formulations, routes, prescribed dosages, prescribing doctors, prescription dates, links to both source prescriptions, and the mandatory label: "Potential duplicate — please review with your doctor or pharmacist."

### FR-024 — Alert review action
The patient can mark an alert as Reviewed. The timestamp is recorded. The alert remains visible and is visually distinguished (reviewed vs. unreviewed). Marking as reviewed is not medical clearance; the UI states this explicitly on every mark-as-reviewed action.

### FR-025 — Combination product and formulation handling
The detection engine accounts for: combination products (decomposed to constituents), different dosage forms, different release formulations (immediate vs. extended release), and different routes of administration. These variations reduce match confidence and are reported in the alert detail with a note naming both medicines and the matching ingredient.

### FR-026 — Duplicate alert list
Dedicated screen listing all alerts grouped by active ingredient. Each alert shows: both medicine names and an unreviewed/reviewed badge. Alerts are sorted with unreviewed first.

### FR-035 — Optional medication reminders
The patient can set optional reminders for any reviewed medicine. Reminders use the prescription's dosage frequency field as a default schedule; the patient can override it. Reminders are stored locally using Android notification channels. Every reminder notification is labelled with the prescription date and the prescribing doctor — not presented as a current clinical instruction.

### FR-036 — Dose logging
The patient can log individual doses as Taken, Skipped, or Missed. The log stores the patient's reported action and timestamp. The adherence view shows a summary per medicine. Logged doses are explicitly distinguished from prescribed instructions.

### FR-037 — Interaction check with configured reference
Available only when `MED_REFERENCE_PROVIDER` is configured. Compares the patient's current medicine list against the reference. Results show the medicines involved, the nature of the reported interaction, and a recommendation to discuss with a doctor or pharmacist.

### FR-038 — Interaction check unavailable state
When the reference source is not configured or unavailable, the feature shows: "Interaction checking is not available. Please discuss your medicines with your doctor or pharmacist." It does not imply the combination is safe and does not show partial results.

---

## 3. Tickets and acceptance criteria

### MHA-041 — DuplicateAlert entity and Room DAO
Room entity for `DuplicateAlert` with fields: id, patientId, normalizedIngredientId, medicine1Id, medicine2Id, matchConfidence, combinationNote (nullable), reviewState (`UNREVIEWED` / `REVIEWED`), reviewedAt (nullable), createdAt.

Key ACs:
- Schema export JSON is updated and committed.
- DAO tests cover: insert, queryByPatient (unreviewed first), queryByIngredient, updateReviewState.
- `combinationNote` is stored when a partial combination match is detected.

### MHA-042 — Duplicate detection algorithm (UseCase)
`DetectDuplicatesUseCase` takes a `patientId`, loads all `REVIEWED` medicines for that patient, normalises ingredient IDs, decomposes combination products using the synonym/ingredient map, and produces a list of `DuplicateAlert` candidates. Existing alerts are updated; resolved alerts are marked stale.

Key ACs:
- Determinism test: identical input produces identical output (run twice, compare).
- Test cases required (all using synthetic data):
  - Same ingredient, different brands → alert created.
  - Different ingredients, similar names → no alert.
  - Combination product vs. single ingredient (partial match) → reduced-confidence alert with combination note.
  - Combination product vs. same combination (full match) → full-confidence alert.
  - Paracetamol / acetaminophen synonym → alert created (treated as same ingredient).
  - Unreviewed records excluded → no alert for unreviewed medicine.
- 100% branch coverage required for this UseCase.

### MHA-043 — Duplicate alert list screen
`DuplicateAlertListScreen` with `LazyColumn`. Alerts grouped by active ingredient using `SectionHeader`. Unreviewed alerts appear before reviewed alerts within each group. Each item shows both medicine names and a `StatusBadge` (Unreviewed / Reviewed).

Key ACs:
- Empty state shows: "No potential duplicates found. This does not mean your medicines are safe to combine — always discuss with your doctor."
- List updates reactively when the detection engine runs.
- All items have touch targets ≥ 48×48 dp.
- Screen passes `AccessibilityChecks`.

### MHA-044 — Duplicate alert detail screen
`DuplicateAlertDetailScreen` shows all fields from FR-023. The mandatory label "Potential duplicate — please review with your doctor or pharmacist" is displayed at the top of the screen in a visually prominent style. Combination note (if present) is shown in a distinct section.

Key ACs:
- Mandatory label is always visible — it must not be hidden behind a scroll or collapsed section.
- Source prescription links navigate to the correct prescription detail screen.
- Screen passes `AccessibilityChecks`.
- Mark as Reviewed button is present.

### MHA-045 — Mark-as-reviewed action
The "Mark as Reviewed" button opens a `ConfirmationDialog` that includes the text: "Marking as reviewed means you have seen this alert. It does not mean this combination has been approved by a doctor." On confirmation, `reviewState` is updated and `reviewedAt` is set.

Key ACs:
- Confirmation dialog text is hardcoded and not user-editable.
- After marking as reviewed, the alert remains visible on the detail screen with a "Reviewed" badge.
- The action is reversible: the patient can un-review (sets `reviewState = UNREVIEWED`, clears `reviewedAt`).
- ViewModel test covers: mark-as-reviewed transition and un-review transition.

### MHA-046 — Combination product handling in detection engine
`DecomposeCombinationUseCase` takes a medicine name and returns its constituent `NormalizedIngredient` IDs using the bundled ingredient map. Single-ingredient medicines return a list of one. Unknown medicines return an empty list (not an error).

Key ACs:
- Unit test: combination product "SynthaCo-A+B" decomposed into two ingredient IDs.
- Unit test: unknown medicine name returns empty list without exception.
- Unit test: partial match (one constituent matches a separate medicine) → alert has `matchConfidence < 1.0` and `combinationNote` is set.

### MHA-047 — Alerts backend API
FastAPI endpoints: `GET /alerts` (list, paginated), `GET /alerts/{id}` (detail), `PATCH /alerts/{id}` (reviewState update only). All endpoints require authentication and patient-scoped authorisation.

Key ACs:
- `PATCH /alerts/{id}` accepts only `reviewState`; other field updates return HTTP 422.
- Cross-patient access returns HTTP 404.
- Alert creation is not exposed as a public API endpoint — alerts are created server-side by the detection engine only.
- `TestClient` tests cover: list, detail, mark-as-reviewed, cross-patient access.

### MHA-059 — Reminder entity and Room DAO
Room entity for `MedicationReminder` with fields: id, medicineId, patientId, scheduleJson (serialised schedule), isEnabled, createdAt.

Key ACs:
- Schema export updated and committed.
- DAO tests cover: insert, queryByMedicine, toggleEnabled, delete.

### MHA-060 — Reminder creation and management UI
`ReminderSetupScreen` accessible from the medicine detail screen. Frequency pre-populated from the prescription's dosage instructions. Patient can override the schedule. Each reminder is labelled with the prescription date and prescribing doctor.

Key ACs:
- Reminder notification text includes: medicine name, prescribed frequency, and "Based on prescription from [doctor] dated [date] — this is a reminder, not a current clinical instruction."
- Reminder setup screen passes `AccessibilityChecks`.
- Disabling a reminder cancels the pending Android notification.

### MHA-061 — Android notification channels for reminders
Register a `MedicationReminderChannel` notification channel with importance `IMPORTANCE_DEFAULT`. Notifications are scheduled via `AlarmManager` or `WorkManager`. Notifications do not include prescription text or document content.

Key ACs:
- Notification channel is registered on app launch.
- Test verifies notification content contains medicine name and reminder label but no prescription text.
- Notifications are cancelled when the associated medicine is deleted.

### MHA-062 — Dose log entity and Room DAO
Room entity for `DoseLog` with fields: id, medicineId, patientId, loggedAt, action (`TAKEN` / `SKIPPED` / `MISSED`), note (nullable).

Key ACs:
- Schema export updated and committed.
- DAO tests cover: insert, queryByMedicineAndDateRange, delete.
- `note` field must not be logged in Logcat.

### MHA-063 — Dose logging UI
A "Log Dose" action is available on the medicine detail screen. Opens a bottom sheet with three options: Taken, Skipped, Missed. Optional note field. Saves a `DoseLog` record.

Key ACs:
- Logging a dose does not modify `PrescriptionMedicine.reviewState` or any other entity.
- Bottom sheet passes `AccessibilityChecks`.
- Logged dose is immediately reflected in the adherence summary.

### MHA-064 — Adherence tracking screen
`AdherenceScreen` shows a summary per medicine: total doses logged (by type) over a date range selector. Each row links to the medicine detail screen.

Key ACs:
- Date range defaults to the last 30 days; the patient can change it.
- "Logged doses are your self-reported records — they do not represent confirmed medical administration." disclaimer is shown at the top of the screen.
- Empty state shown when no doses have been logged.

### MHA-065 — Interaction checking service interface (backend)
Define an `InteractionCheckService` interface with: `check(ingredientIds: list[str]) -> InteractionResult`. `InteractionResult` includes: `available: bool`, `interactions: list[InteractionRecord]`, `disclaimerText`. The service is backed by the provider configured in `MED_REFERENCE_PROVIDER`; a stub returns `available = False` when no provider is configured.

Key ACs:
- Stub is used in all tests — no calls to real external APIs.
- `disclaimerText` in every real result: "These results are informational only. Please discuss all your medicines with your doctor or pharmacist."
- `InteractionResult` schema passes mypy strict mode.
- Tests verify the unavailable stub returns `available = False` with appropriate `disclaimerText`.

### MHA-066 — Interaction check UI (available state)
`InteractionCheckScreen` shows: list of medicine pairs with reported interactions, nature of each interaction, and the mandatory disclaimer. A "Discuss with your doctor or pharmacist" call-to-action is shown at the top of the results.

Key ACs:
- Disclaimer is always displayed above the results list — not below.
- No result is labelled "safe" — interactions are labelled as "reported" or "no interaction reported."
- Screen passes `AccessibilityChecks`.

### MHA-067 — Interaction check UI (unavailable state)
When `available = False`, `InteractionCheckScreen` shows only: "Interaction checking is not available. Please discuss your medicines with your doctor or pharmacist." No partial results are shown. No implication that the combination is safe.

Key ACs:
- Unavailable state is tested with the stub.
- No network retry is attempted when the service is unavailable due to configuration (vs. transient error — which shows a separate retry option).
- Unavailable state passes `AccessibilityChecks`.

---

## 4. Implementation order

1. **MHA-041** — DuplicateAlert entity and Room DAO (data model anchor)
2. **MHA-042** — Duplicate detection algorithm UseCase (core logic; requires NormalizedIngredient from medicine-intelligence)
3. **MHA-046** — Combination product handling UseCase (extends detection algorithm)
4. **MHA-043** — Duplicate alert list screen (requires entity and detection UseCase)
5. **MHA-044** — Duplicate alert detail screen (requires list screen navigation)
6. **MHA-045** — Mark-as-reviewed action (extends detail screen)
7. **MHA-047** — Alerts backend API (requires FastAPI scaffold and DuplicateAlert schema)
8. **MHA-059** — Reminder entity and Room DAO
9. **MHA-060** — Reminder creation and management UI (requires reminder entity)
10. **MHA-061** — Android notification channels for reminders (extends reminder UI)
11. **MHA-062** — Dose log entity and Room DAO
12. **MHA-063** — Dose logging UI (requires dose log entity)
13. **MHA-064** — Adherence tracking screen (requires dose log DAO)
14. **MHA-065** — Interaction checking service interface (backend)
15. **MHA-066** — Interaction check UI — available state (requires MHA-065)
16. **MHA-067** — Interaction check UI — unavailable state (parallel to MHA-066)

---

## 5. Out of scope

- Diagnosis, prescribing, or dose optimisation — MVP exclusions.
- Any UI that implies a combination is safe.
- Automated medication changes based on interaction results.
- Real medication reference provider integration (requires separate licensing and legal review).

---

## 6. Notes and constraints

- `DetectDuplicatesUseCase` must be deterministic: 100% branch coverage required, and a determinism test is part of the definition of done.
- The mandatory disclaimer on `DuplicateAlertDetailScreen` ("Potential duplicate — please review with your doctor or pharmacist") is hardcoded and must not be configurable or removable.
- The mark-as-reviewed confirmation text is hardcoded and must not be configurable.
- Interaction check results must never use the word "safe" in any label, badge, or message.
- Reminder notifications must not include prescription text or document content.
- All test fixtures use synthetic data only.
- The `MED_REFERENCE_PROVIDER` environment variable is server-side only — never in the APK.
