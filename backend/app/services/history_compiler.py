"""
HistoryCompiler — deterministic medical history compiler.

Ported from Android HistoryCompiler.kt.

Compiles a structured medical history from reviewed prescription data.
This is entirely deterministic — same input always produces the same output.
No AI/LLM is involved here; the AI narrative is generated separately by
HistoryNarratorService.

Medical safety:
- Only OCR-reviewed / COMPLETED prescriptions are included.
- Never invents diagnoses, treatment recommendations, or fabricated values.
- Every entry links to the source prescription ID.
- The AI_SUMMARY section is always flagged as AI-generated with a disclaimer.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date
from enum import Enum


class SectionType(str, Enum):
    MEDICINES = "MEDICINES"
    DOCTORS = "DOCTORS"
    TIMELINE = "TIMELINE"
    AI_SUMMARY = "AI_SUMMARY"


class ContentType(str, Enum):
    DOCUMENTED_FACT = "DOCUMENTED_FACT"
    PATIENT_REPORTED = "PATIENT_REPORTED"
    UNCERTAIN_EXTRACTED = "UNCERTAIN_EXTRACTED"
    AI_GENERATED = "AI_GENERATED"


@dataclass
class SourceReference:
    prescription_id: str
    field_name: str | None = None


@dataclass
class HistoryEntry:
    text: str
    content_type: ContentType
    source_references: list[SourceReference] = field(default_factory=list)


@dataclass
class HistorySection:
    type: SectionType
    title: str
    entries: list[HistoryEntry]
    is_ai_generated: bool = False

    def __post_init__(self) -> None:
        if self.type == SectionType.AI_SUMMARY:
            self.is_ai_generated = True


def compile(
    prescriptions: list[dict],
    medicines: list[dict],
    doctors: list[dict],
) -> list[HistorySection]:
    """
    Compile a medical history from reviewed prescription data.

    Each *prescription* dict must have:
        id, ocr_status, prescription_date (date|None), patient_notes (str|None),
        doctor_id (str|None)

    Each *medicine* dict must have:
        id, prescription_id, brand_name (str|None), active_ingredient (str|None),
        strength (str|None), dosage (str|None), medicine_status ("CURRENT"|"PAST"|"UNKNOWN")

    Each *doctor* dict must have:
        id, name, specialty (str|None), clinic_name (str|None)

    Returns a list of HistorySection in display order:
        [MEDICINES, DOCTORS, TIMELINE, AI_SUMMARY (stub)]
    """
    # Only COMPLETED (fully reviewed) prescriptions contribute.
    reviewed = [
        p for p in prescriptions
        if p.get("ocr_status") in ("COMPLETED", "REVIEWED")
    ]
    reviewed.sort(
        key=lambda p: (
            p.get("prescription_date")
            if isinstance(p.get("prescription_date"), date)
            else (date.fromisoformat(p["prescription_date"]) if p.get("prescription_date") else date.min)
        ),
        reverse=True,
    )
    reviewed_ids = {p["id"] for p in reviewed}

    reviewed_medicines = [m for m in medicines if m.get("prescription_id") in reviewed_ids]
    reviewed_medicines.sort(key=lambda m: (m.get("brand_name") or "").lower())

    reviewed_doctor_ids = {p["doctor_id"] for p in reviewed if p.get("doctor_id")}
    reviewed_doctors = [d for d in doctors if d["id"] in reviewed_doctor_ids]
    reviewed_doctors.sort(key=lambda d: d.get("name", "").lower())

    return [
        _compile_medicines_section(reviewed_medicines, reviewed),
        _compile_doctors_section(reviewed_doctors, reviewed),
        _compile_timeline_section(reviewed),
        _stub_ai_summary(),
    ]


# ---------------------------------------------------------------------------
# Section compilers
# ---------------------------------------------------------------------------

def _compile_medicines_section(
    medicines: list[dict],
    reviewed: list[dict],
) -> HistorySection:
    active = [m for m in medicines if m.get("medicine_status") != "PAST"]
    entries: list[HistoryEntry] = []

    for med in active:
        parts: list[str] = []
        if med.get("brand_name"):
            parts.append(med["brand_name"])
        if med.get("active_ingredient"):
            parts.append(f"({med['active_ingredient']})")
        if med.get("strength"):
            parts.append(med["strength"])
        if med.get("dosage"):
            parts.append(f"— {med['dosage']}")
        status = med.get("medicine_status", "UNKNOWN")
        if status not in ("UNKNOWN",):
            parts.append(f"[{status.title()}]")

        is_reviewed = any(p["id"] == med.get("prescription_id") for p in reviewed)
        content_type = ContentType.DOCUMENTED_FACT if is_reviewed else ContentType.PATIENT_REPORTED

        entries.append(HistoryEntry(
            text=" ".join(parts) if parts else "Unnamed medicine",
            content_type=content_type,
            source_references=[
                SourceReference(
                    prescription_id=med["prescription_id"],
                    field_name="brand_name",
                )
            ] if med.get("prescription_id") else [],
        ))

    return HistorySection(
        type=SectionType.MEDICINES,
        title="Active Medicines",
        entries=entries,
    )


def _compile_doctors_section(
    doctors: list[dict],
    reviewed: list[dict],
) -> HistorySection:
    entries: list[HistoryEntry] = []

    for doc in doctors:
        specialty = doc.get("specialty") or ""
        parts = [doc.get("name", "Unknown doctor"), f"— {specialty}"] if specialty else [doc.get("name", "Unknown doctor")]
        if doc.get("clinic_name"):
            parts.append(f"({doc['clinic_name']})")

        # Source: most recent reviewed prescription referencing this doctor
        source_rx = next(
            (p for p in reviewed if p.get("doctor_id") == doc["id"]),
            None,
        )
        entries.append(HistoryEntry(
            text=" ".join(parts),
            content_type=ContentType.DOCUMENTED_FACT,
            source_references=[
                SourceReference(prescription_id=source_rx["id"], field_name="doctor_id")
            ] if source_rx else [],
        ))

    return HistorySection(type=SectionType.DOCTORS, title="Doctors", entries=entries)


def _compile_timeline_section(reviewed: list[dict]) -> HistorySection:
    entries: list[HistoryEntry] = []

    for rx in reviewed:
        if not rx.get("prescription_date"):
            continue  # absent dates are omitted — never fabricated
        parts = [f"Prescription: {rx['prescription_date']}"]
        if rx.get("patient_notes"):
            parts.append(f"— {rx['patient_notes']}")

        entries.append(HistoryEntry(
            text=" ".join(parts),
            content_type=ContentType.DOCUMENTED_FACT,
            source_references=[
                SourceReference(prescription_id=rx["id"], field_name="prescription_date")
            ],
        ))

    return HistorySection(
        type=SectionType.TIMELINE,
        title="Prescription Timeline",
        entries=entries,
    )


def _stub_ai_summary() -> HistorySection:
    """
    Returns a stub AI_SUMMARY section.

    The real AI narrative is generated separately by HistoryNarratorService
    using the Gemini API. This stub is returned when no AI summary is available
    yet.

    Medical safety: MUST NOT contain clinical advice, diagnoses, treatment
    recommendations, or fabricated health information.
    """
    return HistorySection(
        type=SectionType.AI_SUMMARY,
        title="AI Summary",
        is_ai_generated=True,
        entries=[HistoryEntry(
            text=(
                "AI summary generation requires a configured AI provider. "
                "Reviewed records are compiled in the sections above."
            ),
            content_type=ContentType.DOCUMENTED_FACT,
            source_references=[],
        )],
    )
