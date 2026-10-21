# Kiro Implementation Prompt — MHAI-012: PDF export

Read the README, all `.kiro/steering/*.md`, `.kiro/specs/medihistory-mvp/requirements.md`, and relevant skills before editing.

**Jira:** MHAI-012  
**Requirements:** R5;R7  
**Dependencies:** MHAI-011  
**Component:** Doctor Report  
**Skills:** Android; PDF; Privacy

## Task
Generate readable PDF with selected sections, date, source index, limitations, and explicit share confirmation.

## Acceptance criteria
Only selected sections included; readable PDF; sharing requires action; file handled safely.

## Rules
Inspect the repository first and do not overwrite unrelated work. Keep scope focused. Use synthetic data only. Add tests for normal, edge, failure, and privacy/security cases. Preserve originals and source links. Never invent medical facts or advise medication changes. Do not log medical content or commit secrets. Update relevant docs and report unmet criteria honestly.

## Report after implementation
1. Files changed and why.
2. Acceptance criteria evidence.
3. Tests and results.
4. Privacy/security impact.
5. Limitations and follow-up work.
