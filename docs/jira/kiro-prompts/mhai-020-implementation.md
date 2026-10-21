# Kiro Implementation Prompt — MHAI-020: MVP release readiness

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-020  
**Requirements:** R1-R8  
**Dependencies:** MHAI-018;MHAI-019  
**Component:** Release Readiness  
**Skills:** QA; Release; Documentation

## Task
Run end-to-end workflows with synthetic data; validate backup, report export, duplicate findings, deletion, accessibility, release notes.

## Acceptance criteria
Acceptance evidence complete; no release-blocking security issue; setup/rollback notes; limitations clear.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
