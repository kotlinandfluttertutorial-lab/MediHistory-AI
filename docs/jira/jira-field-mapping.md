# Jira CSV Field Mapping Guide — MediHistory AI

**Document version:** 1.0  
**Status:** Draft  
**Last updated:** 2025

This guide explains every column in `jira-ticket-master.csv`, the expected Jira field type, import configuration, and common resolution steps.

---

## 1. CSV Column Definitions

| Column Name | Description | Jira Field Type | Notes |
|-------------|-------------|-----------------|-------|
| **Issue ID** | Local proposed identifier (e.g. MHA-001). Used for cross-referencing in this repository. Jira ignores this column during import and assigns its own key. | Text (ignored by Jira importer) | Do not use as a Jira issue key. Map to a custom text field "Local ID" if traceability is needed. |
| **Issue Type** | Jira issue type. Values: `Story`, `Task`, `Bug`, `Sub-task`. All tickets in the master CSV are `Story` or `Task`. | Issue Type (system field) | Must match the issue types configured in the target Jira project. |
| **Epic ID** | The local Epic ID (e.g. MHA-E04). During import, replace with the actual Jira epic issue key after epics are created (see Section 3). | Epic Link (custom field) | Epic Link is a Jira-specific field. Map this column to `Epic Link` or `Epic Name` depending on Jira version. In Jira Cloud (new issue view) use `Parent Epic`. |
| **Summary** | One-line issue title. Maximum 255 characters. | Summary (system field) | Required. Import will fail if empty. |
| **Description** | Full issue description including context, rationale, and implementation notes. Multi-line content is wrapped in double quotes in the CSV. | Description (system field) | Supports plain text during import. Jira will render paragraph breaks as-is. |
| **Acceptance Criteria** | Testable conditions that define done. References AC IDs from `acceptance-criteria.md`. Multi-line, wrapped in double quotes. | Custom text field: `Acceptance Criteria` | Create this field as a **Paragraph (multi-line text)** custom field in Jira before import. Custom field ID example: `customfield_10100`. |
| **Priority** | Issue priority. Values: `High`, `Medium`, `Low`. | Priority (system field) | Must match Jira priority names exactly (case-sensitive). Default Jira priorities: Highest, High, Medium, Low, Lowest. Use `High`/`Medium`/`Low`. |
| **Labels** | Space-separated or comma-separated label list. Examples: `android`, `backend`, `ocr`, `security`, `accessibility`. | Labels (system field) | Labels are free-text in Jira. No pre-creation required. Spaces in label names are not permitted — use hyphens. |
| **Components** | Comma-separated component names. Examples: `Android-UI`, `Android-Data`, `Backend-API`, `OCR-Service`, `Security`. | Component (system field) | Components must be pre-created in the Jira project before import. See Section 2. |
| **Dependencies** | Comma-separated local Issue IDs that this ticket depends on (e.g. `MHA-001, MHA-003`). | Custom text field: `Dependencies` | After import, manually create `blocks` / `is blocked by` issue links using the actual Jira keys. |
| **Requirement IDs** | Comma-separated Functional Requirement IDs from `product-requirements.md` (e.g. `FR-005, FR-006`). | Custom text field: `Requirement IDs` | Create as **Text field (single line)**. Used for traceability queries. |
| **Feature IDs** | Comma-separated Feature IDs from `feature-catalog.md` (e.g. `F02, F03`). | Custom text field: `Feature IDs` | Create as **Text field (single line)**. |
| **Screen IDs** | Comma-separated Screen IDs from `screen-inventory.md` (e.g. `SCR-03, SCR-04`). | Custom text field: `Screen IDs` | Create as **Text field (single line)**. |
| **Figma References** | Placeholder Figma URL or `https://www.figma.com/design/xLPxdsIl7CBJOJTtliHKj5/MediHis` for screens not yet designed. Replace with actual Figma node links after design handoff. | Custom text field: `Figma References` | Create as **URL** or **Text (single line)** field. |
| **Test Requirements** | Summary of required test coverage: positive, negative, edge case, security. Multi-line, wrapped in double quotes. | Custom text field: `Test Requirements` | Create as **Paragraph (multi-line text)**. |
| **Skills** | Comma-separated Kiro skill names or implementation prompt references (e.g. `kiro-android-room`, `kiro-backend-fastapi`). | Custom text field: `Skills` | Create as **Text field (single line)**. |
| **Story Points** | Effort estimate in story points. Values: `1`, `2`, `3`, `5`, `8`. | Story Points (system field in Jira Software) | In Jira Cloud this may be labelled `Story point estimate`. In Jira Server it is a custom field. |

---

## 2. Required Jira Project Components

Pre-create the following components in the Jira project before running the CSV import:

| Component Name | Description |
|----------------|-------------|
| `Android-UI` | Jetpack Compose screens and navigation |
| `Android-Data` | Room database, DAOs, repositories |
| `Android-Background` | WorkManager jobs, notification channels |
| `Backend-API` | FastAPI endpoints and schemas |
| `Backend-Data` | PostgreSQL models, migrations |
| `OCR-Service` | OCR and AI extraction pipeline |
| `Security` | Encryption, Keystore, auth, access control |
| `CI-CD` | GitHub Actions workflows |
| `Testing` | Unit, UI, integration, and API test suites |
| `Design-System` | Material 3 theme, Compose component library |

---

## 3. Custom Fields to Create Before Import

Run these steps in **Jira Settings → Issues → Custom Fields → Add custom field** before importing:

| Field Name | Field Type | Purpose |
|------------|------------|---------|
| `Acceptance Criteria` | Paragraph (multi-line text) | Structured AC per ticket |
| `Test Requirements` | Paragraph (multi-line text) | Test coverage requirements |
| `Requirement IDs` | Text field (single line) | FR-xxx traceability |
| `Feature IDs` | Text field (single line) | F0x feature traceability |
| `Screen IDs` | Text field (single line) | SCR-xx screen traceability |
| `Figma References` | Text field (single line) | Figma design links |
| `Skills` | Text field (single line) | Kiro skill references |
| `Local ID` | Text field (single line) | Preserve MHA-xxx local ID |
| `Dependencies` | Text field (single line) | Cross-ticket dependencies |

After creating each field, note the `customfield_XXXXX` ID from the Jira field configuration page. Use these IDs when mapping columns in the CSV import wizard.

---

## 4. Import Settings Summary

| Setting | Value |
|---------|-------|
| **Project key** | `MHA` (or the key assigned when creating the project) |
| **Issue type mapping** | `Story` → Story, `Task` → Task |
| **Date format** | `dd/MMM/yy` or ISO 8601 (`yyyy-MM-dd`) |
| **Encoding** | UTF-8 |
| **Delimiter** | Comma (`,`) |
| **Text qualifier** | Double quote (`"`) |
| **Multi-value separator (Labels)** | Space or comma (configure in import wizard) |
| **Multi-value separator (Components)** | Comma |

---

## 5. Epic Link Handling During Import

Jira's Epic Link field cannot be resolved from a local text ID during bulk CSV import. Follow this two-pass procedure:

**Pass 1 — Import epics only:**
1. Create a separate CSV containing only Epic rows (use a simple spreadsheet with columns: Issue Type=Epic, Summary=epic name, Description).
2. Import the epics. Note the Jira-assigned keys (e.g. `MHA-12`, `MHA-34`).
3. Build a mapping table: Local Epic ID → Jira Epic Key.

**Pass 2 — Import stories and tasks:**
1. In `jira-ticket-master.csv`, replace every `Epic ID` cell value (e.g. `MHA-E04`) with the corresponding Jira Epic Key from the mapping table.
2. Re-run the import with the updated CSV.
3. In the import wizard, map the `Epic ID` column to the `Epic Link` field (Jira Server) or `Parent` field (Jira Cloud next-gen).

> Alternatively, after importing all issues, use Jira's bulk edit to set Epic Link in bulk by filtering on `Labels` containing the epic label (e.g. `epic-e04`).

---

## 6. Common Import Errors and Resolutions

| Error | Cause | Resolution |
|-------|-------|------------|
| `Field 'Story Points' cannot be set` | Story Points field not available in project | Enable Jira Software features on the project, or map to a custom number field |
| `Component 'Android-UI' does not exist` | Component not pre-created | Create all components in project settings before import (see Section 2) |
| `Invalid issue type 'Story'` | Project uses a different issue type scheme | Check project issue type scheme; rename CSV value to match exactly |
| `Epic link cannot be resolved` | Epic key in CSV does not match a real Jira issue | Run Pass 1 (epics import) first; replace local Epic IDs with real Jira keys |
| `Field 'Acceptance Criteria' not recognized` | Custom field not created or column name typo | Create the custom field; verify column header in CSV matches field name exactly |
| `Encoding error on row N` | Non-UTF-8 character in CSV | Save CSV as UTF-8 with BOM using Excel's "CSV UTF-8" export option |
| `Row N skipped — Summary empty` | Summary column is blank | All rows must have a non-empty Summary. Check for line-wrap issues inside quoted fields |
| `Duplicate key` | Re-importing a CSV that was already imported | Use the "Update existing issues" option in the import wizard, or deduplicate rows |
| `Custom field 'Figma References' not found` | Custom field not created | Create the field (see Section 3) before running the import |
| `Multi-line text truncated` | Description or AC column contains unescaped internal double quotes | Escape internal double quotes as `""` (two consecutive double-quote characters) inside a quoted CSV field |

---

## 7. Post-Import Steps

1. **Set Epic Links:** Verify every story/task is linked to an epic. Filter by `Epic Link is EMPTY` to find orphaned tickets.
2. **Create issue links:** For every dependency in the `Dependencies` column, create a `blocks` / `is blocked by` link between the corresponding Jira issues.
3. **Assign to sprints:** Use the Jira board backlog view to drag issues into sprints. Suggested sprint mapping is in `jira-import-guide.md`.
4. **Verify traceability:** Run a Jira filter for each Requirement ID (e.g. `"Requirement IDs" ~ "FR-005"`) and confirm the expected tickets are returned.
5. **Link Figma:** Replace `https://www.figma.com/design/xLPxdsIl7CBJOJTtliHKj5/MediHis` in the `Figma References` field once design handoff is complete.
6. **Validate component assignment:** Filter by `Component is EMPTY` and assign missing components.
