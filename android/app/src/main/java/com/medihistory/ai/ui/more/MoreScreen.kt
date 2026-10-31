package com.medihistory.ai.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.medihistory.ai.ui.navigation.Routes

/**
 * "More" disclosure hub — bottom nav tab 5.
 *
 * Provides navigation to secondary destinations that do not fit in the main bottom nav:
 * Safety Center, New Doctor Mode, Reminders, and Privacy & Data.
 *
 * Accessibility: each row has a merged content description for TalkBack.
 *
 * @param onNavigate  Called with the target route string when a row is tapped.
 * @param modifier    Modifier applied to the Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit,
    patientId: String = "current-patient",
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(text = "More") })
        },
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            item {
                MoreRow(
                    icon = Icons.Outlined.AccountCircle,
                    title = "My Profile",
                    subtitle = "View and edit your profile",
                    onClick = { onNavigate(Routes.PATIENT_PROFILE) },
                )
            }
            item {
                MoreRow(
                    icon = Icons.Outlined.MedicalServices,
                    title = "My Doctors",
                    subtitle = "Manage your doctors",
                    onClick = { onNavigate(Routes.DOCTOR_LIST) },
                )
            }
            item {
                MoreRow(
                    icon = Icons.Outlined.Shield,
                    title = "Safety Center",
                    subtitle = "Interaction check & duplicate alerts",
                    onClick = { onNavigate(Routes.safetyCenter(patientId)) },
                )
            }
            item {
                MoreRow(
                    icon = Icons.Outlined.PersonAdd,
                    title = "New Doctor Mode",
                    subtitle = "Prepare & share a summary",
                    onClick = { onNavigate(Routes.NEW_DOCTOR_MODE) },
                )
            }
            item {
                MoreRow(
                    icon = Icons.Outlined.Alarm,
                    title = "Reminders",
                    subtitle = "Manage medication reminders",
                    onClick = { onNavigate(Routes.REMINDERS) },
                )
            }
            item {
                MoreRow(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy & Data",
                    subtitle = "Export, delete, backup settings",
                    onClick = { onNavigate(Routes.PRIVACY) },
                )
            }
        }
    }
}

/**
 * A single navigable row in the More screen.
 *
 * @param icon        Leading icon.
 * @param title       Primary label.
 * @param subtitle    Secondary descriptive text.
 * @param onClick     Called when the row is tapped.
 * @param modifier    Modifier applied to the card.
 */
@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier =
            modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$title. $subtitle"
                },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorative; full description on card
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Navigate to $title",
            )
        }
    }
}
