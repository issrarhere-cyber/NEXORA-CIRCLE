package com.nexora.circle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NexoraColors {
    val SpaceBlack = Color(0xFF05060F)
    val DeepNavy = Color(0xFF0B0D24)
    val Panel = Color(0xCC12163A)
    val NebulaViolet = Color(0xFF7C5CFF)
    val NebulaPink = Color(0xFFFF5CA8)
    val PlasmaCyan = Color(0xFF2DE2E6)
    val StarWhite = Color(0xFFEAF0FF)
    val Muted = Color(0xFF8E96C7)
}

private val NexoraDarkScheme = darkColorScheme(
    primary = NexoraColors.NebulaViolet,
    secondary = NexoraColors.PlasmaCyan,
    tertiary = NexoraColors.NebulaPink,
    background = NexoraColors.SpaceBlack,
    surface = NexoraColors.DeepNavy,
    onPrimary = Color.White,
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
