"""
DuplicateEngine — deterministic algorithm for detecting potential duplicate active
ingredients across a patient's medicine list.

Ported from Android DuplicateDetector.kt + IngredientNormalizer.kt.

Medical safety:
- Results are always labelled "Potential duplicate" — never "Duplicate medication".
- This does NOT indicate that any combination is safe, approved, or that medicines
  are clinically interchangeable.
- This is a static, deterministic algorithm — NOT AI-generated.
  Same input always produces the same sorted output.
- Duplicate alerts are informational only; they do not block any action.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from enum import Enum

from app.services.ingredient_normalizer import is_synonym, normalize


class MatchType(str, Enum):
    EXACT = "EXACT"
    PARTIAL = "PARTIAL"


@dataclass(frozen=True)
class DuplicateCandidate:
    """
    An intermediate result before it is persisted as a DuplicateAlert row.

    Medical safety: never implies clinical safety, approval, or interchangeability.
    Always labelled "Potential duplicate" in the UI.
    """

    medicine_a_id: str
    medicine_b_id: str
    matching_ingredient: str
    match_type: MatchType
    confidence: float
    combination_note: str | None


def _split_ingredients(ingredient: str) -> list[str]:
    """
    Split a (possibly normalised) ingredient string on +, / and , delimiters.
    Single-ingredient strings return a one-element list.
    """
    parts = re.split(r"[+/,]", ingredient)
    return [p.strip() for p in parts if p.strip()]


def _build_combination_note(
    medicine_a_name: str,
    medicine_b_name: str,
    matching_ingredient: str,
) -> str:
    return (
        f"Both {medicine_a_name} and {medicine_b_name} contain {matching_ingredient}. "
        "One or both may be a combination product. "
        "Please review with your doctor or pharmacist."
    )


def _check_pair(
    med_a_id: str,
    med_a_name: str,
    ing_a: str,
    med_b_id: str,
    med_b_name: str,
    ing_b: str,
) -> DuplicateCandidate | None:
    """Check one pair for a potential duplicate. Returns None if no overlap."""

    # --- EXACT match (including synonym equivalence) ---
    if is_synonym(ing_a, ing_b):
        return DuplicateCandidate(
            medicine_a_id=med_a_id,
            medicine_b_id=med_b_id,
            matching_ingredient=normalize(ing_a),
            match_type=MatchType.EXACT,
            confidence=0.95,
            combination_note=None,
        )

    # --- PARTIAL match: combination product overlap ---
    a_norm = normalize(ing_a)
    b_norm = normalize(ing_b)
    a_parts = _split_ingredients(a_norm)
    b_parts = _split_ingredients(b_norm)

    # Only check partial when at least one side is a combination product
    if len(a_parts) <= 1 and len(b_parts) <= 1:
        return None

    common = set(a_parts) & set(b_parts)
    if not common:
        return None

    matching = next(iter(sorted(common)))  # deterministic: take alphabetically first
    return DuplicateCandidate(
        medicine_a_id=med_a_id,
        medicine_b_id=med_b_id,
        matching_ingredient=matching,
        match_type=MatchType.PARTIAL,
        confidence=0.70,
        combination_note=_build_combination_note(med_a_name, med_b_name, matching),
    )


def detect(
    medicines: list[dict],
) -> list[DuplicateCandidate]:
    """
    Detect potential duplicate active ingredients across *medicines*.

    Each item in *medicines* must be a dict with keys:
        id              str   — medicine identifier
        name            str   — display name (e.g. brand name)
        active_ingredient str | None — the active ingredient string
        status          str   — "CURRENT" | "PAST" | "UNKNOWN"

    Only medicines with status CURRENT or UNKNOWN and a non-blank active_ingredient
    are considered. PAST medicines are excluded.

    Returns a deterministically sorted list of DuplicateCandidate.
    Empty if no potential duplicates are found. Never raises.
    """
    active = [
        m
        for m in medicines
        if m.get("status") in ("CURRENT", "UNKNOWN")
        and m.get("active_ingredient")
        and m["active_ingredient"].strip()
    ]
    # Sort deterministically by id before processing
    active.sort(key=lambda m: m["id"])

    candidates: list[DuplicateCandidate] = []

    for i in range(len(active)):
        for j in range(i + 1, len(active)):
            a, b = active[i], active[j]
            candidate = _check_pair(
                med_a_id=a["id"],
                med_a_name=a["name"],
                ing_a=a["active_ingredient"],
                med_b_id=b["id"],
                med_b_name=b["name"],
                ing_b=b["active_ingredient"],
            )
            if candidate:
                candidates.append(candidate)

    return sorted(candidates, key=lambda c: (c.medicine_a_id, c.medicine_b_id))
