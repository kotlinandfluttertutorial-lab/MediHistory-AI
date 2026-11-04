"""
HistoryService — business logic for MedicalHistorySummary operations.

Now uses HistoryCompiler (ported from Android) to build the deterministic
sections synchronously, then optionally calls the Gemini API for the AI
narrative section via HistoryNarrator.

Medical safety: only COMPLETED (reviewed) prescriptions are included.
Privacy: medicine names and doctor names are never logged.
"""

from __future__ import annotations

import json
import uuid
from datetime import UTC, datetime

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.errors import raise_http_error
from app.models.doctor import Doctor
from app.models.history import MedicalHistorySummary
from app.models.medicine import PrescriptionMedicine
from app.models.prescription import Prescription
from app.schemas.history import SummaryContent
from app.services.history_compiler import (
    ContentType,
    HistorySection,
    SectionType,
    compile as compile_history,
)


class HistoryService:
    """Service class for medical history summary operations."""

    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def generate(self, patient_id: str) -> dict:
        """
        Compile a medical history for the patient and persist it.

        Uses HistoryCompiler (deterministic) for Medicines / Doctors / Timeline
        sections. Sets AI_SUMMARY to stub — the AI narrative endpoint
        (POST /ai/narrate-history) is called separately by the Android app.

        Returns:
            dict with summary_id, status, and job_id.
        """
        # Reject if a GENERATING job is already in progress
        in_progress = await self._db.execute(
            select(MedicalHistorySummary).where(
                MedicalHistorySummary.patient_id == patient_id,
                MedicalHistorySummary.status == "GENERATING",
            )
        )
        if in_progress.scalar_one_or_none() is not None:
            raise_http_error(
                409,
                "GENERATION_IN_PROGRESS",
                "A history generation job is already in progress for this patient.",
            )

        # ── Load data ─────────────────────────────────────────────────────────
        prescriptions_result = await self._db.execute(
            select(Prescription)
            .options(selectinload(Prescription.medicines))
            .options(selectinload(Prescription.doctor))
            .where(Prescription.patient_id == patient_id)
        )
        prescriptions = list(prescriptions_result.scalars().all())

        doctors_result = await self._db.execute(
            select(Doctor).where(
                Doctor.patient_id == patient_id,
                Doctor.is_deleted == False,  # noqa: E712
            )
        )
        doctors = list(doctors_result.scalars().all())

        # ── Build dicts for the compiler ──────────────────────────────────────
        prescription_dicts = [
            {
                "id": p.id,
                "ocr_status": p.ocr_status,
                "prescription_date": p.prescription_date,
                "patient_notes": p.patient_notes,
                "doctor_id": p.doctor_id,
            }
            for p in prescriptions
        ]

        medicine_dicts = [
            {
                "id": m.id,
                "prescription_id": m.prescription_id,
                "brand_name": m.brand_name,
                "active_ingredient": m.active_ingredient,
                "strength": m.strength,
                "dosage": m.dosage,
                "medicine_status": m.medicine_status,
            }
            for p in prescriptions
            for m in (p.medicines or [])
        ]

        doctor_dicts = [
            {
                "id": d.id,
                "name": d.name,
                "specialty": d.specialty,
                "clinic_name": d.clinic_name,
            }
            for d in doctors
        ]

        # ── Compile deterministic sections ────────────────────────────────────
        sections = compile_history(prescription_dicts, medicine_dicts, doctor_dicts)

        # ── Serialise to JSON ─────────────────────────────────────────────────
        summary_json = json.dumps(
            {
                "sections": [
                    {
                        "type": s.type.value,
                        "title": s.title,
                        "is_ai_generated": s.is_ai_generated,
                        "entries": [
                            {
                                "text": e.text,
                                "content_type": e.content_type.value,
                                "source_references": [
                                    {"prescription_id": r.prescription_id, "field_name": r.field_name}
                                    for r in e.source_references
                                ],
                            }
                            for e in s.entries
                        ],
                    }
                    for s in sections
                ]
            },
            default=str,
        )

        # ── Determine version number ──────────────────────────────────────────
        max_version_result = await self._db.execute(
            select(MedicalHistorySummary.version_number)
            .where(MedicalHistorySummary.patient_id == patient_id)
            .order_by(MedicalHistorySummary.version_number.desc())
            .limit(1)
        )
        max_version = max_version_result.scalar_one_or_none()
        next_version = (max_version or 0) + 1

        # Mark all existing summaries as not current
        await self._db.execute(
            update(MedicalHistorySummary)
            .where(MedicalHistorySummary.patient_id == patient_id)
            .values(is_current=False)
        )

        # ── Persist summary row ───────────────────────────────────────────────
        job_id = str(uuid.uuid4())
        summary = MedicalHistorySummary(
            id=str(uuid.uuid4()),
            patient_id=patient_id,
            version_number=next_version,
            is_current=True,
            status="COMPLETED",
            job_id=job_id,
            summary_json=summary_json,
        )
        self._db.add(summary)
        await self._db.commit()
        await self._db.refresh(summary)

        return {
            "summary_id": summary.id,
            "status": summary.status,
            "job_id": summary.job_id,
        }

    async def list_summaries(
        self, patient_id: str
    ) -> tuple[list[MedicalHistorySummary], int]:
        result = await self._db.execute(
            select(MedicalHistorySummary)
            .where(MedicalHistorySummary.patient_id == patient_id)
            .order_by(MedicalHistorySummary.version_number.desc())
        )
        summaries = list(result.scalars().all())
        return summaries, len(summaries)

    async def get_summary(
        self, summary_id: str, patient_id: str
    ) -> MedicalHistorySummary | None:
        result = await self._db.execute(
            select(MedicalHistorySummary).where(
                MedicalHistorySummary.id == summary_id,
                MedicalHistorySummary.patient_id == patient_id,
            )
        )
        summary = result.scalar_one_or_none()
        if summary is None:
            return None

        if summary.summary_json:
            try:
                parsed = SummaryContent.model_validate(json.loads(summary.summary_json))
                summary._parsed_summary = parsed  # type: ignore[attr-defined]
            except Exception:
                summary._parsed_summary = None  # type: ignore[attr-defined]
        else:
            summary._parsed_summary = None  # type: ignore[attr-defined]

        return summary
