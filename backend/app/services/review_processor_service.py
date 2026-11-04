"""
ReviewProcessorService — server-side PostReviewProcessor.

After the patient finishes reviewing all OCR-extracted fields in the Android
app, the app calls POST /prescriptions/{id}/process-review. This service:

1. Reads all REVIEWED extracted fields for the prescription.
2. Resolves/creates a Doctor row from the confirmed doctor_name field.
3. Updates Prescription.doctor_id and prescription_date from confirmed fields.
4. Creates PrescriptionMedicine rows from confirmed medicine_name fields.
5. Runs duplicate detection across all patient medicines.
6. Persists any new DuplicateAlert rows (deduplicates by pair).
7. Advances prescription ocr_status → COMPLETED.

Medical safety invariants:
- Only fields with review_state=REVIEWED and a non-null raw_value are used.
- REJECTED and PENDING_REVIEW fields are ignored entirely — never inferred.
- Doctor specialty defaults to UNKNOWN — never guessed from OCR.
- Medicine status defaults to UNKNOWN — always patient-assigned, never inferred.
- No dose, duration, diagnosis, or allergy is ever fabricated.

Privacy: field values are never logged.
"""

from __future__ import annotations

import re
import uuid
from datetime import date, datetime
from typing import TYPE_CHECKING

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.doctor import Doctor
from app.models.duplicate_alert import DuplicateAlert
from app.models.medicine import PrescriptionMedicine
from app.models.prescription import Prescription
from app.services.duplicate_engine import MatchType, detect

_DATE_FORMATS = [
    "%Y-%m-%d",
    "%d/%m/%Y",
    "%d-%m-%Y",
    "%d %b %Y",
    "%d %B %Y",
    "%B %d, %Y",
    "%d/%m/%y",
]

_FIELD_DOCTOR_NAME = "doctor_name"
_FIELD_PRESCRIPTION_DATE = "prescription_date"
_FIELD_MEDICINE_NAME = "medicine_name"


class ReviewProcessorService:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    async def process(self, prescription_id: str, patient_id: str, reviewed_fields: list[dict]) -> dict:
        """
        Apply patient-confirmed OCR field values to the downstream tables.

        *reviewed_fields* is a list of dicts, each with:
            field_name   str
            raw_value    str | None
            patient_value str | None   — confirmed/edited value
            review_state str           — "REVIEWED" | "REJECTED" | "PENDING_REVIEW"

        Returns a summary dict with counts of records created/updated.
        """
        # Load prescription (patient-scoped)
        result = await self._db.execute(
            select(Prescription)
            .options(selectinload(Prescription.doctor))
            .where(Prescription.id == prescription_id, Prescription.patient_id == patient_id)
        )
        prescription = result.scalar_one_or_none()
        if prescription is None:
            return {"error": "prescription_not_found"}

        # Index REVIEWED fields with non-null patient_value by field_name
        reviewed: dict[str, str] = {}
        for f in reviewed_fields:
            if (
                f.get("review_state") == "REVIEWED"
                and f.get("patient_value") is not None
                and str(f["patient_value"]).strip()
            ):
                reviewed[f["field_name"]] = str(f["patient_value"]).strip()

        # ── Doctor ────────────────────────────────────────────────────────────
        doctor_id: str | None = prescription.doctor_id
        doctor_name_value = reviewed.get(_FIELD_DOCTOR_NAME)
        if doctor_name_value:
            doctor_id = await self._find_or_create_doctor(doctor_name_value, patient_id)

        # ── Prescription date ─────────────────────────────────────────────────
        date_value = reviewed.get(_FIELD_PRESCRIPTION_DATE)
        parsed_date: date | None = self._parse_date(date_value) if date_value else None

        # ── Update prescription ───────────────────────────────────────────────
        changed = False
        if doctor_id and doctor_id != prescription.doctor_id:
            prescription.doctor_id = doctor_id
            changed = True
        if parsed_date and parsed_date != prescription.prescription_date:
            prescription.prescription_date = parsed_date
            changed = True
        if changed:
            await self._db.flush()

        # ── Medicines ─────────────────────────────────────────────────────────
        medicine_entries = {
            k: v for k, v in reviewed.items()
            if k == _FIELD_MEDICINE_NAME or k.startswith(f"{_FIELD_MEDICINE_NAME}_")
        }
        medicines_created = 0
        for field_key in sorted(medicine_entries.keys()):
            name = medicine_entries[field_key].strip()
            if not name:
                continue
            suffix = field_key.removeprefix(_FIELD_MEDICINE_NAME)
            dose = reviewed.get(f"dose{suffix}")
            frequency = reviewed.get(f"frequency{suffix}")
            dosage_instructions = ", ".join(filter(None, [dose, frequency])) or None

            med = PrescriptionMedicine(
                id=str(uuid.uuid4()),
                prescription_id=prescription_id,
                patient_id=patient_id,
                brand_name=name,
                dosage=dosage_instructions,
                medicine_status="UNKNOWN",
                review_state="REVIEWED",
            )
            self._db.add(med)
            medicines_created += 1

        if medicines_created > 0:
            await self._db.flush()

        # ── Duplicate detection ───────────────────────────────────────────────
        alerts_created = 0
        if medicines_created > 0:
            alerts_created = await self._run_duplicate_detection(patient_id)

        # ── Advance OCR status ────────────────────────────────────────────────
        prescription.ocr_status = "COMPLETED"
        await self._db.commit()

        return {
            "prescription_id": prescription_id,
            "ocr_status": "COMPLETED",
            "doctor_id": doctor_id,
            "medicines_created": medicines_created,
            "alerts_created": alerts_created,
        }

    # -------------------------------------------------------------------------
    # Private helpers
    # -------------------------------------------------------------------------

    async def _find_or_create_doctor(self, name: str, patient_id: str) -> str:
        """Find existing doctor by name (case-insensitive) or create a new one."""
        result = await self._db.execute(
            select(Doctor).where(
                Doctor.patient_id == patient_id,
                func.lower(Doctor.name) == name.lower(),
            )
        )
        existing = result.scalar_one_or_none()
        if existing:
            return existing.id

        doctor = Doctor(
            id=str(uuid.uuid4()),
            patient_id=patient_id,
            name=name,
            specialty="UNKNOWN",
        )
        self._db.add(doctor)
        await self._db.flush()
        return doctor.id

    async def _run_duplicate_detection(self, patient_id: str) -> int:
        """
        Run duplicate detection across all patient medicines and persist new alerts.
        Returns count of newly created alerts.
        """
        # Load all patient medicines
        result = await self._db.execute(
            select(PrescriptionMedicine).where(
                PrescriptionMedicine.patient_id == patient_id,
                PrescriptionMedicine.medicine_status != "PAST",
            )
        )
        all_medicines = result.scalars().all()

        med_dicts = [
            {
                "id": m.id,
                "name": m.brand_name or "",
                "active_ingredient": m.active_ingredient or "",
                "status": m.medicine_status,
            }
            for m in all_medicines
        ]

        candidates = detect(med_dicts)
        if not candidates:
            return 0

        # Load existing alert pairs to avoid duplicates
        existing_result = await self._db.execute(
            select(DuplicateAlert.medicine_a_id, DuplicateAlert.medicine_b_id).where(
                DuplicateAlert.patient_id == patient_id
            )
        )
        existing_pairs = {
            frozenset([row[0], row[1]]) for row in existing_result.fetchall()
        }

        created = 0
        for c in candidates:
            pair = frozenset([c.medicine_a_id, c.medicine_b_id])
            if pair in existing_pairs:
                continue
            alert = DuplicateAlert(
                id=str(uuid.uuid4()),
                patient_id=patient_id,
                medicine_a_id=c.medicine_a_id,
                medicine_b_id=c.medicine_b_id,
                matching_ingredient=c.matching_ingredient,
                match_type=c.match_type.value,
                confidence_score=c.confidence,
                combination_note=c.combination_note,
            )
            self._db.add(alert)
            existing_pairs.add(pair)
            created += 1

        if created > 0:
            await self._db.flush()

        return created

    @staticmethod
    def _parse_date(raw: str) -> date | None:
        """Try common date formats. Returns None if none match — never fabricates."""
        for fmt in _DATE_FORMATS:
            try:
                return datetime.strptime(raw.strip(), fmt).date()
            except ValueError:
                continue
        return None
