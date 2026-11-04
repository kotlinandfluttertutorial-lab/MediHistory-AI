"""
AI Agents router — server-side AI agent endpoints (F11, F12, F15).

POST /ai/understand-prescription  — F11: plain-language prescription explanation
POST /ai/narrate-history          — F15: AI narrative for medical history
POST /ai/prepare-visit            — F12: pre-visit summary with suggested questions

All routes require a valid JWT.
All AI-generated content is labelled with a disclaimer.
Privacy: medicine names and doctor names passed to Gemini are always patient-confirmed
values — never raw document text or unreviewed OCR output.
"""

from __future__ import annotations

import os
from typing import Annotated, Optional

from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.core.security import get_current_patient

router = APIRouter(prefix="/ai", tags=["ai"])

# ---------------------------------------------------------------------------
# Gemini helper — thin wrapper, same pattern as Android GeminiService
# ---------------------------------------------------------------------------

def _gemini_api_key() -> str | None:
    return os.environ.get("GEMINI_API_KEY") or None


async def _generate(prompt: str) -> str | None:
    """
    Call Gemini API and return the text response, or None on failure.
    Privacy: prompt content is never logged.
    """
    api_key = _gemini_api_key()
    if not api_key:
        return None
    try:
        import google.generativeai as genai  # type: ignore[import]
        genai.configure(api_key=api_key)
        model = genai.GenerativeModel(
            model_name="gemini-1.5-flash",
            generation_config={"temperature": 0.3, "max_output_tokens": 512},
        )
        response = model.generate_content(prompt)
        text = (response.text or "").strip()
        return text if text else None
    except Exception:
        # Never log exception — it may echo prompt content containing patient data.
        return None


# ---------------------------------------------------------------------------
# F11 — Prescription Understanding
# ---------------------------------------------------------------------------

class MedicineInput(BaseModel):
    name: str
    strength: Optional[str] = None
    dosage_instructions: Optional[str] = None


class UnderstandPrescriptionRequest(BaseModel):
    medicines: list[MedicineInput]
    doctor_name: Optional[str] = None
    prescription_date: Optional[str] = None


class MedicineUnderstanding(BaseModel):
    medicine_name: str
    note: Optional[str] = None
    is_ai_fallback: bool = False


class UnderstandPrescriptionResponse(BaseModel):
    medicines: list[MedicineUnderstanding]
    disclaimer: str = (
        "AI-generated — always verify with your doctor or pharmacist."
    )


@router.post(
    "/understand-prescription",
    response_model=UnderstandPrescriptionResponse,
    summary="Generate plain-language explanations for medicines on a reviewed prescription (F11)",
)
async def understand_prescription(
    request: UnderstandPrescriptionRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
) -> UnderstandPrescriptionResponse:
    """
    Takes a list of confirmed medicine names and returns a plain-language explanation
    for each one using the Gemini API.

    Input must be patient-confirmed data only — never raw OCR output.
    Medical safety: output never diagnoses, recommends, or advises changing medicines.
    """
    if not request.medicines:
        return UnderstandPrescriptionResponse(medicines=[])

    medicine_list = "\n".join(
        f"- {m.name}{' ' + m.strength if m.strength else ''}"
        + (f" ({m.dosage_instructions})" if m.dosage_instructions else "")
        for m in request.medicines
    )
    context = ""
    if request.doctor_name:
        context += f"Prescribing doctor: {request.doctor_name}. "
    if request.prescription_date:
        context += f"Date: {request.prescription_date}."

    prompt = f"""You are a patient health information assistant.
{context}

Medicines on this prescription:
{medicine_list}

For each medicine, write ONE short sentence (20-35 words) explaining its common general
use in plain language a non-medical person can understand.
Do NOT diagnose, recommend, or advise starting or stopping any medicine.
Do NOT say any medicine is safe, unsafe, or appropriate for this patient.
Start each line with the exact medicine name followed by a colon.
If unsure, write: "[medicine name]: Information not available."
"""

    raw = await _generate(prompt)

    if raw is None:
        return UnderstandPrescriptionResponse(
            medicines=[
                MedicineUnderstanding(
                    medicine_name=m.name, note=None, is_ai_fallback=True
                )
                for m in request.medicines
            ]
        )

    # Parse response
    line_map: dict[str, str] = {}
    for line in raw.splitlines():
        if ":" in line:
            idx = line.index(":")
            key = line[:idx].strip()
            note = line[idx + 1:].strip()
            if key and note:
                line_map[key] = note

    results = []
    for m in request.medicines:
        note = next(
            (v for k, v in line_map.items()
             if m.name.lower().startswith(k.lower()) or k.lower().startswith(m.name.split()[0].lower())),
            None,
        )
        results.append(MedicineUnderstanding(
            medicine_name=m.name,
            note=note if note and note != "Information not available." else None,
            is_ai_fallback=note is None,
        ))

    return UnderstandPrescriptionResponse(medicines=results)


# ---------------------------------------------------------------------------
# F15 — Medicine History Narrator
# ---------------------------------------------------------------------------

class PrescriptionSummaryInput(BaseModel):
    date: Optional[str] = None
    doctor_name: Optional[str] = None
    medicine_names: list[str] = []


class NarrateHistoryRequest(BaseModel):
    prescriptions: list[PrescriptionSummaryInput]


class NarrateHistoryResponse(BaseModel):
    narrative: str
    disclaimer: str = (
        "AI-generated narrative — this is a patient-compiled record, not a clinical report. "
        "Always verify with your doctor."
    )
    is_ai_fallback: bool = False


@router.post(
    "/narrate-history",
    response_model=NarrateHistoryResponse,
    summary="Generate a plain-language medical history narrative (F15)",
)
async def narrate_history(
    request: NarrateHistoryRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
) -> NarrateHistoryResponse:
    """
    Takes a chronological list of reviewed prescriptions and generates a 3-5 sentence
    plain-language narrative of the patient's medication history.

    Medical safety: never diagnoses, never recommends, never infers missing data.
    """
    if not request.prescriptions:
        return NarrateHistoryResponse(
            narrative="No reviewed prescriptions available to summarise.",
            is_ai_fallback=True,
        )

    summaries = "\n".join(
        f"Prescription {i + 1} ({p.date or 'date unknown'}): "
        f"{p.doctor_name or 'unknown doctor'} prescribed "
        f"{', '.join(p.medicine_names) if p.medicine_names else 'no medicines recorded'}."
        for i, p in enumerate(request.prescriptions)
    )

    prompt = f"""You are a patient health record assistant.

Here is a chronological summary of the patient's reviewed prescriptions:
{summaries}

Write a concise, plain-language narrative (3-5 sentences) summarising this medication
history for the patient.
Rules:
- Do NOT diagnose any condition.
- Do NOT recommend starting, stopping, or changing any medicine.
- If no clinical reason is stated, write "the clinical reason is not recorded".
- Keep language simple and reassuring.
- End with: "This summary is based on your reviewed prescription records."
"""

    raw = await _generate(prompt)

    if raw is None:
        return NarrateHistoryResponse(
            narrative=(
                "AI summary generation requires a configured AI provider. "
                "Reviewed records are compiled above."
            ),
            is_ai_fallback=True,
        )

    return NarrateHistoryResponse(narrative=raw)


# ---------------------------------------------------------------------------
# F12 — Prepare for Visit
# ---------------------------------------------------------------------------

class MedicineForVisit(BaseModel):
    name: str
    strength: Optional[str] = None
    dosage_instructions: Optional[str] = None


class AlertForVisit(BaseModel):
    matching_ingredient: str


class PrepareVisitRequest(BaseModel):
    medicines: list[MedicineForVisit]
    open_alerts: list[AlertForVisit] = []
    doctor_name: Optional[str] = None


class SuggestedQuestion(BaseModel):
    text: str
    is_fallback: bool = False


class PrepareVisitResponse(BaseModel):
    current_medicines: list[MedicineForVisit]
    open_alerts: list[AlertForVisit]
    suggested_questions: list[SuggestedQuestion]
    disclaimer: str = (
        "AI-generated — always verify with your doctor or pharmacist."
    )


@router.post(
    "/prepare-visit",
    response_model=PrepareVisitResponse,
    summary="Generate a pre-visit summary with suggested questions (F12)",
)
async def prepare_visit(
    request: PrepareVisitRequest,
    patient_id: Annotated[str, Depends(get_current_patient)],
) -> PrepareVisitResponse:
    """
    Generates 3-5 suggested questions for the patient's upcoming doctor visit.
    Never recommends stopping or changing medicines — questions are framed as
    "consider asking your doctor".
    """
    medicine_list = "\n".join(
        f"- {m.name}{' ' + m.strength if m.strength else ''}"
        for m in request.medicines[:8]
    )
    alert_list = "\n".join(
        f"- Potential duplicate: {a.matching_ingredient} found in two medicines."
        for a in request.open_alerts[:3]
    )
    doctor_part = f"You are visiting {request.doctor_name}." if request.doctor_name else ""

    prompt = f"""You are a patient health assistant helping a patient prepare for a doctor appointment.
{doctor_part}

Current medicines:
{medicine_list or 'None recorded.'}

{('Potential duplicate ingredient alerts:\n' + alert_list) if request.open_alerts else ''}

Write 3-5 plain-language questions the patient should consider asking their doctor.
Rules:
- Frame every question as something the patient "might want to ask" — never a directive.
- Do NOT say any medicine is safe, unsafe, or should be stopped.
- Do NOT diagnose or recommend treatment.
- Keep each question to one sentence.
- Start each question with a dash (-).
"""

    raw = await _generate(prompt)

    if raw is None:
        fallback_questions = []
        if request.medicines:
            fallback_questions.append(SuggestedQuestion(
                text=f"Are all {len(request.medicines)} of my current medicines still needed?",
                is_fallback=True,
            ))
        if request.open_alerts:
            fallback_questions.append(SuggestedQuestion(
                text=f"I have {len(request.open_alerts)} potential duplicate ingredient alert(s) — could you review them with me?",
                is_fallback=True,
            ))
        fallback_questions.append(SuggestedQuestion(
            text="Should I make any changes to my current medicines before the next appointment?",
            is_fallback=True,
        ))
        return PrepareVisitResponse(
            current_medicines=request.medicines,
            open_alerts=request.open_alerts,
            suggested_questions=fallback_questions,
        )

    questions = [
        SuggestedQuestion(text=line.lstrip("- ").strip())
        for line in raw.splitlines()
        if line.strip().startswith("-") and len(line.strip()) > 5
    ][:5]

    return PrepareVisitResponse(
        current_medicines=request.medicines,
        open_alerts=request.open_alerts,
        suggested_questions=questions if questions else [
            SuggestedQuestion(
                text="Are there any changes I should make to my current medicines?",
                is_fallback=True,
            )
        ],
    )
