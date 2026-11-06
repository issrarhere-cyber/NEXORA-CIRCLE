package com.nexora.circle.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.theme.NexoraColors
import kotlin.math.cos
import kotlin.math.sin

private fun DrawScope.heart(color: Color, filled: Boolean) {
    val w = size.width
    val h = size.height
    val p = Path().apply {
        moveTo(w * 0.5f, h * 0.88f)
        cubicTo(w * 0.0f, h * 0.5f, w * 0.1f, h * 0.12f, w * 0.3f, h * 0.14f)
        cubicTo(w * 0.42f, h * 0.15f, w * 0.5f, h * 0.25f, w * 0.5f, h * 0.3f)
        cubicTo(w * 0.5f, h * 0.25f, w * 0.58f, h * 0.15f, w * 0.7f, h * 0.14f)
        cubicTo(w * 0.9f, h * 0.12f, w * 1.0f, h * 0.5f, w * 0.5f, h * 0.88f)
        close()
    }
    if (filled) drawPath(p, color, style = Fill)
    drawPath(p, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.comment(color: Color) {
    val w = size.width
    val h = size.height
    val s
