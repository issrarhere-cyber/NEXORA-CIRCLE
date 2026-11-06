package com.nexora.circle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexora.circle.ui.theme.NexoraColors

/**
 * Full dark backdrop: plain pure black (AMOLED).
 * Kept the same name and parameters so every screen keeps working.
 */
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexoraColors.SpaceBlack),
    ) {
        content()
    }
}
