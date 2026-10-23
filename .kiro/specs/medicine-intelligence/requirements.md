# Requirements — Medicine Intelligence

**Spec version:** 1.0
**Status:** Draft
**Feature(s):** F04
**Jira tickets:** MHA-034, MHA-035, MHA-036, MHA-037, MHA-038, MHA-039, MHA-040
**Functional requirements:** FR-017, FR-018, FR-019, FR-020, FR-021
**Dependencies:** prescription-management spec (PrescriptionMedicine entity and review flow must be complete)

---

## 1. Purpose

This spec covers the consolidated view of all medicines across all prescriptions, and the backend data model that supports it:

- **Medicine entity** — normalised record keyed on verified active ingredient identity
- **Medicine explorer screen** — searchable, scrollable, lazy-loading list of all medicines
- **Search and filters** — search by name or active ingredient; filter by doctor, specialty, date range, review state
- **Medicine detail screen** — all prescription occurrences of a medicine grouped by active ingredient
- **Status assignment** — patient-assigned Current / Past / Unknown grouping (never AI-inferred)
- **Source document links** — every medicine entry links to its source prescription
- **Medicines backend API** — read and update endpoints with patient-scoped authorisation

The medicine entity is the anchor for duplicate detection (medication-safety spec) and history summarisation (medical-history spec). Get this data model right before those specs start.

---

## 2. Functional requirements

### FR-017 — Medicine explorer list
All unique medicines across all prescriptions in a searchable, scrollable list. Each entry shows: medicine name, active ingredient (when known), strength, formulation, most recent prescribing doctor, and most recent prescription date. Lazy loading is required for collections beyond a configurable page size.

### FR-018 — Medicine search and filter
Search bar that queries medicine name and active ingredient. Filter options: by doctor, by specialty, by date range, by review state. Active filters are shown as dismissible `DismissibleChip` components. Filters stack (AND logic).

### FR-019 — Medicine detail screen
All prescription occurrences of a medicine (matched on active ingredient across brands). Each occurrence shows: brand name, strength, formulation, dosage instructions, prescribing doctor, prescription date, and a link to the source prescription. A medicine icon or thumbnail is shown where available.

### FR-020 — Medicine status grouping
Explorer groups medicines into: **Current** (from active/undated prescriptions), **Past** (from prescriptions marked historical), **Unknown** (status not determinable). Status is patient-assigned only — never inferred by AI or derived automatically from prescription date.

### FR-021 — Source document link
Every medicine entry in the explorer and the detail screen links directly to the source `Prescription`. The link navigates to the prescription detail screen with the original document visible.

---

## 3. Tickets and acceptance criteria

### MHA-034 — MedicineEntity Room schema and DAO
Room entity for `MedicineEntity` with fields: id, patientId, normalizedIngredientId (foreign key to `NormalizedIngredient`), brandName, strength, formulation, route, status (`CURRENT` / `PAST` / `UNKNOWN`), createdAt, updatedAt. `NormalizedIngredient` table stores: id, canonicalName, synonyms (JSON array).

Key ACs:
- Schema export JSON is updated and committed.
- DAO tests cover: insert, queryByPatient, queryByIngredient, updateStatus.
- `normalizedIngredientId` is nullable — medicines without a resolved active ingredient are stored with a null reference, not blocked.
- Synonym matching query is covered by a DAO unit test (e.g. "paracetamol" matches synonym "acetaminophen").

### MHA-035 — Medicine explorer screen (Android)
`MedicineExplorerScreen` with a `LazyColumn` inside a `Scaffold`. `MedicineExplorerViewModel` exposes a `StateFlow<MedicineExplorerUiState>` covering: loading, empty, and list states. Medicines are grouped by status (Current → Past → Unknown) using `SectionHeader` components.

Key ACs:
- Initial load state shows `LoadingOverlay`.
- Empty state shows the shared `EmptyState` composable with a message directing the patient to add prescriptions.
- Scrolling to the bottom of a large list triggers the next page load without visible jank.
- All list items have touch targets ≥ 48×48 dp.

### MHA-036 — Medicine search and filter (Android)
Search bar updates the list reactively with debounce (300 ms). Filter bottom sheet or modal allows selecting: doctor, specialty, date range, review state. Active filters appear as dismissible chips above the list.

Key ACs:
- Search with no results shows an empty state with the search term displayed.
- Dismissing a filter chip removes that filter and refreshes the list.
- Filters persist across navigation away and back within the same session.
- Search is tested with: exact name match, partial name match, active ingredient match, no match.

### MHA-037 — Medicine detail screen (Android)
`MedicineDetailScreen` showing: normalised ingredient name (if known), list of occurrences grouped by prescription, status badge, and a source link per occurrence. Status can be changed from this screen.

Key ACs:
- Tapping a source link navigates to the prescription detail screen (verified in navigation test).
- Status change is reflected immediately in the explorer list on back navigation (reactive Flow).
- Screen passes `AccessibilityChecks`.
- If active ingredient is unknown, a plain-language note is shown ("Active ingredient not identified — brand name only").

### MHA-038 — Status assignment UI
The detail screen includes a status selector (segmented control or dropdown): Current / Past / Unknown. Selection updates `MedicineEntity.status` via `UpdateMedicineStatusUseCase`.

Key ACs:
- Default status for a newly extracted medicine is `UNKNOWN` — not inferred from prescription date.
- Status update is persisted to Room and reflected without restart.
- Status selector is labelled for TalkBack ("Medication status").

### MHA-039 — Medicines backend API
FastAPI endpoints: `GET /medicines` (list, paginated), `GET /medicines/{id}` (detail with occurrences), `PATCH /medicines/{id}` (status update only). All endpoints require authentication and patient-scoped authorisation.

Key ACs:
- `PATCH /medicines/{id}` accepts only the `status` field; attempts to update other fields return HTTP 422.
- Cross-patient access returns HTTP 404.
- `GET /medicines` supports `?search=` and `?status=` query parameters.
- `TestClient` integration tests cover: list, detail, status update, cross-patient access attempt, search.

### MHA-040 — Ingredient normalisation service
A `NormalizeIngredientUseCase` maps OCR-extracted medicine names to `NormalizedIngredient` records using a configurable synonym list. The synonym list is bundled as a JSON asset (not fetched at runtime). Unmapped names produce a null `normalizedIngredientId` — they are not blocked or fabricated.

Key ACs:
- Synonym list asset is committed to `android/app/src/main/assets/ingredient_synonyms.json`.
- Unit test verifies "paracetamol" and "acetaminophen" map to the same `NormalizedIngredient`.
- Unit test verifies an unknown name results in null, not an exception.
- Synonym matching is case-insensitive and trims whitespace.

---

## 4. Implementation order

1. **MHA-034** — MedicineEntity Room schema and DAO (data model anchor for this spec)
2. **MHA-040** — Ingredient normalisation service (required before any medicine can be linked to an ingredient)
3. **MHA-035** — Medicine explorer screen (requires entity and DAO)
4. **MHA-036** — Medicine search and filter (extends explorer screen)
5. **MHA-037** — Medicine detail screen (requires explorer navigation)
6. **MHA-038** — Status assignment UI (built into detail screen)
7. **MHA-039** — Medicines backend API (requires FastAPI scaffold and MedicineEntity schema)

---

## 5. Out of scope

- Duplicate detection algorithm and alerts (medication-safety spec — depends on this spec).
- AI history summarisation (medical-history spec — depends on this spec).
- Medication reminders (medication-safety spec).
- Interaction checking (medication-safety spec).
- Pharmacy or drug reference data integration (MVP exclusion).

---

## 6. Notes and constraints

- Medicine status (`CURRENT` / `PAST` / `UNKNOWN`) is always patient-assigned. No code path may derive or infer status automatically.
- `normalizedIngredientId` being null is a valid, expected state — it must not block the patient from viewing or managing the medicine.
- The synonym list (`ingredient_synonyms.json`) is a static asset bundled with the APK; it is not fetched at runtime, and updating it requires an app update.
- All test fixtures use synthetic medicine names (e.g. "Synthamol 500mg", "Testocillin 250mg") — no real brand names.
- The ingredient normalisation result feeds directly into the duplicate detection engine in the medication-safety spec; the normalisation logic must be deterministic and fully unit-tested before that spec begins.
