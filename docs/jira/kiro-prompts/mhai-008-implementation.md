# Kiro Implementation Prompt — MHAI-008: Medication history

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-008  
**Requirements:** R3  
**Dependencies:** MHAI-006;MHAI-007  
**Component:** Medication History  
**Skills:** Android; Data Modeling; UX

## Task
Show history grouped by current/past/unknown, source, date, strength, and review status.

## Acceptance criteria
Every medicine links to source; status explicit; filters work; no automatic current assumption.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
