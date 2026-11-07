package com.nexora.circle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.circle.ui.theme.NexoraColors

private val NexoraCyan = Color(0xFF00E5FF)
private val PanelBlack = Color(0xFF0A0E11)
private val DeleteRed = Color(0xFFFF4D5E)

@Composable
private fun ActionItem(icon: ImageVector, count: Int?, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        if (count != null) {
            Spacer(Modifier.width(4.dp))
            Text(count.toString(), color = tint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = NexoraCyan, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, color = Color.White, fontSize = 17.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    // New (all optional, so old callers still compile)
    viewCount: Int = 0,
    saved: Boolean = false,
    visibility: String = "Public",
    isOwnPost: Boolean = true,
    onQuote: () -> Unit = {},
    onReactWithVideo: () -> Unit = {},
    onSave: () -> Unit = {},
    onVisibilityChange: (String) -> Unit = {},
    onDelete: () -> Unit = {},
) {
    val grey = NexoraColors.StarWhite.copy(alpha = 0.7f)
    var showSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ActionItem(Icons.Outlined.ChatBubbleOutline, null, grey, onComment)
        ActionItem(
            Icons.Outlined.Repeat,
            resignalCount,
            if (resignaled) Color(0xFF00BA7C) else grey,
        ) { showSheet = true }
        ActionItem(
            if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            likeCount,
            if (liked) Color(0xFFF91880) else grey,
            onLike,
        )
        ActionItem(Icons.Outlined.BarChart, viewCount, grey) {}
        ActionItem(
            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            null,
            if (saved) NexoraCyan else grey,
            onSave,
        )
        ActionItem(Icons.Outlined.Share, null, grey, onShare)

        Box {
            ActionItem(Icons.Outlined.MoreHoriz, null, grey) { showMenu = true }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .background(PanelBlack)
                    .border(1.dp, NexoraCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            ) {
                if (isOwnPost) {
                    listOf(
                        "Public" to Icons.Outlined.Language,
                        "Only Match Signaled" to Icons.Outlined.GpsFixed,
                        "Only Me" to Icons.Outlined.Lock,
                    ).forEach { (label, icon) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color.White) },
                            leadingIcon = { Icon(icon, null, tint = NexoraCyan) },
                            trailingIcon = {
                                if (visibility == label) {
                                    Icon(Icons.Filled.Check, null, tint = NexoraCyan)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onVisibilityChange(label)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = DeleteRed) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = DeleteRed) },
                        onClick = {
                            showMenu = false
                            showDelete = true
                        },
                    )
                }
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = PanelBlack,
        ) {
            Box(Modifier.navigationBarsPadding()) {
                androidx.compose.foundation.layout.Column {
                    SheetRow(Icons.Outlined.Repeat, "Resignaled") {
                        showSheet = false
                        onResignal()
                    }
                    SheetRow(Icons.Outlined.Edit, "Quote") {
                        showSheet = false
                        onQuote()
                    }
                    SheetRow(Icons.Outlined.Videocam, "React with Video") {
                        showSheet = false
                        onReactWithVideo()
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            containerColor = PanelBlack,
            text = {
                Text("Are you sure you want to delete this post?", color = Color.White)
            },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    onDelete()
                }) { Text("Delete", color = DeleteRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel", color = NexoraCyan)
                }
            },
        )
    }
}
