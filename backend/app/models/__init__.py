"""
ORM model registry.

Import all model classes here so that SQLAlchemy's Base.metadata is
fully populated before Alembic autogenerate or create_all() is called.
"""

from app.models.doctor import Doctor
from app.models.duplicate_alert import DuplicateAlert
from app.models.history import MedicalHistorySummary
from app.models.medicine import PrescriptionMedicine
from app.models.patient import Patient
from app.models.prescription import Prescription
from app.models.refresh_token import RefreshToken
from app.models.reminder import Reminder

__all__ = [
    "Doctor",
    "DuplicateAlert",
    "MedicalHistorySummary",
    "Patient",
    "Prescription",
    "PrescriptionMedicine",
    "RefreshToken",
    "Reminder",
]
