# Kiro Implementation Prompt — MHAI-009: Duplicate matching engine

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-009  
**Requirements:** R4  
**Dependencies:** MHAI-007;MHAI-008  
**Component:** Duplicate Detection  
**Skills:** Medication Safety; Rules Engine; Testing

## Task
Compare verified ingredient IDs and show source dates, strengths, formulation, and mapping status.

## Acceptance criteria
Matches flagged conservatively; strengths distinct; ambiguous mappings not definitive; evidence/disclaimer included.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
