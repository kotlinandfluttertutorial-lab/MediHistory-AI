# Kiro Implementation Prompt — MHAI-013: Account and authentication

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-013  
**Requirements:** R6;R7  
**Dependencies:** MHAI-001  
**Component:** Cloud Backup  
**Skills:** Backend; Auth; Security

## Task
Implement authentication, session handling, and server-side patient-scoped authorization.

## Acceptance criteria
Unauthorized requests fail; cross-user access blocked; tokens not logged/insecurely stored; tests pass.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
