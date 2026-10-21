# Kiro Implementation Prompt — MHAI-011: New Doctor Mode

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-011  
**Requirements:** R5  
**Dependencies:** MHAI-004;MHAI-008;MHAI-010  
**Component:** Doctor Report  
**Skills:** Android; PDF; UX

## Task
Build report builder with patient-entered details, confirmed allergies/conditions, medicine statuses, duplicate findings, reports, and source index.

## Acceptance criteria
Unconfirmed facts labeled; statuses separated; sources present; preview works; no facts invented.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
