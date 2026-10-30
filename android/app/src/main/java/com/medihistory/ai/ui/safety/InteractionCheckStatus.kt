package com.medihistory.ai.ui.safety

/**
 * Sealed class representing all possible states for the interaction-check feature.
 *
 * Medical safety invariants:
 * - There is NO state that implies a medicine combination is safe.
 * - [NotConfigured] is semantically distinct from [Results] with an empty list.
 *   An empty list would mean "checking ran and found nothing". [NotConfigured] means
 *   "checking did not run because no verified reference source is available."
 * - When checking cannot be completed, the patient is directed to a doctor or pharmacist —
 *   the app NEVER concludes that the absence of results means safety.
 * - The word "safe" must never appear in any UI text produced by these states.
 */
sealed class InteractionCheckStatus {
    /**
     * No verified medicine reference source has been configured.
     * Interaction checking is unavailable.
     *
     * UI must display:
     * "Interaction checking not available"
     * "This does not mean your medicines are safe to combine.
     *  Please consult your doctor or pharmacist."
     *
     * NOTE: This is not the same as [Results] with an empty list.
     * It means checking could not be attempted, not that no interactions were found.
     */
    object NotConfigured : InteractionCheckStatus()

    /**
     * Interaction check is in progress.
     * UI should show a loading indicator.
     */
    object Checking : InteractionCheckStatus()

    /**
     * Interaction check completed and produced a list of results.
     *
     * An empty [interactions] list means the check ran and found nothing to report.
     * It does NOT mean the medicines are confirmed safe to combine.
     * The UI must never present an empty list as a safety confirmation.
     */
    data class Results(val interactions: List<InteractionResult>) : InteractionCheckStatus()

    /**
     * The interaction check encountered an error and could not complete.
     * UI should show [message] and offer a retry action.
     * This state must NOT be presented as "No interactions found."
     */
    data class Error(val message: String) : InteractionCheckStatus()
}

/**
 * A single interaction result between two medicines.
 *
 * Medical safety invariants:
 * - [severity] NEVER uses the word "safe" or implies clinical approval.
 *   Permitted values: "Consult doctor", "Review with pharmacist".
 * - [reviewWarning] is ALWAYS non-empty. It directs the patient to a healthcare professional.
 *   The app never concludes that a combination is clinically approved.
 *
 * @param medicineAName   Display name of the first medicine in the pair.
 * @param medicineBName   Display name of the second medicine in the pair.
 * @param severity        Informational label indicating urgency of review.
 *                        Allowed values: "Consult doctor", "Review with pharmacist".
 *                        NEVER "safe", "no interaction", or any clinical clearance term.
 * @param reviewWarning   Always-present disclaimer directing the patient to consult a professional.
 *                        Example: "Please review this with your doctor or pharmacist."
 *                        Must be non-empty.
 */
data class InteractionResult(
    val medicineAName: String,
    val medicineBName: String,
    val severity: String,
    val reviewWarning: String,
) {
    init {
        require(reviewWarning.isNotBlank()) {
            "reviewWarning must not be blank — every interaction result requires a " +
                "disclaimer directing the patient to consult a healthcare professional."
        }
        require(!severity.lowercase().contains("safe")) {
            "severity must never contain 'safe'. Use 'Consult doctor' or 'Review with pharmacist'."
        }
        require(!severity.lowercase().contains("no interaction")) {
            "severity must never contain 'no interaction'. " +
                "Use 'Consult doctor' or 'Review with pharmacist'."
        }
    }
}
