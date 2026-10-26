package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Secondary outlined action button wrapping Material 3 [OutlinedButton].
 *
 * Enforces a minimum height of 48 dp to satisfy the WCAG 2.5.5 touch-target requirement.
 *
 * @param text     Button label text.
 * @param onClick  Called when the button is tapped.
 * @param modifier Modifier applied to the button.
 * @param enabled  Whether the button is interactive. Defaults to true.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
    ) {
        Text(text = text)
    }
}
