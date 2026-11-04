"""Tests for HistoryCompiler (ported from Android HistoryCompilerTest)."""
import pytest
from app.services.history_compiler import (
    SectionType,
    ContentType,
    compile as compile_history,
)


def _rx(id: str, status: str = "COMPLETED", date: str = "2024-03-10",
        doctor_id: str | None = None, notes: str | None = None) -> dict:
    return {
        "id": id,
        "ocr_status": status,
        "prescription_date": date,
        "patient_notes": notes,
        "doctor_id": doctor_id,
    }


def _med(id: str, rx_id: str, name: str = "Synthamol 500mg",
         ingredient: str = "synthamolide", status: str = "CURRENT") -> dict:
    return {
        "id": id,
        "prescription_id": rx_id,
        "brand_name": name,
        "active_ingredient": ingredient,
        "strength": "500mg",
        "dosage": "1 tablet daily",
        "medicine_status": status,
    }


def _doc(id: str, name: str = "Dr. Synthetic", specialty: str = "CARDIOLOGY") -> dict:
    return {"id": id, "name": name, "specialty": specialty, "clinic_name": None}


class TestCompile:
    def test_empty_input_returns_four_sections(self):
        sections = compile_history([], [], [])
        assert len(sections) == 4
        types = [s.type for s in sections]
        assert SectionType.MEDICINES in types
        assert SectionType.DOCTORS in types
        assert SectionType.TIMELINE in types
        assert SectionType.AI_SUMMARY in types

    def test_only_completed_prescriptions_included(self):
        prescriptions = [
            _rx("rx1", status="COMPLETED"),
            _rx("rx2", status="PENDING"),
        ]
        meds = [_med("m1", "rx1"), _med("m2", "rx2")]
        sections = compile_history(prescriptions, meds, [])
        medicine_section = next(s for s in sections if s.type == SectionType.MEDICINES)
        # Only rx1 is COMPLETED, so only m1 should appear
        assert len(medicine_section.entries) == 1

    def test_past_medicines_excluded_from_medicines_section(self):
        prescriptions = [_rx("rx1")]
        meds = [
            _med("m1", "rx1", status="CURRENT"),
            _med("m2", "rx1", status="PAST"),
        ]
        sections = compile_history(prescriptions, meds, [])
        medicine_section = next(s for s in sections if s.type == SectionType.MEDICINES)
        assert len(medicine_section.entries) == 1

    def test_timeline_excludes_prescriptions_with_no_date(self):
        prescriptions = [
            _rx("rx1", date="2024-03-10"),
            _rx("rx2", date=None),
        ]
        sections = compile_history(prescriptions, [], [])
        timeline = next(s for s in sections if s.type == SectionType.TIMELINE)
        assert len(timeline.entries) == 1

    def test_ai_summary_section_is_always_stub(self):
        sections = compile_history([_rx("rx1")], [], [])
        ai_section = next(s for s in sections if s.type == SectionType.AI_SUMMARY)
        assert ai_section.is_ai_generated is True
        assert len(ai_section.entries) == 1
        assert "AI" in ai_section.entries[0].text or "reviewed" in ai_section.entries[0].text.lower()

    def test_doctors_section_uses_reviewed_prescriptions_only(self):
        prescriptions = [
            _rx("rx1", doctor_id="doc1"),
            _rx("rx2", status="PENDING", doctor_id="doc2"),
        ]
        docs = [_doc("doc1", "Dr. Alpha"), _doc("doc2", "Dr. Beta")]
        sections = compile_history(prescriptions, [], docs)
        doctor_section = next(s for s in sections if s.type == SectionType.DOCTORS)
        # Only doc1 is referenced by a COMPLETED prescription
        assert len(doctor_section.entries) == 1
        assert "Dr. Alpha" in doctor_section.entries[0].text

    def test_medicine_entry_content_type_is_documented_fact(self):
        prescriptions = [_rx("rx1")]
        meds = [_med("m1", "rx1")]
        sections = compile_history(prescriptions, meds, [])
        medicine_section = next(s for s in sections if s.type == SectionType.MEDICINES)
        assert medicine_section.entries[0].content_type == ContentType.DOCUMENTED_FACT

    def test_source_reference_links_to_prescription(self):
        prescriptions = [_rx("rx1")]
        meds = [_med("m1", "rx1")]
        sections = compile_history(prescriptions, meds, [])
        medicine_section = next(s for s in sections if s.type == SectionType.MEDICINES)
        refs = medicine_section.entries[0].source_references
        assert len(refs) == 1
        assert refs[0].prescription_id == "rx1"
