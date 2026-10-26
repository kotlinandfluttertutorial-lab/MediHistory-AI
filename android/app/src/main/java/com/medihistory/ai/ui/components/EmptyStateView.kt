package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Full-area empty-state view with a decorative icon, a message, and a primary action button.
 *
 * The icon is decorative (contentDescription = null) because the [message] text conveys the
 * meaning to screen-reader users. The action button uses [PrimaryButton] which already
 * enforces the 48 dp minimum touch target.
 *
 * @param message     Human-readable explanation of why the list is empty.
 * @param actionLabel Label for the primary action button.
 * @param onAction    Called when the action button is tapped.
 * @param modifier    Modifier applied to the outer [Column].
 * @param icon        Decorative icon. Defaults to [Icons.Outlined.Inbox].
 */
@Composable
fun EmptyStateView(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 32.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative — message text conveys context
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(
            text = actionLabel,
            onClick = onAction,
        )
    }
}
