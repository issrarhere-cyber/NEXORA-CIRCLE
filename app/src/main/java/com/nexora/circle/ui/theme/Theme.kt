package com.nexora.circle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object NexoraColors {
    val SpaceBlack = Color(0xFF020208)
    val DeepNavy = Color(0xFF070818)
    val Panel = Color(0xCC0B0D22)
    val NebulaViolet = Color(0xFF8B6BFF)
    val NebulaPink = Color(0xFFFF4FB0)
    val PlasmaCyan = Color(0xFF00F0FF)
    val StarWhite = Color(0xFFF2F6FF)
    val Muted = Color(0xFF9AA3D6)
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
