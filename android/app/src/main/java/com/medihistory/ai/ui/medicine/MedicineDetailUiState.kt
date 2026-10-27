package com.medihistory.ai.ui.medicine

import com.medihistory.ai.data.model.Medicine

/**
 * UI state for [MedicineDetailScreen].
 *
 * [medicine] is null while loading or when no record is found.
 * [error] is set when the repository returns null (unknown ID or cross-patient access)
 * or when an exception is thrown.
 *
 * Privacy: do not log any field of this state — it contains patient medicine data.
 */
data class MedicineDetailUiState(
    val medicine: Medicine? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
