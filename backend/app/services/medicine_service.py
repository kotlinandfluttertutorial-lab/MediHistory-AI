"""
Medicine service.

Provides grouped medicine list and update operations.
All operations are patient-scoped.

Medicines are grouped by active_ingredient_slug in Python because SQLite
lacks window functions.
"""

from typing import Optional

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.core.errors import raise_http_error
from app.models.medicine import PrescriptionMedicine
from app.models.prescription import Prescription
from app.schemas.medicine import MedicineGroup, MedicineOccurrence, UpdateMedicineRequest


class MedicineService:
    def __init__(self, db: AsyncSession) -> None:
        self._db = db

    # ------------------------------------------------------------------
    # List grouped
    # ------------------------------------------------------------------

    async def list_grouped(
        self,
        patient_id: str,
        search: Optional[str] = None,
        doctor_id_filter: Optional[str] = None,
        medicine_status_filter: Optional[str] = None,
        page: int = 1,
        page_size: int = 20,
    ) -> tuple[list[MedicineGroup], int]:
        """
        Return medicines grouped by active_ingredient_slug.

        Returns (groups, total_slug_count).
        """
        filters = [PrescriptionMedicine.patient_id == patient_id]
        if medicine_status_filter:
            filters.append(PrescriptionMedicine.medicine_status == medicine_status_filter)
        if search:
            search_lower = search.lower()
            filters.append(
                PrescriptionMedicine.active_ingredient_slug.contains(search_lower)
                | PrescriptionMedicine.brand_name.ilike(f"%{search}%")
            )

        # Load all matching medicines with joined prescription and doctor
        result = await self._db.execute(
            select(PrescriptionMedicine)
            .options(
                selectinload(PrescriptionMedicine.prescription).selectinload(
                    Prescription.doctor
                )
            )
            .where(*filters)
            .order_by(PrescriptionMedicine.active_ingredient_slug)
        )
        medicines = list(result.scalars().all())

        # Apply doctor_id filter after join
        if doctor_id_filter:
            medicines = [
                m
                for m in medicines
                if m.prescription and m.prescription.doctor_id == doctor_id_filter
            ]

        # Group by active_ingredient_slug in Python
        groups_map: dict[str, list[PrescriptionMedicine]] = {}
        for med in medicines:
            slug = med.active_ingredient_slug or "__unknown__"
            groups_map.setdefault(slug, []).append(med)

        # Build MedicineGroup objects
        all_groups = []
        for slug, meds in groups_map.items():
            occurrences = []
            for med in meds:
                doc_name = None
                p_date = None
                if med.prescription:
                    p_date = med.prescription.prescription_date
                    if med.prescription.doctor:
                        doc_name = med.prescription.doctor.name
                occurrences.append(
                    MedicineOccurrence(
                        prescription_medicine_id=med.id,
                        brand_name=med.brand_name,
                        strength=med.strength,
                        formulation=med.formulation,
                        dosage=med.dosage,
                        doctor_name=doc_name,
                        prescription_date=p_date,
                        prescription_id=med.prescription_id,
                        medicine_status=med.medicine_status,
                    )
                )
            sample = meds[0]
            all_groups.append(
                MedicineGroup(
                    active_ingredient=sample.active_ingredient,
                    generic_name=None,
                    occurrences=occurrences,
                )
            )

        total = len(all_groups)

        # Paginate groups
        offset = (page - 1) * page_size
        paginated = all_groups[offset : offset + page_size]

        return paginated, total

    # ------------------------------------------------------------------
    # Get by slug
    # ------------------------------------------------------------------

    async def get_by_slug(
        self, patient_id: str, slug: str
    ) -> Optional[MedicineGroup]:
        """Return the MedicineGroup for the given slug, or None."""
        groups, _ = await self.list_grouped(patient_id=patient_id, search=slug)
        # Filter for exact slug match
        for group in groups:
            sample_slug = (group.active_ingredient or "").lower().replace(" ", "-")
            if sample_slug == slug or slug == slug:
                # Re-fetch with exact slug match
                break

        result = await self._db.execute(
            select(PrescriptionMedicine)
            .options(
                selectinload(PrescriptionMedicine.prescription).selectinload(
                    Prescription.doctor
                )
            )
            .where(
                PrescriptionMedicine.patient_id == patient_id,
                PrescriptionMedicine.active_ingredient_slug == slug,
            )
        )
        medicines = list(result.scalars().all())
        if not medicines:
            return None

        occurrences = []
        for med in medicines:
            doc_name = None
            p_date = None
            if med.prescription:
                p_date = med.prescription.prescription_date
                if med.prescription.doctor:
                    doc_name = med.prescription.doctor.name
            occurrences.append(
                MedicineOccurrence(
                    prescription_medicine_id=med.id,
                    brand_name=med.brand_name,
                    strength=med.strength,
                    formulation=med.formulation,
                    dosage=med.dosage,
                    doctor_name=doc_name,
                    prescription_date=p_date,
                    prescription_id=med.prescription_id,
                    medicine_status=med.medicine_status,
                )
            )

        sample = medicines[0]
        return MedicineGroup(
            active_ingredient=sample.active_ingredient,
            generic_name=None,
            occurrences=occurrences,
        )

    # ------------------------------------------------------------------
    # Update
    # ------------------------------------------------------------------

    async def update(
        self,
        medicine_id: str,
        patient_id: str,
        data: UpdateMedicineRequest,
    ) -> PrescriptionMedicine:
        """
        Update a medicine record.

        Verifies prescription.patient_id == patient_id (404 otherwise).
        Recomputes active_ingredient_slug if active_ingredient changed.
        """
        result = await self._db.execute(
            select(PrescriptionMedicine)
            .options(selectinload(PrescriptionMedicine.prescription))
            .where(PrescriptionMedicine.id == medicine_id)
        )
        medicine = result.scalar_one_or_none()

        if medicine is None:
            raise_http_error(404, "MEDICINE_NOT_FOUND", "Medicine not found.")

        # Patient-scope check
        if medicine.prescription is None or medicine.prescription.patient_id != patient_id:
            raise_http_error(404, "MEDICINE_NOT_FOUND", "Medicine not found.")

        if data.brand_name is not None:
            medicine.brand_name = data.brand_name
        if data.active_ingredient is not None:
            medicine.active_ingredient = data.active_ingredient
            medicine.active_ingredient_slug = data.active_ingredient.lower().replace(" ", "-")
        if data.strength is not None:
            medicine.strength = data.strength
        if data.formulation is not None:
            medicine.formulation = data.formulation
        if data.dosage is not None:
            medicine.dosage = data.dosage
        if data.frequency is not None:
            medicine.frequency = data.frequency
        if data.route is not None:
            medicine.route = data.route
        if data.duration is not None:
            medicine.duration = data.duration
        if data.review_state is not None:
            medicine.review_state = data.review_state
        if data.medicine_status is not None:
            medicine.medicine_status = data.medicine_status

        await self._db.commit()
        await self._db.refresh(medicine)
        return medicine
