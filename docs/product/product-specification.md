# Product Specification
## Vision
A privacy-first Android app for maintaining a reliable, source-linked personal medical record across multiple providers and sharing an understandable summary with a new doctor.
## Problem
Patients may receive prescriptions from multiple doctors and forget medicine names, strengths, dates, allergies, or tests. Handwritten prescriptions are difficult to organize; overlapping active ingredients may go unnoticed.
## Goals
- Collect prescriptions and reports.
- Preserve originals and make OCR reviewable.
- Flag potential duplicate active ingredients for professional review.
- Generate a source-linked New Doctor Mode summary.
- Support offline access and opt-in encrypted cloud backup.
## Non-goals
Diagnosis, prescribing, dose optimization, automatic medication reconciliation, pharmacy services, EHR integration, emergency triage, or clinical-safety claims.
## Core flow
Import prescription → extract candidate fields → patient reviews/corrects → maintain source-linked medicine history → compare verified ingredients → select records → preview New Doctor Mode report → export/share intentionally.
## Initial success criteria
Every medicine links to its source; low-confidence fields are visibly marked; duplicate alerts show evidence and disclaimer; locally saved records are viewable offline; authorization tests block cross-user access.
