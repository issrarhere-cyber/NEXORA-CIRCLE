package com.nexora.circle.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.nexora.circle.ui.theme.NexoraColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
)

/**
 * Animated galaxy backdrop: deep-space gradient, two drifting nebula glows
 * and a field of twinkling stars. Wrap any screen content with it.
 */
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val stars = remember {
        val rnd = Random(2026)
        List(140) {
            Star(
                x = rnd.nextFloat(),
                y = rnd.nextFloat(),
                radius = 0.6f + rnd.nextFloat() * 1.8f,
                phase = rnd.nextFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "space")
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(NexoraColors.SpaceBlack, NexoraColors.DeepNavy, NexoraColors.SpaceBlack),
                ),
            )

            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NexoraColors.NebulaViolet.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(w * (0.25f + 0.1f * drift), h * 0.22f),
                    radius = w * 0.75f,
                ),
                radius = w * 0.75f,
                center = Offset(w * (0.25f + 0.1f * drift), h * 0.22f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NexoraColors.NebulaPink.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(w * (0.8f - 0.1f * drift), h * 0.7f),
                    radius = w * 0.7f,
                ),
                radius = w * 0.7f,
                center = Offset(w * (0.8f - 0.1f * drift), h * 0.7f),
            )

            stars.forEach { s ->
                val alpha = 0.35f + 0.65f * (0.5f + 0.5f * sin(2.0 * PI * (twinkle + s.phase))).toFloat()
                drawCircle(
                    color = NexoraColors.StarWhite.copy(alpha = alpha),
                    radius = s.radius,
                    center = Offset(s.x * w, s.y * h),
                )
            }
        }
        content()
    }
}
