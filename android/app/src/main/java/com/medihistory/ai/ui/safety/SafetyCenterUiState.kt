package com.medihistory.ai.ui.safety

/**
 * UI state for [SafetyCenterScreen].
 *
 * Medical safety invariants:
 * - There is NO field named "safe", "clear", "noInteractions", or any equivalent.
 * - [interactionCheckStatus] of [InteractionCheckStatus.NotConfigured] is the default and does
 *   NOT imply that medicines are safe to combine — it means checking was not attempted.
 * - An [InteractionCheckStatus.Results] with an empty list is semantically different from
 *   [InteractionCheckStatus.NotConfigured]: it means checking ran and returned no entries,
 *   NOT that the combination is safe.
 *
 * @param unreviewedDuplicateCount Number of potential duplicate alerts that the patient
 *                                  has not yet reviewed. Never implies clinical safety.
 * @param interactionCheckStatus   Current state of the interaction-check feature.
 *                                  Default is [InteractionCheckStatus.NotConfigured].
 * @param isLoading                True while initial data is being loaded.
 */
data class SafetyCenterUiState(
    val unreviewedDuplicateCount: Int = 0,
    val interactionCheckStatus: InteractionCheckStatus = InteractionCheckStatus.NotConfigured,
    val isLoading: Boolean = false,
)
