package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Thin wrapper around [ElevatedCard] providing consistent elevation and click handling
 * across MediHistory AI screens.
 *
 * @param onClick   Called when the card is tapped.
 * @param modifier  Modifier applied to the card.
 * @param elevation Card elevation. Defaults to 1 dp to keep the surface hierarchy subtle.
 * @param content   Column-scoped content inside the card.
 */
@Composable
fun MediCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
    ) {
        Column(content = content)
    }
}
