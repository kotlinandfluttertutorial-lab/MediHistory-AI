# Kiro Implementation Prompt — MHAI-015: Conflict-safe sync

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-015  
**Requirements:** R6  
**Dependencies:** MHAI-014  
**Component:** Cloud Backup  
**Skills:** Sync; Offline; Testing

## Task
Add versioning and conflict resolution for edits, extraction versions, deletion markers, and retryable jobs.

## Acceptance criteria
Confirmed edits not silently overwritten; conflicts visible; uploads idempotent; offline/retry tests pass.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
