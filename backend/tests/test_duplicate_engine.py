"""Tests for DuplicateEngine (ported from Android DuplicateDetectorTest)."""
import pytest
from app.services.duplicate_engine import detect, MatchType


def _med(id: str, name: str, ingredient: str, status: str = "CURRENT") -> dict:
    return {"id": id, "name": name, "active_ingredient": ingredient, "status": status}


class TestDetect:
    def test_empty_list_returns_empty(self):
        assert detect([]) == []

    def test_no_duplicates_returns_empty(self):
        meds = [
            _med("m1", "Synthamol 500mg", "synthamolide"),
            _med("m2", "Testocillin 250mg", "testocillin"),
        ]
        assert detect(meds) == []

    def test_exact_match_detected(self):
        meds = [
            _med("m1", "Synthamol 500mg", "paracetamol"),
            _med("m2", "Testamol 650mg", "paracetamol"),
        ]
        results = detect(meds)
        assert len(results) == 1
        assert results[0].match_type == MatchType.EXACT
        assert results[0].matching_ingredient == "paracetamol"

    def test_synonym_pair_detected_as_exact(self):
        meds = [
            _med("m1", "Calpol", "paracetamol"),
            _med("m2", "Dolo", "acetaminophen"),
        ]
        results = detect(meds)
        assert len(results) == 1
        assert results[0].match_type == MatchType.EXACT
        assert results[0].matching_ingredient == "paracetamol"

    def test_past_medicines_excluded(self):
        meds = [
            _med("m1", "Synthamol 500mg", "paracetamol", status="CURRENT"),
            _med("m2", "Testamol 650mg", "paracetamol", status="PAST"),
        ]
        assert detect(meds) == []

    def test_missing_ingredient_excluded(self):
        meds = [
            {"id": "m1", "name": "A", "active_ingredient": None, "status": "CURRENT"},
            {"id": "m2", "name": "B", "active_ingredient": "", "status": "CURRENT"},
        ]
        assert detect(meds) == []

    def test_partial_match_combination_product(self):
        # A+B combination vs plain B
        meds = [
            _med("m1", "Combo A+B", "synthamolide+testocillin"),
            _med("m2", "Plain B", "testocillin"),
        ]
        results = detect(meds)
        assert len(results) == 1
        assert results[0].match_type == MatchType.PARTIAL
        assert results[0].combination_note is not None

    def test_same_combination_both_sides_is_exact(self):
        meds = [
            _med("m1", "Combo X", "synthamolide"),
            _med("m2", "Combo Y", "synthamolide"),
        ]
        results = detect(meds)
        assert len(results) == 1
        assert results[0].match_type == MatchType.EXACT

    def test_output_is_deterministic(self):
        import random
        meds = [
            _med("m1", "A", "paracetamol"),
            _med("m2", "B", "paracetamol"),
            _med("m3", "C", "ibuprofen"),
            _med("m4", "D", "ibuprofen"),
        ]
        result1 = detect(meds)
        shuffled = meds.copy()
        random.shuffle(shuffled)
        result2 = detect(shuffled)
        assert [(r.medicine_a_id, r.medicine_b_id) for r in result1] == \
               [(r.medicine_a_id, r.medicine_b_id) for r in result2]

    def test_confidence_exact_is_higher_than_partial(self):
        exact_med = [
            _med("m1", "A", "paracetamol"),
            _med("m2", "B", "paracetamol"),
        ]
        partial_med = [
            _med("m3", "C", "paracetamol+caffeine"),
            _med("m4", "D", "paracetamol"),
        ]
        exact_result = detect(exact_med)
        partial_result = detect(partial_med)
        assert exact_result[0].confidence > partial_result[0].confidence
