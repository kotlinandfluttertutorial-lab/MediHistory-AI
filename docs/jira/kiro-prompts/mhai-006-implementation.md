# Kiro Implementation Prompt — MHAI-006: OCR review UI

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-006  
**Requirements:** R2  
**Dependencies:** MHAI-005  
**Component:** OCR Review  
**Skills:** Android; Compose; OCR Review

## Task
Show original beside extracted fields, confidence/review state, edit/save actions. Keep raw OCR separate from normalized data.

## Acceptance criteria
Uncertain values marked; edits save; source opens; unresolved data never appears confirmed.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
