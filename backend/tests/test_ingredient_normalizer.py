"""Tests for IngredientNormalizer (ported from Android IngredientNormalizerTest)."""
import pytest
from app.services.ingredient_normalizer import normalize, is_synonym


class TestNormalize:
    def test_known_synonym_returns_canonical(self):
        assert normalize("acetaminophen") == "paracetamol"

    def test_canonical_name_returns_itself(self):
        assert normalize("paracetamol") == "paracetamol"

    def test_case_insensitive(self):
        assert normalize("ACETAMINOPHEN") == "paracetamol"
        assert normalize("Paracetamol") == "paracetamol"

    def test_unknown_ingredient_returns_lowercased(self):
        assert normalize("Synthamolide") == "synthamolide"

    def test_strips_whitespace(self):
        assert normalize("  paracetamol  ") == "paracetamol"

    def test_metformin_hydrochloride_maps_to_metformin(self):
        assert normalize("metformin hydrochloride") == "metformin"

    def test_aspirin_synonym(self):
        assert normalize("acetylsalicylic acid") == "aspirin"

    def test_amlodipine_besylate_maps_to_amlodipine(self):
        assert normalize("amlodipine besylate") == "amlodipine"


class TestIsSynonym:
    def test_synonym_pair_returns_true(self):
        assert is_synonym("acetaminophen", "paracetamol") is True

    def test_same_ingredient_returns_true(self):
        assert is_synonym("ibuprofen", "ibuprofen") is True

    def test_different_ingredients_returns_false(self):
        assert is_synonym("ibuprofen", "paracetamol") is False

    def test_salt_form_is_synonym(self):
        assert is_synonym("metformin hydrochloride", "metformin") is True
