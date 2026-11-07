package com.nexora.circle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.theme.NexoraColors

@Composable
private fun ActionItem(icon: ImageVector, count: Int?, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(count.toString(), color = tint)
        }
    }
}

@Composable
fun PostActionBar(
    liked: Boolean,
    resignaled: Boolean,
    likeCount: Int,
    resignalCount: Int,
    onComment: () -> Unit,
    onLike: () -> Unit,
    onResignal: () -> Unit,
    onShare: () -> Unit,
) {
    val grey = NexoraColors.StarWhite.copy(alpha = 0.7f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ActionItem(Icons.Outlined.ChatBubbleOutline, null, grey, onComment)
        ActionItem(
            Icons.Outlined.Repeat,
            resignalCount,
            if (resignaled) Color(0xFF00BA7C) else grey,
            onResignal,
        )
        ActionItem(
            if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            likeCount,
            if (liked) Color(0xFFF91880) else grey,
            onLike,
        )
        ActionItem(Icons.Outlined.Share, null, grey, onShare)
    }
}
