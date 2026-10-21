# MVP Requirements
**R1 Import:** Capture prescription photo/import supported image/PDF; preserve original; show progress/errors.
**R2 OCR review:** Show extracted text, normalized fields, confidence and review state; leave missing values blank; patient can correct; every medicine links to source.
**R3 History:** List medicines with source/date/strength and current/past/unknown status; new extraction defaults to unknown unless confirmed.
**R4 Duplicate detection:** Compare verified active ingredient IDs; show sources/dates/strengths/mapping state; ambiguous mappings remain unresolved; display professional-review disclaimer.
**R5 New Doctor Mode:** Build selectable report with patient-entered details, confirmed allergies/conditions, current/past/unknown medicines, possible duplicates, reports, and source index; label unconfirmed facts; preview before export.
**R6 Hybrid storage:** Saved records available offline; backup opt-in/authenticated; show sync status; no silent overwrite of confirmed edits.
**R7 Privacy/sharing:** Private by default; explicit preview/action; selected records only; export/delete with retention explanations.
**R8 Reliability/accessibility:** Handle offline, low storage, OCR/network/backup failures; scalable text, screen reader support, contrast, actionable recovery.
