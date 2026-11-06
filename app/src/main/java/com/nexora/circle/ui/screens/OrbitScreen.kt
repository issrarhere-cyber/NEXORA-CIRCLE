package com.nexora.circle.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nexora.circle.ui.components.NexoraIcon
import com.nexora.circle.ui.components.NxIcon
import com.nexora.circle.ui.components.PostActions
import com.nexora.circle.ui.theme.NexoraColors

private data class Post(
    val id: String,
    val authorUid: String,
    val name: String,
    val handle: String,
    val text: String,
    val time: Long,
    val likes: List<String>,
    val resignals: List<String>,
    val resignalOf: String,
    val resignalFromName: String,
)

private fun ago(time: Long): String {
    val m = (System.currentTimeMillis() - time) / 60000
    return when {
        m < 1 -> "now"
        m < 60 -> "${m}m"
        m < 1440 -> "${m / 60}h"
        else -> "${m / 1440}d"
    }
}

@Composable
private fun Avatar(photo: ImageBitmap?, initial: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(NexoraColors.PlasmaCyan),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(initial, color = NexoraColors.SpaceBlack, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeaderIcon(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(
                        containerColor = NexoraColors.PlasmaCyan,
                        contentColor = NexoraColors.SpaceBlack,
                    ) {
                        Text(if (count > 99) "99+" else count.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NexoraColors.StarWhite,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun OrbitScreen(
    onOpenComms: () -> Unit,
    onOpenNexoraX: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSignal: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    signalCount: Int = 0,
    beamCount: Int = 0,
    notifCount: Int = 0,
) {
    val context = LocalContext.current
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val myUid = user?.uid.orEmpty()
    val db = remember { FirebaseFirestore.getInstance() }
    val profile = remember { context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE) }
    val name = remember {
        profile.getString("name", "")?.trim().orEmpty()
            .ifBlank { user?.displayName.orEmpty() }
    }
    val handle = remember { profile.getString("username", "")?.trim().orEmpty() }
    val photo = remember {
        profile.getString("photo_path", null)?.let {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(it, opts)?.asImageBitmap()
        }
    }
    val initial = (name.firstOrNull()?.uppercaseChar() ?: '?').toString()

    val posts = remember { mutableStateListOf<Post>() }
    var tab by remember { mutableStateOf(0) }
    var composing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    DisposableEffect(myUid) {
        val reg = db.collection("posts")
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    errorMsg = "Could not load posts: ${err.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snap != null) {
                    errorMsg = ""
                    val list = snap.documents.mapNotNull { d ->
                        val text = d.getString("text") ?: return@mapNotNull null
                        @Suppress("UNCHECKED_CAST")
                        val likes = (d.get("likes") as? List<String>) ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val resignals = (d.get("resignals") as? List<String>) ?: emptyList()
                        Post(
                            id = d.id,
                            authorUid = d.getString("authorUid") ?: "",
                            name = d.getString("authorName") ?: "",
                            handle = d.getString("authorHandle") ?: "",
                            text = text,
                            time = d.getLong("time") ?: 0L,
                            likes = likes,
                            resignals = resignals,
                            resignalOf = d.getString("resignalOf") ?: "",
                            resignalFromName = d.getString("resignalFromName") ?: "",
                        )
                    }
                    posts.clear()
                    posts.addAll(list)
                }
            }
        onDispose { reg.remove() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(modifier = Modifier.clickable { onOpenProfile() }) {
                    Avatar(photo, initial, 36.dp)
                }
                NexoraIcon(NxIcon.Orbit, tint = NexoraColors.PlasmaCyan, iconSize = 32.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeaderIcon(Icons.Outlined.Search, "Search", 0) { onOpenSearch() }
                    HeaderIcon(Icons.Outlined.PersonAdd, "Signal", signalCount) { onOpenSignal() }
                    HeaderIcon(Icons.AutoMirrored.Outlined.Send, "Beam", beamCount) { onOpenComms() }
                    HeaderIcon(Icons.Outlined.Notifications, "Notifications", notifCount) { onOpenNotifications() }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("For you", "Following").forEachIndexed { i, label ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tab = i }
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            label,
                            color = NexoraColors.StarWhite,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(48.dp)
                                .background(
                                    if (tab == i) NexoraColors.PlasmaCyan else Color.Transparent,
                                    RoundedCornerShape(2.dp),
                                )
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NexoraColors.Divider)
            )

            if (errorMsg.isNotEmpty()) {
                Text(
                    errorMsg,
                    color = NexoraColors.NebulaPink,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            if (tab == 1) {
                Text(
                    "Posts from people you follow will appear here. Coming soon.",
                    color = NexoraColors.StarWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            } else if (posts.isEmpty()) {
                Text(
                    "No posts yet. Tap + to share your first signal.",
                    color = NexoraColors.StarWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts, key = { it.id }) { post ->
                        val mine = post.authorUid == myUid
                        val liked = post.likes.contains(myUid)
                        val resignaled = post.resignals.contains(myUid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(
                                if (mine) photo else null,
                                (post.name.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                40.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                if (post.resignalOf.isNotEmpty()) {
                                    Text(
                                        "Resignaled from ${post.resignalFromName.ifBlank { "Explorer" }}",
                                        color = NexoraColors.StarWhite,
                                    )
                                }
                                Text(
                                    post.name.ifBlank { "Explorer" },
                                    color = NexoraColors.StarWhite,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    (if (post.handle.isNotBlank()) "X.${post.handle} · " else "") + ago(post.time),
                                    color = NexoraColors.StarWhite,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(post.text, color = NexoraColors.StarWhite)
