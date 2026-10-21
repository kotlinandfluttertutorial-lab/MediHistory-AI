# Kiro Implementation Prompt — MHAI-003: Prescription capture and import

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-003  
**Requirements:** R1  
**Dependencies:** MHAI-001;MHAI-002  
**Component:** Document Capture  
**Skills:** Android; CameraX; File Handling

## Task
Implement CameraX capture and image/PDF import. Preserve original files and handle invalid/large files safely.

## Acceptance criteria
Capture/import works; original reopens; errors actionable; formats documented.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
