# Kiro Implementation Prompt — MHAI-005: OCR extraction service

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-005  
**Requirements:** R2  
**Dependencies:** MHAI-003  
**Component:** OCR Review  
**Skills:** Python; OCR; API

## Task
Add provider-neutral OCR interface returning typed fields, confidence, and source location where available.

## Acceptance criteria
Malformed output rejected; uncertain fields flagged; missing values stay missing; synthetic OCR tests pass.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
