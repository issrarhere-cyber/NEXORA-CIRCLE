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
    val path = Path()
    path.moveTo(w * 0.5f, h * 0.88f)
    path.cubicTo(w * 0.0f, h * 0.5f, w * 0.1f, h * 0.12f, w * 0.3f, h * 0.14f)
    path.cubicTo(w * 0.42f, h * 0.15f, w * 0.5f, h * 0.25f, w * 0.5f, h * 0.3f)
    path.cubicTo(w * 0.5f, h * 0.25f, w * 0.58f, h * 0.15f, w * 0.7f, h * 0.14f)
    path.cubicTo(w * 0.9f, h * 0.12f, w * 1.0f, h * 0.5f, w * 0.5f, h * 0.88f)
    path.close()
    if (filled) drawPath(path, color, style = Fill)
    drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.comment(color: Color) {
    val w = size.width
    val h = size.height
    val st = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    drawOval(color, Offset(w * 0.06f, h * 0.1f), Size(w * 0.88f, h * 0.65f), style = st)
    val tail = Path()
    tail.moveTo(w * 0.26f, h * 0.68f)
    tail.lineTo(w * 0.14f, h * 0.9f)
    tail.lineTo(w * 0.42f, h * 0.74f)
    drawPath(tail, color, style = st)
}

private fun DrawScope.resignal(color: Color) {
    val w = size.width
    val h = size.height
    val st = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    val r = w * 0.36f
    val cx = w / 2
    val cy = h / 2
    val tl = Offset(cx - r, cy - r)
    val sz = Size(r * 2, r * 2)
    drawArc(color, 190f, 150f, false, tl, sz, style = st)
    drawArc(color, 10f, 150f, false, tl, sz, style = st)
    val rad = Math.PI / 180.0
    val e1 = Offset(cx + r * cos(340 * rad).toFloat(), cy + r * sin(340 * rad).toFloat())
    val e2 = Offset(cx + r * cos(160 * rad).toFloat(), cy + r * sin(160 * rad).toFloat())
    val a = w * 0.16f
    drawLine(color, e1, Offset(e1.x - a, e1.y), st.width, StrokeCap.Round)
    drawLine(color, e1, Offset(e1.x, e1.y + a), st.width, StrokeCap.Round)
    drawLine(color, e2, Offset(e2.x + a, e2.y), st.width, StrokeCap.Round)
    drawLine(color, e2, Offset(e2.x, e2.y - a), st.width, StrokeCap.Round)
}

private fun DrawScope.share(color: Color) {
    val w = size.width
    val h = size.height
    val st = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    val r = w * 0.12f
    val a = Offset(w * 0.8f, h * 0.2f)
    val b = Offset(w * 0.2f, h * 0.5f)
    val c = Offset(w * 0.8f, h * 0.8f)
    drawLine(color, Offset(b.x + r, b.y - r * 0.5f), Offset(a.x - r, a.y + r * 0.6f), st.width)
    drawLine(color, Offset(b.x + r, b.y + r * 0.5f), Offset(c.x - r, c.y - r * 0.6f), st.width)
    drawCircle(color, r, a, style = st)
    drawCircle(color, r, b, style = st)
    drawCircle(color, r, c, style = st)
}

@Composable
fun PostActions(
    liked: Boolean,
    likeCount: Int,
    resignaled: Boolean,
    resignalCount: Int,
    mine: Boolean,
    shareText: String,
    onLike: () -> Unit,
    onResignal: () -> Unit,
    onDelete: () -> Unit,
    onComment: () -> Unit = {},
) {
    val context = LocalContext.current
    val muted = NexoraColors.Muted
    val likeColor = if (liked) NexoraColors.NebulaPink else muted
    val resignalColor = if (resignaled) NexoraColors.PlasmaCyan else muted

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clickable { onLike() },
        ) {
            Canvas(Modifier.size(22.dp)) { heart(likeColor, liked) }
            if (likeCount > 0) Text("$likeCount", color = muted)
        }
        Canvas(Modifier.size(22.dp).clickable { onComment() }) { comment(muted) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.clickable { onResignal() },
        ) {
            Canvas(Modifier.size(22.dp)) { resignal(resignalColor) }
            if (resignalCount > 0) Text("$resignalCount", color = resignalColor)
        }
        Canvas(
            Modifier.size(22.dp).clickable {
                val send = Intent(Intent.ACTION_SEND)
                send.type = "text/plain"
                send.putExtra(Intent.EXTRA_TEXT, shareText)
                context.startActivity(Intent.createChooser(send, "Share"))
            }
        ) { share(muted) }
        if (mine) {
            Text("Delete", color = muted, modifier = Modifier.clickable { onDelete() })
        }
    }
}
