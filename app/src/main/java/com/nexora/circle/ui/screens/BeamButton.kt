package com.nexora.circle.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BeamPink = Color(0xFFFF5FA2)

@Composable
private fun BeamStreaks(width: Dp, height: Dp) {
    Canvas(Modifier.size(width = width, height = height)) {
        val k = size.width / 200f
        fun streak(x1: Float, y1: Float, x2: Float, y2: Float, w: Float) {
            drawLine(
                color = Color.White,
                start = Offset(x1 * k, y1 * k),
                end = Offset(x2 * k, y2 * k),
                strokeWidth = w * k,
                cap = StrokeCap.Round,
            )
        }
        streak(12f, 75f, 95f, 12f, 26f)
        streak(40f, 115f, 182f, 20f, 42f)
        streak(70f, 150f, 188f, 92f, 26f)
    }
}

@Composable
fun BeamButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .height(buttonHeight)
            .clip(RoundedCornerShape(50))
            .background(BeamPink)
            .clickable { onClick() }
            .semantics { contentDescription = "Beam" },
        contentAlignment = Alignment.Center,
    ) {
        BeamStreaks(30.dp, 25.dp)
    }
}

@Composable
fun BeamHeaderIcon(count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable { onClick() }
            .semantics { contentDescription = "Beam" },
        contentAlignment = Alignment.Center,
    ) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(containerColor = Color.White, contentColor = Color.Black) {
                        Text(if (count > 99) "99+" else count.toString())
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BeamPink),
                contentAlignment = Alignment.Center,
            ) {
                BeamStreaks(18.dp, 15.dp)
            }
        }
    }
}
