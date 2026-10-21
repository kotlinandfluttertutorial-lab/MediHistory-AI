# Kiro Implementation Prompt — MHAI-002: Local data model and offline repository

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-002  
**Requirements:** R1;R3;R6  
**Dependencies:** MHAI-001  
**Component:** Foundation  
**Skills:** Android; Room; Data Modeling

## Task
Define Room entities for documents, extracted fields, medicines, review events, and sync state. Preserve unknown status.

## Acceptance criteria
Records persist and view offline; unknown distinct from current/past; migrations tested; no medical content in logs.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
