"""
IngredientNormalizer — deterministic synonym mapping for active ingredient names.

Ported from Android IngredientNormalizer.kt.

Maps known synonyms to a single canonical INN (International Nonproprietary Name)
form so that duplicate detection works across equivalent ingredient names regardless
of how they appear on prescriptions.

Medical safety:
- This establishes naming equivalence ONLY.
- It does NOT imply that medicines sharing an active ingredient are clinically
  interchangeable, that any combination is safe, or that any medicine can be
  substituted for another.
- This is a static, deterministic lookup table — NOT AI-generated.
  Same input always produces the same output.
"""

# Synonym map: alternative name (lowercase) → canonical INN name (lowercase).
_SYNONYMS: dict[str, str] = {
    # Paracetamol / acetaminophen
    "paracetamol": "paracetamol",
    "acetaminophen": "paracetamol",
    # Metoprolol salt forms — distinct salts, each maps to itself
    "metoprolol succinate": "metoprolol succinate",
    "metoprolol tartrate": "metoprolol tartrate",
    # Ibuprofen
    "ibuprofen": "ibuprofen",
    # Metformin
    "metformin": "metformin",
    "metformin hydrochloride": "metformin",
    # Amlodipine
    "amlodipine": "amlodipine",
    "amlodipine besylate": "amlodipine",
    "amlodipine maleate": "amlodipine",
    # Atorvastatin
    "atorvastatin": "atorvastatin",
    "atorvastatin calcium": "atorvastatin",
    # Rosuvastatin
    "rosuvastatin": "rosuvastatin",
    "rosuvastatin calcium": "rosuvastatin",
    # Omeprazole
    "omeprazole": "omeprazole",
    "omeprazole magnesium": "omeprazole",
    # Pantoprazole
    "pantoprazole": "pantoprazole",
    "pantoprazole sodium": "pantoprazole",
    # Aspirin
    "aspirin": "aspirin",
    "acetylsalicylic acid": "aspirin",
    # Cetirizine
    "cetirizine": "cetirizine",
    "cetirizine hydrochloride": "cetirizine",
    # Levothyroxine
    "levothyroxine": "levothyroxine",
    "levothyroxine sodium": "levothyroxine",
    # Lisinopril
    "lisinopril": "lisinopril",
    # Telmisartan
    "telmisartan": "telmisartan",
    # Hydrochlorothiazide
    "hydrochlorothiazide": "hydrochlorothiazide",
    "hctz": "hydrochlorothiazide",
    # Pregabalin
    "pregabalin": "pregabalin",
    # Diclofenac
    "diclofenac": "diclofenac",
    "diclofenac sodium": "diclofenac",
    "diclofenac potassium": "diclofenac",
}


def normalize(ingredient: str) -> str:
    """
    Return the canonical INN form of *ingredient*.

    If the ingredient is in the synonym map its canonical name is returned.
    Otherwise the trimmed lowercase form is returned as-is.

    Pure and deterministic: same input always produces the same output.
    """
    key = ingredient.strip().lower()
    return _SYNONYMS.get(key, key)


def is_synonym(a: str, b: str) -> bool:
    """
    Return True if *a* and *b* normalise to the same canonical ingredient name.

    Establishes naming equivalence ONLY — does not imply clinical
    interchangeability or safety.
    """
    return normalize(a) == normalize(b)
