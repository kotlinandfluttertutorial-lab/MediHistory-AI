# AI-SDLC Workflow
Traceability: requirement → spec → Jira issue → Kiro prompt → implementation → tests → review evidence.
1. Discover scope, user, risks, and data flows.
2. Specify user stories and acceptance criteria.
3. Design architecture, data model, security, and UX.
4. Import Jira and order dependencies.
5. Implement one ticket at a time.
6. Verify unit, integration, UI, offline, accessibility, and security behavior.
7. Review acceptance criteria and traceability.
8. Release to internal testing with synthetic data and rollback notes.
Each ticket reports changed files, tests, acceptance evidence, privacy impact, limitations, and docs updated.
Release gate: no critical/high security issue; no unreviewed OCR data shown as confirmed; no duplicate finding without source evidence/disclaimer; no sharing without preview and explicit action.
