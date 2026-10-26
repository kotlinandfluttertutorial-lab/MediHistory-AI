package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.theme.MediHistoryTheme

// ---------------------------------------------------------------------------
// StatusBadge previews — one per OcrStatus value
// ---------------------------------------------------------------------------

@Preview(name = "StatusBadge — PENDING", showBackground = true)
@Composable
private fun PreviewStatusBadgePending() {
    MediHistoryTheme(dynamicColor = false) {
        StatusBadge(status = OcrStatus.PENDING, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "StatusBadge — PROCESSING", showBackground = true)
@Composable
private fun PreviewStatusBadgeProcessing() {
    MediHistoryTheme(dynamicColor = false) {
        StatusBadge(status = OcrStatus.PROCESSING, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "StatusBadge — NEEDS_REVIEW", showBackground = true)
@Composable
private fun PreviewStatusBadgeNeedsReview() {
    MediHistoryTheme(dynamicColor = false) {
        StatusBadge(status = OcrStatus.NEEDS_REVIEW, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "StatusBadge — REVIEWED", showBackground = true)
@Composable
private fun PreviewStatusBadgeReviewed() {
    MediHistoryTheme(dynamicColor = false) {
        StatusBadge(status = OcrStatus.REVIEWED, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "StatusBadge — ERROR", showBackground = true)
@Composable
private fun PreviewStatusBadgeError() {
    MediHistoryTheme(dynamicColor = false) {
        StatusBadge(status = OcrStatus.ERROR, modifier = Modifier.padding(8.dp))
    }
}

// ---------------------------------------------------------------------------
// ConfidenceBadge previews — High / Medium / Low
// ---------------------------------------------------------------------------

@Preview(name = "ConfidenceBadge — High (0.85)", showBackground = true)
@Composable
private fun PreviewConfidenceBadgeHigh() {
    MediHistoryTheme(dynamicColor = false) {
        ConfidenceBadge(confidence = 0.85f, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "ConfidenceBadge — Medium (0.62)", showBackground = true)
@Composable
private fun PreviewConfidenceBadgeMedium() {
    MediHistoryTheme(dynamicColor = false) {
        ConfidenceBadge(confidence = 0.62f, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "ConfidenceBadge — Low (0.40)", showBackground = true)
@Composable
private fun PreviewConfidenceBadgeLow() {
    MediHistoryTheme(dynamicColor = false) {
        ConfidenceBadge(confidence = 0.40f, modifier = Modifier.padding(8.dp))
    }
}

// ---------------------------------------------------------------------------
// MedicineStatusChip previews — one per MedicineStatus value
// ---------------------------------------------------------------------------

@Preview(name = "MedicineStatusChip — CURRENT", showBackground = true)
@Composable
private fun PreviewMedicineStatusChipCurrent() {
    MediHistoryTheme(dynamicColor = false) {
        MedicineStatusChip(status = MedicineStatus.CURRENT, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "MedicineStatusChip — PAST", showBackground = true)
@Composable
private fun PreviewMedicineStatusChipPast() {
    MediHistoryTheme(dynamicColor = false) {
        MedicineStatusChip(status = MedicineStatus.PAST, modifier = Modifier.padding(8.dp))
    }
}

@Preview(name = "MedicineStatusChip — UNKNOWN", showBackground = true)
@Composable
private fun PreviewMedicineStatusChipUnknown() {
    MediHistoryTheme(dynamicColor = false) {
        MedicineStatusChip(status = MedicineStatus.UNKNOWN, modifier = Modifier.padding(8.dp))
    }
}

// ---------------------------------------------------------------------------
// EmptyStateView preview
// ---------------------------------------------------------------------------

@Preview(name = "EmptyStateView", showBackground = true, heightDp = 400)
@Composable
private fun PreviewEmptyStateView() {
    MediHistoryTheme(dynamicColor = false) {
        EmptyStateView(
            message = "No prescriptions yet. Add your first prescription to get started.",
            actionLabel = "Add prescription",
            onAction = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// ---------------------------------------------------------------------------
// Component gallery — all chips side by side
// ---------------------------------------------------------------------------

@Preview(name = "Component Gallery", showBackground = true, widthDp = 360, heightDp = 600)
@Composable
private fun PreviewComponentGallery() {
    MediHistoryTheme(dynamicColor = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            OcrStatus.entries.forEach { status ->
                StatusBadge(status = status)
            }
            ConfidenceBadge(confidence = 0.85f)
            ConfidenceBadge(confidence = 0.62f)
            ConfidenceBadge(confidence = 0.40f)
            MedicineStatus.entries.forEach { status ->
                MedicineStatusChip(status = status)
            }
        }
    }
}
