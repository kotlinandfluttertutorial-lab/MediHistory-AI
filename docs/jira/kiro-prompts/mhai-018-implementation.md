# Kiro Implementation Prompt — MHAI-018: Security and privacy testing

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-018  
**Requirements:** R6;R7  
**Dependencies:** MHAI-013;MHAI-014;MHAI-016;MHAI-017  
**Component:** Release Readiness  
**Skills:** Security Testing; Privacy; QA

## Task
Test cross-account access, IDOR, secrets, sensitive logs, local file exposure, transport, and deletion.

## Acceptance criteria
No critical/high issue remains for release; no medical content in logs; authorization tests pass.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
