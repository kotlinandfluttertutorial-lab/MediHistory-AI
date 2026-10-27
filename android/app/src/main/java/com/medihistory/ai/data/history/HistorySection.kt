package com.medihistory.ai.data.history

/**
 * Categorises the top-level section type within a compiled medical history.
 *
 * [AI_SUMMARY] is always labelled as AI-generated in the UI and never
 * used to convey clinical facts or advice.
 */
enum class SectionType { MEDICINES, DOCTORS, TIMELINE, AI_SUMMARY }

/**
 * Categorises the provenance of a single [HistoryEntry].
 *
 * UI rules:
 * - [DOCUMENTED_FACT]: green check icon — field present and reviewed in source document.
 * - [PATIENT_REPORTED]: person icon — patient-entered note, not from OCR.
 * - [UNCERTAIN_EXTRACTED]: question-mark icon — OCR field below confidence threshold.
 * - [AI_GENERATED]: sparkle icon — content produced by an AI model; always amber chip.
 */
enum class ContentType { DOCUMENTED_FACT, PATIENT_REPORTED, UNCERTAIN_EXTRACTED, AI_GENERATED }

/**
 * A typed link back to the source prescription document.
 *
 * [prescriptionId] identifies the prescription.
 * [fieldName] identifies the field within the OCR output, if known.
 */
data class SourceReference(
    val prescriptionId: String,
    val fieldName: String? = null,
)

/**
 * A single line of content within a [HistorySection].
 *
 * Every entry that originates from a reviewed record MUST include at least one
 * [SourceReference] so the patient can navigate to the source document.
 *
 * Medical safety: this class stores text only — it never stores diagnoses,
 * clinical recommendations, or fabricated values.
 */
data class HistoryEntry(
    val text: String,
    val contentType: ContentType,
    val sourceReferences: List<SourceReference> = emptyList(),
)

/**
 * A named group of [HistoryEntry] items within a compiled medical history.
 *
 * [isAiGenerated] defaults to true when [type] is [SectionType.AI_SUMMARY].
 * The UI must display an amber "AI-generated" chip on every section where
 * [isAiGenerated] is true.
 */
data class HistorySection(
    val type: SectionType,
    val title: String,
    val entries: List<HistoryEntry>,
    val isAiGenerated: Boolean = (type == SectionType.AI_SUMMARY),
)
