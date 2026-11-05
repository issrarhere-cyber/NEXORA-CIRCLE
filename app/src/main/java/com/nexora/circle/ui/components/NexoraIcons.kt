package com.nexora.circle.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NxIcon { Home, Search, Orbit, Bell, Chat, Plus, Heart }

@Composable
fun NexoraIcon(
    icon: NxIcon,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    iconSize: Dp = 24.dp,
    filled: Boolean = false,
) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val sw = w * 0.085f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (icon) {
            NxIcon.Home -> {
                val p = Path().apply {
                    moveTo(0.12f * w, 0.48f * w)
                    lineTo(0.5f * w, 0.14f * w)
                    lineTo(0.88f * w, 0.48f * w)
                    moveTo(0.22f * w, 0.42f * w)
                    lineTo(0.22f * w, 0.86f * w)
                    lineTo(0.78f * w, 0.86f * w)
                    lineTo(0.78f * w, 0.42f * w)
                }
                drawPath(p, tint, style = stroke)
            }

            NxIcon.Search -> {
                drawCircle(
                    color = tint,
                    radius = 0.28f * w,
                    center = Offset(0.44f * w, 0.44f * w),
                    style = stroke,
                )
                drawLine(
                    color = tint,
                    start = Offset(0.66f * w, 0.66f * w),
                    end = Offset(0.88f * w, 0.88f * w),
                    strokeWidth = sw,
                    cap = StrokeCap.Round,
                )
            }

            NxIcon.Orbit -> {
                drawCircle(color = tint, radius = 0.15f * w, center = Offset(w / 2, w / 2))
                rotate(-25f, Offset(w / 2, w / 2)) {
                    drawOval(
                        color = tint,
                        topLeft = Offset(0.06f * w, 0.33f * w),
                        size = Size(0.88f * w, 0.34f * w),
                        style = stroke,
                    )
                }
            }

            NxIcon.Bell -> {
                val p = Path().apply {
                    moveTo(0.2f * w, 0.74f * w)
                    lineTo(0.28f * w, 0.62f * w)
                    lineTo(0.28f * w, 0.44f * w)
                    cubicTo(0.28f * w, 0.24f * w, 0.72f * w, 0.24f * w, 0.72f * w, 0.44f * w)
                    lineTo(0.72f * w, 0.62f * w)
                    lineTo(0.8f * w, 0.74f * w)
                    close()
                }
                drawPath(p, tint, style = stroke)
                drawCircle(color = tint, radius = 0.06f * w, center = Offset(0.5f * w, 0.86f * w))
            }

            NxIcon.Chat -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.12f * w, 0.16f * w),
                    size = Size(0.76f * w, 0.56f * w),
                    cornerRadius = CornerRadius(0.14f * w),
                    style = stroke,
                )
                val tail = Path().apply {
                    moveTo(0.34f * w, 0.72f * w)
                    lineTo(0.3f * w, 0.88f * w)
                    lineTo(0.5f * w, 0.72f * w)
                }
                drawPath(tail, tint, style = stroke)
            }

            NxIcon.Plus -> {
                drawLine(tint, Offset(0.5f * w, 0.2f * w), Offset(0.5f * w, 0.8f * w), sw, StrokeCap.Round)
                drawLine(tint, Offset(0.2f * w, 0.5f * w), Offset(0.8f * w, 0.5f * w), sw, StrokeCap.Round)
            }

            NxIcon.Heart -> {
                val p = Path().apply {
                    moveTo(0.5f * w, 0.84f * w)
                    cubicTo(0.1f * w, 0.56f * w, 0.14f * w, 0.2f * w, 0.34f * w, 0.2f * w)
                    cubicTo(0.44f * w, 0.2f * w, 0.5f * w, 0.28f * w, 0.5f * w, 0.32f * w)
                    cubicTo(0.5f * w, 0.28f * w, 0.56f * w, 0.2f * w, 0.66f * w, 0.2f * w)
                    cubicTo(0.86f * w, 0.2f * w, 0.9f * w, 0.56f * w, 0.5f * w, 0.84f * w)
                    close()
                }
                drawPath(p, tint, style = if (filled) Fill else stroke)
            }
        }
    }
}
