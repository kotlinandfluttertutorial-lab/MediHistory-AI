package com.medihistory.ai.ui.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medihistory.ai.ui.prescription.PrescriptionCard
import com.medihistory.ai.ui.theme.extendedColors

/**
 * Dashboard screen — SCR-01-DASHBOARD, Feature F01.
 *
 * Matches Figma spec:
 * - LargeTopAppBar that collapses on scroll with greeting + date
 * - 2×2 OutlinedCard stats grid with icons; each tile is tappable
 * - Horizontally-scrollable 72dp square quick-action buttons
 * - "Needs your attention" attention card (always shown; "All caught up" when nothing pending)
 * - Recent activity list using existing PrescriptionCard
 * - Empty state for first-time users
 * - Skeleton shimmer loading state
 * - Extended FAB that shrinks on scroll-down
 *
 * Medical safety: no clinical advice. Stats are counts only.
 * Privacy: patientId is passed to ViewModel only; never surfaced in UI strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    patientId: String,
    onAddPrescription: () -> Unit,
    onViewDuplicates: () -> Unit,
    onViewHistory: () -> Unit,
    onNewDoctorMode: () -> Unit,
    onViewPrescriptions: () -> Unit,
    onViewMedicines: () -> Unit = {},
    onViewDoctors: () -> Unit = {},
    onViewProfile: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(patientId) { viewModel.load(patientId) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val isFabExpanded by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    var showVisitSheet by rememberSaveable { mutableStateOf(false) }
    val visitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DashboardTopBar(
                patientName = uiState.patientName.ifBlank { "there" },
                currentDate = uiState.currentDate,
                pendingAlertCount = uiState.pendingAlertCount,
                onViewDuplicates = onViewDuplicates,
                onViewProfile = onViewProfile,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Add Prescription") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onAddPrescription,
                expanded = isFabExpanded,
                modifier = Modifier.semantics { contentDescription = "Add prescription" },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> DashboardSkeleton(modifier = Modifier.padding(innerPadding))

            uiState.error != null -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Failed to load dashboard.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(onClick = { viewModel.load(patientId) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            // First-use empty state
            uiState.prescriptionCount == 0 && !uiState.isLoading -> {
                DashboardEmptyState(
                    patientName = uiState.patientName.ifBlank { "there" },
                    onAddPrescription = onAddPrescription,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = innerPadding,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                ) {
                    // ── 1. Stats grid ──────────────────────────────────────────
                    item {
                        StatsGrid(
                            uiState = uiState,
                            onViewPrescriptions = onViewPrescriptions,
                            onViewMedicines = onViewMedicines,
                            onViewDuplicates = onViewDuplicates,
                            onViewDoctors = onViewDoctors,
                        )
                    }

                    // ── 2. Quick-action buttons ────────────────────────────────
                    item {
                        QuickActionsRow(
                            onAddPrescription = onAddPrescription,
                            onViewDuplicates = onViewDuplicates,
                            onViewHistory = onViewHistory,
                            onNewDoctorMode = onNewDoctorMode,
                        )
                    }

                    // ── 3. Attention card (always visible) ─────────────────────
                    item {
                        AttentionCard(
                            pendingOcrCount = uiState.pendingOcrCount,
                            pendingAlertCount = uiState.pendingAlertCount,
                            onReviewOcr = onViewPrescriptions,
                            onViewDuplicates = onViewDuplicates,
                            onViewHistory = onViewHistory,
                        )
                    }

                    // ── 4. Prepare for visit card (F12) ────────────────────────
                    item {
                        PrepareForVisitCard(
                            isLoading = uiState.isVisitSummaryLoading,
                            hasSummary = uiState.visitSummary != null,
                            onPrepare = {
                                viewModel.prepareForVisit(patientId)
                                showVisitSheet = true
                            },
                        )
                    }

                    // ── 5. Recent activity ─────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Recent activity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            TextButton(onClick = onViewPrescriptions) {
                                Text("See all")
                            }
                        }
                    }

                    if (uiState.recentPrescriptions.isEmpty()) {
                        item {
                            Text(
                                text = "No prescriptions yet. Add your first one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    } else {
                        items(
                            items = uiState.recentPrescriptions,
                            key = { rx -> rx.id },
                        ) { prescription ->
                            PrescriptionCard(
                                prescription = prescription,
                                onClick = onViewPrescriptions,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    // Bottom spacer to clear the FAB
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }

    // ── Visit preparation bottom sheet (F12) ─────────────────────────────────
    if (showVisitSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVisitSheet = false },
            sheetState = visitSheetState,
        ) {
            VisitSummarySheetContent(
                uiState = uiState,
                onDismiss = { showVisitSheet = false },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Top App Bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    patientName: String,
    currentDate: String,
    pendingAlertCount: Int,
    onViewDuplicates: () -> Unit,
    onViewProfile: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    LargeTopAppBar(
        title = {
            Column {
                if (currentDate.isNotEmpty()) {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Good morning, $patientName 👋",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        actions = {
            val alertDesc =
                if (pendingAlertCount > 0) {
                    "Notifications. $pendingAlertCount unread alerts."
                } else {
                    "Notifications. No unread alerts."
                }

            IconButton(
                onClick = onViewDuplicates,
                modifier = Modifier.semantics { contentDescription = alertDesc },
            ) {
                BadgedBox(
                    badge = {
                        if (pendingAlertCount > 0) {
                            Badge { Text(pendingAlertCount.toString()) }
                        }
                    },
                ) {
                    Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                }
            }
            // Profile avatar circle
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClickLabel = "View profile") { onViewProfile() }
                        .semantics { contentDescription = "Profile. Tap to view your profile." },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = patientName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(8.dp))
        },
        scrollBehavior = scrollBehavior,
        colors =
            TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Stats 2×2 grid — OutlinedCards, tappable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsGrid(
    uiState: DashboardUiState,
    onViewPrescriptions: () -> Unit,
    onViewMedicines: () -> Unit,
    onViewDuplicates: () -> Unit,
    onViewDoctors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(
                icon = Icons.Outlined.Receipt,
                count = uiState.prescriptionCount,
                label = "Prescriptions",
                countColor = MaterialTheme.colorScheme.primary,
                onClick = onViewPrescriptions,
                contentDesc = "${uiState.prescriptionCount} prescriptions. Tap to view.",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Outlined.MedicalServices,
                count = uiState.doctorCount,
                label = "Doctors",
                countColor = MaterialTheme.colorScheme.secondary,
                onClick = onViewDoctors,
                contentDesc = "${uiState.doctorCount} doctors. Tap to manage.",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatTile(
                icon = Icons.Outlined.Medication,
                count = uiState.medicineCount,
                label = "Medicines",
                countColor = MaterialTheme.colorScheme.tertiary,
                onClick = onViewMedicines,
                contentDesc = "${uiState.medicineCount} active medicines. Tap to view.",
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Outlined.WarningAmber,
                count = uiState.pendingAlertCount,
                label = "Pending alerts",
                countColor =
                    if (uiState.pendingAlertCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                onClick = onViewDuplicates,
                contentDesc = "${uiState.pendingAlertCount} pending duplicate alerts. Tap to review.",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    count: Int,
    label: String,
    countColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    contentDesc: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = contentDesc },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = countColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick-action buttons — 72dp square FilledTonalButton tiles
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onAddPrescription: () -> Unit,
    onViewDuplicates: () -> Unit,
    onViewHistory: () -> Unit,
    onNewDoctorMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickActionTile(
            icon = Icons.Outlined.Photo,
            label = "Add\nPrescription",
            onClick = onAddPrescription,
            contentDesc = "Add prescription",
        )
        QuickActionTile(
            icon = Icons.Outlined.CompareArrows,
            label = "Check\nDuplicates",
            onClick = onViewDuplicates,
            contentDesc = "Check duplicates",
        )
        QuickActionTile(
            icon = Icons.Outlined.HistoryEdu,
            label = "My\nHistory",
            onClick = onViewHistory,
            contentDesc = "View my history",
        )
        QuickActionTile(
            icon = Icons.Outlined.PersonAdd,
            label = "New\nDoctor",
            onClick = onNewDoctorMode,
            contentDesc = "New doctor mode",
        )
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentDesc: String,
) {
    ElevatedCard(
        onClick = onClick,
        modifier =
            Modifier
                .size(80.dp)
                .semantics { contentDescription = contentDesc },
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Attention card — always shown
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AttentionCard(
    pendingOcrCount: Int,
    pendingAlertCount: Int,
    onReviewOcr: () -> Unit,
    onViewDuplicates: () -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasActions = pendingOcrCount > 0 || pendingAlertCount > 0

    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Needs your attention" },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector =
                        if (hasActions) {
                            Icons.Outlined.NotificationsActive
                        } else {
                            Icons.Outlined.CheckCircle
                        },
                    contentDescription = null,
                    tint =
                        if (hasActions) {
                            MaterialTheme.extendedColors.warning
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (hasActions) "Needs your attention" else "All caught up ✓",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color =
                        if (hasActions) {
                            MaterialTheme.extendedColors.onWarningContainer
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                )
            }

            if (hasActions) {
                Spacer(Modifier.height(12.dp))

                if (pendingOcrCount > 0) {
                    AttentionRow(
                        icon = Icons.Outlined.Warning,
                        text = "OCR review needed — $pendingOcrCount prescription(s)",
                        actionLabel = "Review →",
                        onClick = onReviewOcr,
                    )
                }

                if (pendingOcrCount > 0 && pendingAlertCount > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                if (pendingAlertCount > 0) {
                    AttentionRow(
                        icon = Icons.Outlined.WarningAmber,
                        text = "$pendingAlertCount potential duplicate ingredient alert(s)",
                        actionLabel = "View →",
                        onClick = onViewDuplicates,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttentionRow(
    icon: ImageVector,
    text: String,
    actionLabel: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.extendedColors.warning,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(onClick = onClick) {
            Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// First-use empty state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardEmptyState(
    patientName: String,
    onAddPrescription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(horizontal = 24.dp),
    ) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MedicalServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = "Welcome, $patientName!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    text = "Add your first prescription to get started. MediHistory AI will organise your medicines and help you prepare for your next doctor visit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                ExtendedFloatingActionButton(
                    text = { Text("Add first prescription") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = onAddPrescription,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Skeleton shimmer loading state
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer_alpha",
    )
    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)

    @Composable
    fun ShimmerBox(
        width: androidx.compose.ui.unit.Dp = 120.dp,
        height: androidx.compose.ui.unit.Dp = 16.dp,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier =
                modifier
                    .width(width)
                    .height(height)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor),
        )
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .semantics { contentDescription = "Loading" },
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Greeting shimmer
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(width = 100.dp, height = 12.dp)
            ShimmerBox(width = 220.dp, height = 28.dp)
            ShimmerBox(width = 160.dp, height = 12.dp)
        }

        // Stats grid shimmer
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerColor),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerColor),
                    )
                }
            }
        }

        // Quick actions shimmer
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) {
                Box(
                    modifier =
                        Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(shimmerColor),
                )
            }
        }

        // Attention card shimmer
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerColor),
        )

        // Recent activity shimmer
        ShimmerBox(width = 140.dp, height = 18.dp)
        repeat(3) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerColor),
            ) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Prepare for Visit card (F12)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Dashboard card that offers to generate a pre-visit summary (F12).
 *
 * Always visible. Shows a loading spinner while the agent is running.
 * Tapping "Prepare for visit" calls the agent and opens the bottom sheet.
 */
@Composable
private fun PrepareForVisitCard(
    isLoading: Boolean,
    hasSummary: Boolean,
    onPrepare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Prepare for your next doctor visit" },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Prepare for your visit",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text =
                        if (hasSummary) {
                            "Your pre-visit summary is ready"
                        } else {
                            "Get a summary of medicines and questions for your doctor"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                FilledTonalButton(
                    onClick = onPrepare,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Prepare for visit"
                        },
                ) {
                    Text(text = if (hasSummary) "View" else "Prepare")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Visit summary bottom sheet content (F12)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Content of the "Prepare for visit" bottom sheet.
 *
 * Sections:
 * 1. Non-dismissible AI disclaimer
 * 2. Current medicines list
 * 3. Open duplicate alerts to discuss
 * 4. Suggested questions
 *
 * Medical safety: all content is derived from confirmed patient data.
 * The disclaimer is always the first visible element.
 */
@Composable
private fun VisitSummarySheetContent(
    uiState: DashboardUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = uiState.visitSummary

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Prepare for your visit",
            style = MaterialTheme.typography.titleLarge,
        )

        // Non-dismissible disclaimer
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "AI-generated summary — always verify with your doctor or pharmacist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp),
            )
        }

        if (summary == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // ── Current medicines ──────────────────────────────────────────
            if (summary.currentMedicines.isNotEmpty()) {
                Text(
                    text = "Current medicines (${summary.currentMedicines.size})",
                    style = MaterialTheme.typography.titleSmall,
                )
                summary.currentMedicines.forEach { med ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("•", style = MaterialTheme.typography.bodyMedium)
                        Column {
                            Text(
                                text =
                                    med.medicineName +
                                        (med.strength?.let { " $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (!med.dosageInstructions.isNullOrBlank()) {
                                Text(
                                    text = med.dosageInstructions,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
            }

            // ── Open alerts to discuss ─────────────────────────────────────
            if (summary.openAlerts.isNotEmpty()) {
                Text(
                    text = "Alerts to discuss (${summary.openAlerts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.extendedColors.warning,
                )
                summary.openAlerts.forEach { alert ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.extendedColors.warning,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Potential duplicate: ${alert.matchingIngredient}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                HorizontalDivider()
            }

            // ── Suggested questions ────────────────────────────────────────
            if (summary.suggestedQuestions.isNotEmpty()) {
                Text(
                    text = "Questions to consider asking",
                    style = MaterialTheme.typography.titleSmall,
                )
                summary.suggestedQuestions.forEach { question ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("❓", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = question.text,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End),
        ) { Text("Close") }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
