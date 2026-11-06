package com.nexora.circle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NexoraColors {
    val SpaceBlack = Color(0xFF000000)
    val DeepNavy = Color(0xFF0A0A0A)
    val Panel = Color(0xFF121212)
    val NebulaViolet = Color(0xFFB0B0B8)
    val NebulaPink = Color(0xFFFF4FB0)
    val PlasmaCyan = Color(0xFF00F0FF)
    val StarWhite = Color(0xFFF2F2F2)
    val Muted = Color(0xFF8A8A8A)
}

private val NexoraDarkScheme = darkColorScheme(
    primary = NexoraColors.PlasmaCyan,
    secondary = NexoraColors.NebulaViolet,
    tertiary = NexoraColors.NebulaPink,
    background = NexoraColors.SpaceBlack,
    surface = NexoraColors.DeepNavy,
    onPrimary = NexoraColors.SpaceBlack,
    onSecondary = NexoraColors.SpaceBlack,
    onBackground = NexoraColors.StarWhite,
    onSurface = NexoraColors.StarWhite,
)

@Composable
fun NexoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NexoraDarkScheme,
        content = content,
    )
}
