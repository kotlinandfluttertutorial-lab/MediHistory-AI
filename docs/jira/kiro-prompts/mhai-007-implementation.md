# Kiro Implementation Prompt — MHAI-007: Medicine normalization

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-007  
**Requirements:** R2;R4  
**Dependencies:** MHAI-005;MHAI-006  
**Component:** Duplicate Detection  
**Skills:** Medicine Data; Backend; Safety

## Task
Build reference adapter; preserve raw names; handle ambiguous mappings and combination products.

## Acceptance criteria
Verified mappings return ingredient IDs; ambiguous mappings unresolved; combination products and misspellings tested.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
