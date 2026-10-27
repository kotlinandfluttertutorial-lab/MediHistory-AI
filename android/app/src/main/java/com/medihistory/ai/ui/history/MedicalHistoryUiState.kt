package com.medihistory.ai.ui.history

import com.medihistory.ai.data.history.HistorySection

/**
 * Immutable UI state for [MedicalHistoryScreen].
 *
 * Privacy: do not log any field of this state — it may contain patient medicine data.
 *
 * @param sections         Compiled history sections; empty until [generateHistory] completes.
 * @param isLoading        True while the initial data load is in progress.
 * @param isGenerating     True while [HistoryCompiler] is running.
 * @param generationProgress Fractional progress (0f–1f) displayed on the progress indicator.
 * @param error            Non-null when the last operation failed; null on success.
 * @param lastUpdated      Unix epoch ms of the last successful compile; null if never compiled.
 */
data class MedicalHistoryUiState(
    val sections: List<HistorySection> = emptyList(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generationProgress: Float = 0f,
    val error: String? = null,
    val lastUpdated: Long? = null,
)
