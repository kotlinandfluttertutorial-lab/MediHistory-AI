# Kiro Implementation Prompt — MHAI-014: Encrypted cloud backup

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-014  
**Requirements:** R6  
**Dependencies:** MHAI-002;MHAI-003;MHAI-013  
**Component:** Cloud Backup  
**Skills:** Android; Backend; Cloud; Security

## Task
Implement opt-in backup, authenticated upload/download, encrypted object storage, retryable sync, and status.

## Acceptance criteria
Backup off until opt-in; status/errors visible; ownership enforced; retries safe; no sensitive logs.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
