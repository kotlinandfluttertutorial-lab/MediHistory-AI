# MediHistory AI — Medical Safety Steering

## OCR Fallibility
OCR is imperfect. Every extracted field must display its confidence score and review state to the patient. Low-confidence fields must be visually highlighted. The patient must be able to confirm, edit, or reject every extracted field. No extracted field is treated as verified until the patient has reviewed it.

## Review State Machine
Every extracted field progresses through these states only:
- `PENDING_REVIEW` → initial state after OCR
- `REVIEWED` → patient has confirmed or edited the field
- `REJECTED` → patient has rejected the field

Allowed transitions:
- `PENDING_REVIEW` → `REVIEWED`
- `PENDING_REVIEW` → `REJECTED`
- `REVIEWED` → `PENDING_REVIEW` (patient un-reviews)
- `REJECTED` → `PENDING_REVIEW` (patient un-rejects)

No other transitions are valid. The state machine must be enforced in ValidationUtils.

## No Clinical Advice
The app must never:
- Diagnose any condition
- Recommend starting, stopping, or changing a medicine
- Indicate that a drug combination is safe
- Generate a prognosis or treatment recommendation
- Present AI-generated content as clinical fact

Every AI-generated section must be labelled as AI-generated. Every summary must include a disclaimer that it is a patient-compiled record, not a clinical report.

## Duplicate Labelling Rules
- Alerts are always labelled "Potential duplicate" — never "Duplicate medication" or any clinical term
- Duplicate alerts are informational — they do not block the patient from recording medicines
- Every duplicate alert detail screen must show: "Potential duplicate — please review with your doctor or pharmacist"
- The Mark as Reviewed action must include a disclaimer: "Marking as reviewed means you have seen this alert. It does not mean this combination has been approved by a doctor."

## Combination Medicine Handling
- Combination products (e.g. A+B) must be decomposed into constituent ingredients for duplicate matching
- Partial matches (one ingredient matches) produce a reduced-confidence alert with a note explaining the combination
- The combination note must name both medicines and the matching ingredient

## Never Invent Missing Data
The system must NEVER infer or fabricate:
- Missing dose
- Missing duration
- Missing diagnosis
- Missing allergy
- Missing medicine status

If a field is absent from the source document, it is recorded as absent. The OCR system and AI summarisation system must follow this rule absolutely.

## Privacy Rules
- Never log prescription text, medicine names, doctor names, or document contents in Logcat or server logs
- Use synthetic data in all tests and demos
- Never transmit patient data to any third party without explicit patient consent
