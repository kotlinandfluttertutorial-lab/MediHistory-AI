# Kiro Implementation Prompt — MHAI-019: Accessibility and resilience

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-019  
**Requirements:** R1;R2;R5;R6  
**Dependencies:** MHAI-002;MHAI-003;MHAI-006;MHAI-011;MHAI-014  
**Component:** Release Readiness  
**Skills:** Accessibility; Android; QA

## Task
Add scalable text, TalkBack labels, contrast, touch targets, offline/error/loading states, low-storage handling.

## Acceptance criteria
Critical flows accessible; offline viewing works; errors actionable; UI states tested.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
