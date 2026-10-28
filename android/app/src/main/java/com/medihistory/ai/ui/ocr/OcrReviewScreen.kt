package com.medihistory.ai.ui.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.medihistory.ai.data.ocr.ReviewState
import com.medihistory.ai.ui.components.LoadingIndicator
import com.medihistory.ai.ui.theme.extendedColors
import kotlinx.coroutines.launch

/**
 * OCR review screen (SCR-03-OCR-REVIEW).
 *
 * Allows the patient to confirm, edit, or reject every field extracted from a
 * prescription document by the OCR service.
 *
 * Medical-safety contract:
 * - The original document is always preserved and shown as a placeholder that
 *   opens a document viewer. It is NEVER replaced by extracted text.
 * - Every field shows its confidence score AND review state (never colour alone).
 * - Low-confidence fields (< 0.70) are highlighted amber with BOTH colour AND icon label.
 * - Missing fields are displayed as "Not extracted" — never fabricated.
 * - Review state transitions follow the enforced state machine.
 *
 * @param prescriptionId  The prescription whose extracted fields are reviewed.
 * @param onBack          Called when the patient navigates back.
 * @param viewModel       Hilt-provided [OcrReviewViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrReviewScreen(
    prescriptionId: String,
    onBack: () -> Unit,
    viewModel: OcrReviewViewModel = hiltViewModel(),
) {
    LaunchedEffect(prescriptionId) {
        viewModel.load(prescriptionId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Prescription") },
                navigationIcon = {
                    IconButton(
                        onClick  = onBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back"
                        },
                    ) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.pendingCount > 0) {
                ReviewNextButton(
                    onClick = {
                        coroutineScope.launch {
                            scrollToFirstPending(listState, uiState.fields)
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    LoadingIndicator(
                        contentDescription = "Loading extracted fields",
                    )
                }
            }

            uiState.error != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                ) {
                    Text(
                        text  = uiState.error ?: "An error occurred.",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            else -> {
                LazyColumn(
                    state           = listState,
                    contentPadding  = paddingValues,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier        = Modifier.fillMaxSize(),
                ) {
                    // ── Original document placeholder ─────────────────────────
                    item {
                        DocumentPlaceholder(
                            onTap = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Document viewer coming soon")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp),
                        )
                    }

                    // ── Pending / all-reviewed banners ────────────────────────
                    item {
                        StatusBanner(
                            pendingCount = uiState.pendingCount,
                            allReviewed  = uiState.allReviewed,
                            modifier     = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    }

                    // ── Section header ────────────────────────────────────────
                    item {
                        Text(
                            text     = "EXTRACTED FIELDS",
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    // ── Field cards ───────────────────────────────────────────
                    itemsIndexed(
                        items = uiState.fields,
                        key   = { _, field -> field.fieldId },
                    ) { _, field ->
                        ExtractedFieldCard(
                            field            = field,
                            onConfirm        = { viewModel.onConfirmField(field.fieldId) },
                            onEdit           = { viewModel.onEditField(field.fieldId) },
                            onReject         = { viewModel.onRejectField(field.fieldId) },
                            onUnreview       = { viewModel.onUnreviewField(field.fieldId) },
                            onEditTextChange = { text -> viewModel.onEditTextChange(field.fieldId, text) },
                            onSaveEdit       = { viewModel.onSaveEdit(field.fieldId) },
                            onCancelEdit     = { viewModel.onCancelEdit(field.fieldId) },
                            modifier         = Modifier.padding(horizontal = 16.dp),
                        )
                    }

                    // Trailing spacer so last card isn't hidden behind bottom bar.
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private sub-composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Placeholder that represents the original document.
 *
 * Medical-safety: original document is always preserved and shown here.
 * It is NEVER replaced or obscured by extracted text.
 * Tapping opens the document viewer (stub: shows snackbar).
 */
@Composable
private fun DocumentPlaceholder(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier
            .height(180.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(
                onClickLabel = "Open original document",
                onClick      = onTap,
            )
            .semantics { contentDescription = "Original document preserved. Tap to view." },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector        = Icons.Outlined.Description,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Original document preserved",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Banner strip shown above the field list.
 *
 * - When [pendingCount] > 0: amber banner with count.
 * - When [allReviewed] is true: green success banner.
 * - Otherwise: nothing shown.
 *
 * Both colour AND text convey status — never colour alone.
 */
@Composable
private fun StatusBanner(
    pendingCount: Int,
    allReviewed: Boolean,
    modifier: Modifier = Modifier,
) {
    when {
        pendingCount > 0 -> {
            Surface(
                color    = MaterialTheme.extendedColors.warningContainer,
                shape    = MaterialTheme.shapes.small,
                modifier = modifier.semantics {
                    contentDescription = "$pendingCount field(s) need your review"
                },
            ) {
                Text(
                    text     = "$pendingCount field(s) need your review",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.extendedColors.onWarningContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        allReviewed -> {
            Surface(
                color    = MaterialTheme.colorScheme.tertiaryContainer,
                shape    = MaterialTheme.shapes.small,
                modifier = modifier.semantics {
                    contentDescription = "All fields reviewed. Thank you."
                },
            ) {
                Text(
                    text     = "All fields reviewed. Thank you.",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        else -> {
            // No banner when no pending fields and not all reviewed (e.g. empty list)
        }
    }
}

/**
 * Sticky bottom action bar shown while there are pending fields.
 * Tapping scrolls to the first PENDING_REVIEW field.
 */
@Composable
private fun ReviewNextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier       = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Button(
                onClick  = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Review next field" },
            ) {
                Text("Review next field")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Scroll helper
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Scrolls [listState] to the first field in [fields] that is PENDING_REVIEW.
 *
 * The list has 3 header items (document placeholder, banner, section label)
 * before the field cards start at index 3.
 */
private suspend fun scrollToFirstPending(
    listState: LazyListState,
    fields: List<FieldReviewState>,
) {
    val headerItemCount = 3
    val firstPendingIndex = fields.indexOfFirst {
        it.reviewState == ReviewState.PENDING_REVIEW && it.rawValue != null
    }
    if (firstPendingIndex >= 0) {
        listState.animateScrollToItem(headerItemCount + firstPendingIndex)
    }
}
