# Feature Catalog — MediHistory AI

**Document version:** 1.0  
**Status:** Draft

This table is the single source of truth for feature identity, priority, status, and traceability. Every Jira epic, Kiro spec, and implementation ticket must reference a Feature ID from this catalog.

---

| Feature ID | Feature Name | Priority | Status | Linked FRs | Key Screens | Epic ID |
|------------|-------------|----------|--------|------------|-------------|---------|
| F01 | Patient Dashboard | High | Planned | FR-001, FR-002, FR-003, FR-004 | SCR-01 (Dashboard) | EPIC-01 |
| F02 | Multi-Doctor Prescription Management | High | Planned | FR-005, FR-006, FR-007, FR-008, FR-009, FR-010 | SCR-02 (Prescription List), SCR-03 (Add Prescription), SCR-04 (Prescription Detail), SCR-05 (Doctor Profile) | EPIC-02 |
| F03 | OCR and Prescription Review | High | Planned | FR-011, FR-012, FR-013, FR-014, FR-015, FR-016 | SCR-06 (OCR Status), SCR-07 (Extraction Review), SCR-08 (Manual Entry) | EPIC-03 |
| F04 | Consolidated Medicine Explorer | High | Planned | FR-017, FR-018, FR-019, FR-020, FR-021 | SCR-09 (Medicine List), SCR-10 (Medicine Detail) | EPIC-04 |
| F05 | Potential Duplicate Ingredient Detection | High | Planned | FR-022, FR-023, FR-024, FR-025, FR-026 | SCR-11 (Duplicate Alert List), SCR-12 (Duplicate Alert Detail) | EPIC-05 |
| F06 | AI Medical History Summarizer | High | Planned | FR-027, FR-028, FR-029, FR-030, FR-031 | SCR-13 (History List), SCR-14 (History Detail), SCR-15 (Summary Generation) | EPIC-06 |
| F07 | New Doctor Mode | Medium | Planned | FR-032, FR-033, FR-034 | SCR-16 (New Doctor Mode Flow), SCR-17 (PDF Preview), SCR-18 (Share Consent) | EPIC-07 |
| F08 | Medication Reminders and Adherence | Medium | Planned | FR-035, FR-036 | SCR-19 (Reminders List), SCR-20 (Reminder Setup), SCR-21 (Dose Log) | EPIC-08 |
| F09 | Medication Interaction Checks | Low | Planned | FR-037, FR-038 | SCR-22 (Interaction Check), SCR-23 (Interaction Results) | EPIC-09 |
| F10 | Privacy and Security | High | Planned | FR-039, FR-040 | SCR-24 (Settings / Privacy), SCR-25 (Export), SCR-26 (Backup Status) | EPIC-10 |

---

## Priority definitions

| Priority | Meaning |
|----------|---------|
| High | Required for MVP launch; blocks release if absent |
| Medium | Included in MVP but may ship in a follow-on release |
| Low | Post-MVP or conditional on external dependency (e.g. licensed reference source for F09) |

## Status definitions

| Status | Meaning |
|--------|---------|
| Planned | Requirements written; not yet designed or implemented |
| In Design | Screen and interaction specifications in progress |
| In Development | Jira tickets created; implementation in progress |
| In Review | Implementation complete; under code review and QA |
| Done | All acceptance criteria passed; docs and traceability updated |
| Deferred | Moved to post-MVP by explicit decision |

---

*Screen IDs (SCR-xx) are defined in docs/design/screen-inventory.md. Epic IDs (EPIC-xx) are defined in docs/planning/jira-import.csv.*
