package com.medihistory.ai.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Centred [CircularProgressIndicator] with a semantic content description for accessibility.
 *
 * @param modifier           Modifier applied to the outer [Box]. Defaults to [Modifier.fillMaxSize].
 * @param contentDescription Accessible description announced by TalkBack. Defaults to "Loading".
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier.fillMaxSize(),
    contentDescription: String = "Loading",
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
    ) {
        CircularProgressIndicator()
    }
}
