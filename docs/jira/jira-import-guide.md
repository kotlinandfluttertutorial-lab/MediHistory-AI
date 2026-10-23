# Jira Import Guide — MediHistory AI

**Document version:** 1.0  
**Status:** Draft  
**Last updated:** 2025

This guide walks through the complete procedure for importing the MediHistory AI issue backlog into Jira, verifying traceability, and configuring the project board.

---

## 1. Pre-Import Checklist

Complete every item before running the CSV import. A failed import is harder to clean up than a properly prepared one.

### 1.1 Jira project setup

- [ ] Create a new Jira Software project with key `MHA`.
- [ ] Select project type: **Scrum** (recommended) or Kanban.
- [ ] Set the project lead.
- [ ] Confirm the issue type scheme includes: **Epic**, **Story**, **Task**, **Bug**, **Sub-task**.
- [ ] Enable the **Jira Software** features if using Jira Cloud (required for Story Points and sprints).

### 1.2 Components

Create all ten components listed in `jira-field-mapping.md` Section 2:

- [ ] `Android-UI`
- [ ] `Android-Data`
- [ ] `Android-Background`
- [ ] `Backend-API`
- [ ] `Backend-Data`
- [ ] `OCR-Service`
- [ ] `Security`
- [ ] `CI-CD`
- [ ] `Testing`
- [ ] `Design-System`

### 1.3 Custom fields

Create all nine custom fields listed in `jira-field-mapping.md` Section 3. For each:

- [ ] `Acceptance Criteria` (Paragraph)
- [ ] `Test Requirements` (Paragraph)
- [ ] `Requirement IDs` (Text — single line)
- [ ] `Feature IDs` (Text — single line)
- [ ] `Screen IDs` (Text — single line)
- [ ] `Figma References` (Text — single line)
- [ ] `Skills` (Text — single line)
- [ ] `Local ID` (Text — single line)
- [ ] `Dependencies` (Text — single line)

After creating each field, record the `customfield_XXXXX` ID for use in the import wizard field mapping step.

### 1.4 Priorities

- [ ] Confirm the project's priority scheme includes exactly: `High`, `Medium`, `Low` (these are the values used in the CSV).

### 1.5 Validate the CSV

- [ ] Open `jira-ticket-master.csv` in a spreadsheet tool. Confirm:
  - No rows have an empty **Summary** column.
  - All multi-line fields are wrapped in double quotes.
  - Internal double quotes in field values are escaped as `""`.
  - File encoding is UTF-8.
- [ ] Count rows: the file should contain at least 70 data rows plus 1 header row.

---

## 2. Creating Epics First (Pass 1)

Epics must exist in Jira before stories and tasks can be linked to them.

### 2.1 Create epics manually or via a minimal CSV

Option A — Manual creation (recommended for 12 epics):

1. In the Jira project, go to **Backlog → Create Epic**.
2. Create each epic using the names and descriptions from `jira-epics.md`:

| Local Epic ID | Epic Summary |
|---------------|-------------|
| MHA-E01 | Project Foundation and Architecture |
| MHA-E02 | Android Design System and Navigation |
| MHA-E03 | Patient and Doctor Records |
| MHA-E04 | Prescription Management and OCR |
| MHA-E05 | Medicine Explorer |
| MHA-E06 | Duplicate Ingredient Detection |
| MHA-E07 | Doctor-Ready Medical History |
| MHA-E08 | New Doctor Mode and Export |
| MHA-E09 | Medication Reminders |
| MHA-E10 | Medication Interaction Safety |
| MHA-E11 | Security and Privacy |
| MHA-E12 | Testing, CI/CD, and Release Readiness |

Option B — Epic CSV import:

Create a minimal CSV with headers: `Issue Type,Summary,Description` and one row per epic (Issue Type = `Epic`). Import using the standard CSV import tool.

### 2.2 Record epic key mapping

After creating or importing epics, Jira assigns numeric keys. Record the mapping:

| Local Epic ID | Jira Epic Key (example) | Actual Jira Key (fill in) |
|---------------|------------------------|--------------------------|
| MHA-E01 | MHA-1 | |
| MHA-E02 | MHA-2 | |
| MHA-E03 | MHA-3 | |
| MHA-E04 | MHA-4 | |
| MHA-E05 | MHA-5 | |
| MHA-E06 | MHA-6 | |
| MHA-E07 | MHA-7 | |
| MHA-E08 | MHA-8 | |
| MHA-E09 | MHA-9 | |
| MHA-E10 | MHA-10 | |
| MHA-E11 | MHA-11 | |
| MHA-E12 | MHA-12 | |

### 2.3 Update the CSV epic links

In `jira-ticket-master.csv`, replace every value in the **Epic ID** column with the actual Jira Epic Key from the mapping table above.

Save the updated file as `jira-ticket-master-with-epic-keys.csv`.

---

## 3. Importing Stories and Tasks (Pass 2)

### 3.1 Navigate to the CSV import tool

- **Jira Cloud:** Settings (gear icon) → System → External System Import → CSV.
- **Jira Server/Data Center:** Administration → System → Import and Export → External System Import → CSV.

### 3.2 Upload the CSV

1. Upload `jira-ticket-master-with-epic-keys.csv`.
2. Set encoding to **UTF-8**.
3. Check "Use first row as field names."
4. Click **Next**.

### 3.3 Map fields in the import wizard

Map each CSV column to the corresponding Jira field:

| CSV Column | Map To |
|------------|--------|
| Issue ID | `Local ID` (custom field) |
| Issue Type | `Issue Type` |
| Epic ID | `Epic Link` (Server) or `Parent` (Cloud) |
| Summary | `Summary` |
| Description | `Description` |
| Acceptance Criteria | `Acceptance Criteria` (custom field) |
| Priority | `Priority` |
| Labels | `Labels` |
| Components | `Component/s` |
| Dependencies | `Dependencies` (custom field) |
| Requirement IDs | `Requirement IDs` (custom field) |
| Feature IDs | `Feature IDs` (custom field) |
| Screen IDs | `Screen IDs` (custom field) |
| Figma References | `Figma References` (custom field) |
| Test Requirements | `Test Requirements` (custom field) |
| Skills | `Skills` (custom field) |
| Story Points | `Story Points` |

### 3.4 Configure multi-value fields

- **Labels:** Set delimiter to comma or space (whichever matches the CSV content).
- **Components:** Set delimiter to comma.

### 3.5 Run import with validation

1. Click **Validate** first. Review and resolve any warnings or errors.
2. When validation passes, click **Import**.
3. Download the import log. Note any skipped rows and their reasons.

---

## 4. Verifying Traceability After Import

### 4.1 Count check

Run these Jira filters and compare counts:

| Filter | Expected |
|--------|----------|
| `project = MHA AND issueType = Story` | ≥ 70 |
| `project = MHA AND issueType = Epic` | 12 |
| `project = MHA AND "Epic Link" is EMPTY AND issueType = Story` | 0 (all stories must be linked to an epic) |
| `project = MHA AND "Requirement IDs" is EMPTY` | Should be low; investigate any unlinked tickets |
| `project = MHA AND Component is EMPTY` | 0 (all tickets must have a component) |

### 4.2 Requirement coverage check

For each Functional Requirement (FR-001 through FR-040), run:

```
project = MHA AND "Requirement IDs" ~ "FR-001"
```

Confirm at least one ticket covers each FR. Cross-reference with `traceability-matrix.csv`.

### 4.3 Epic coverage check

For each epic (MHA-E01 through MHA-E12), open the epic in Jira and confirm:

- All child stories are linked.
- The correct components are set.
- Story point totals are reasonable (no epic has 0 points).

### 4.4 Acceptance criteria check

Spot-check 10 randomly selected tickets and confirm:

- The Acceptance Criteria field is populated.
- The Test Requirements field is populated.
- The Story Points value is 1, 2, 3, 5, or 8.

---

## 5. Setting Up Board Columns

### 5.1 Recommended Scrum board column mapping

| Column Label | Jira Status |
|-------------|-------------|
| Backlog | Backlog |
| Ready for Dev | Selected for Development |
| In Progress | In Progress |
| In Review | In Review |
| Testing | Testing |
| Done | Done |

To configure: **Board settings → Columns → Add column** for each non-default column.

### 5.2 Recommended sprints structure

| Sprint | Epics Covered | Approx. Tickets |
|--------|---------------|-----------------|
| Sprint 0 — Foundation | MHA-E01, MHA-E02 | MHA-001 to MHA-015 |
| Sprint 1 — Records Core | MHA-E03, MHA-E04 (partial) | MHA-016 to MHA-026 |
| Sprint 2 — OCR and Review | MHA-E04 (completion) | MHA-027 to MHA-033 |
| Sprint 3 — Explorer and Detection | MHA-E05, MHA-E06 | MHA-034 to MHA-047 |
| Sprint 4 — History and Export | MHA-E07, MHA-E08 | MHA-048 to MHA-058 |
| Sprint 5 — Reminders and Safety | MHA-E09, MHA-E10 | MHA-059 to MHA-067 |
| Sprint 6 — Security and Testing | MHA-E11, MHA-E12 | MHA-068 to MHA-082 |

### 5.3 Definition of Done

Add the following Definition of Done to the project or sprint:

- All acceptance criteria pass.
- Unit tests written and passing in CI.
- UI test written (for UI tickets) and passing in CI.
- Security review completed for security-relevant changes.
- Privacy review completed for any changes touching patient data.
- Accessibility checked (touch targets, content descriptions, TalkBack).
- Documentation and traceability matrix updated.
- No hardcoded credentials or API keys introduced.

---

## 6. Linking to Figma and Documentation

### 6.1 Figma links

After the Figma design handoff, replace all `[FIGMA_URL_TBD]` values in the `Figma References` field with actual Figma node URLs. The format should be:

```
https://www.figma.com/design/{file-id}/MediHistory-AI?node-id={node-id}
```

Use Jira's bulk edit to update the Figma References field for all tickets in an epic at once.

### 6.2 Documentation links

Add the following links to the Jira project description or a pinned Confluence page:

| Document | Path in Repository |
|----------|--------------------|
| Product Requirements | `docs/product/product-requirements.md` |
| Feature Catalog | `docs/product/feature-catalog.md` |
| Acceptance Criteria | `docs/product/acceptance-criteria.md` |
| Architecture | `docs/architecture/architecture.md` |
| Screen Inventory | `docs/design/screen-inventory.md` |
| Jira Epics | `docs/jira/jira-epics.md` |
| Jira Field Mapping | `docs/jira/jira-field-mapping.md` |
| Traceability Matrix | `docs/jira/traceability-matrix.csv` |

### 6.3 Remote link type

In Jira project settings, add a remote link type called `Figma Design` and another called `Repository Doc` to make documentation links distinguishable from code links on the issue view.
