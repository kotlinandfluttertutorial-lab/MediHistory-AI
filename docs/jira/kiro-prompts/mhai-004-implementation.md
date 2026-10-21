# Kiro Implementation Prompt — MHAI-004: Document library

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-004  
**Requirements:** R1;R3  
**Dependencies:** MHAI-003  
**Component:** Document Capture  
**Skills:** Android; Compose; UX

## Task
Build searchable/filterable library by date, type, and doctor label, with preview and extraction status.

## Acceptance criteria
Search/filter/open works; missing metadata shown as unknown; originals linked; empty/error states tested.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
