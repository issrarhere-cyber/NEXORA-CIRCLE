package com.nexora.circle.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.nexora.circle.ui.components.NexoraIcon
import com.nexora.circle.ui.components.NxIcon
import com.nexora.circle.ui.theme.NexoraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
private fun PostCard(
    post: FeedPost,
    myUid: String,
    myPhoto: ImageBitmap?,
    vm: OrbitViewModel,
    onOpenProfile: () -> Unit,
    onOpenUserProfile: (String) -> Unit,
    onComment: () -> Unit,
    onQuote: () -> Unit,
    onAskDelete: () -> Unit,
) {
    val context = LocalContext.current
    val mine = post.authorUid == myUid

    // Counted once per session (see ViewModel), not on every scroll
    LaunchedEffect(post.id) { vm.markViewed(post) }

    val openAuthor: () -> Unit = {
        if (mine) onOpenProfile()
        else if (post.authorUid.isNotBlank()) onOpenUserProfile(post.authorUid)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // FIX: avatar is now tappable
        Box(modifier = Modifier.clickable { openAuthor() }) {
            Avatar(
                if (mine) myPhoto else null,
                (post.name.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                40.dp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (post.resignalOf.isNotEmpty()) {
                Text(
                    "Resignaled from ${post.resignalFromName.ifBlank { "Explorer" }}",
                    color = NexoraColors.StarWhite,
                )
            }
            // FIX: name is now tappable
            Text(
                post.name.ifBlank { "Explorer" },
                color = NexoraColors.StarWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { openAuthor() },
            )
            Text(
                (if (post.handle.isNotBlank()) "X.${post.handle} · " else "") + ago(post.time),
                color = NexoraColors.StarWhite,
            )
            Spacer(Modifier.height(6.dp))
            Text(post.text, color = NexoraColors.StarWhite)
            Spacer(Modifier.height(8.dp))
            PostActionBar(
                liked = post.likes.contains(myUid),
                resignaled = post.resignals.contains(myUid),
                likeCount = post.likes.size,
                resignalCount = post.resignals.size,
                onComment = onComment,
                onLike = { vm.toggleLike(post) },
                onResignal = { vm.toggleResignal(post) },
                onShare = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, post.text)
                    }
                    context.startActivity(
                        Intent.createChooser(send, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                viewCount = post.views,
                saved = post.saved.contains(myUid),
                visibility = post.visibility,
                isOwnPost = mine,
                onQuote = onQuote,
                onReactWithVideo = {
                    Toast.makeText(context, "Video reactions coming soon", Toast.LENGTH_SHORT).show()
                },
                onSave = { vm.toggleSave(post) },
                onVisibilityChange = { v -> vm.setVisibility(post, v) },
                onDelete = onAskDelete,
            )
        }
    }
}

@Composable
private fun ComposerDialog(
    title: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val limit = OrbitViewModel.MAX_POST_LENGTH
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= limit) text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${text.length}/$limit",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { onPost(text); onDismiss() },
            ) { Text("Post") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun OrbitScreen(
    onOpenComms: () -> Unit,
    onOpenNexoraX: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSignal: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},   // NEW
    signalCount: Int = 0,
    beamCount: Int = 0,
    notifCount: Int = 0,
    vm: OrbitViewModel = viewModel(),
) {
    val context = LocalContext.current
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val myUid = user?.uid.orEmpty()
    val prefs = remember { context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE) }
    val name = remember {
        prefs.getString("name", "")?.trim().orEmpty().ifBlank { user?.displayName.orEmpty() }
    }
    val handle = remember { prefs.getString("username", "")?.trim().orEmpty() }

    // Bitmap decoding moved off the main thread
    val photo by produceState<ImageBitmap?>(initialValue = null) {
        val path = prefs.getString("photo_path", null)
        value = if (path == null) null else withContext(Dispatchers.IO) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        }
    }
    val initial = (name.firstOrNull()?.uppercaseChar() ?: '?').toString()

    val ui by vm.state.collectAsState()

    var tab by rememberSaveable { mutableStateOf(0) }
    var composing by rememberSaveable { mutableStateOf(false) }
    var commentsFor by rememberSaveable { mutableStateOf<String?>(null) }
    var quoteFor by remember { mutableStateOf<FeedPost?>(null) }
    var deleteFor by remember { mutableStateOf<FeedPost?>(null) }

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
                    BeamHeaderIcon(beamCount) { onOpenComms() }
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

            when {
                tab == 1 -> Text(
                    "Posts from people you follow will appear here. Coming soon.",
                    color = NexoraColors.StarWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )

                ui.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = NexoraColors.PlasmaCyan) }

                ui.error != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(ui.error ?: "", color = NexoraColors.NebulaPink, textAlign = TextAlign.Center)
                    Button(onClick = { vm.startListening() }) { Text("Retry") }
                }

                ui.posts.isEmpty() -> Text(
                    "No posts yet. Tap + to share your first signal.",
                    color = NexoraColors.StarWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // FIX: last post's buttons are no longer hidden behind the + button
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(ui.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            myUid = myUid,
                            myPhoto = photo,
                            vm = vm,
                            onOpenProfile = onOpenProfile,
                            onOpenUserProfile = onOpenUserProfile,
                            onComment = { commentsFor = post.id },
                            onQuote = { quoteFor = post },
                            onAskDelete = { deleteFor = post },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { composing = true },
            containerColor = NexoraColors.PlasmaCyan,
            contentColor = NexoraColors.SpaceBlack,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New signal")
        }

        commentsFor?.let { CommentsDialog(it, myUid, name) { commentsFor = null } }

        quoteFor?.let { q ->
            ComposerDialog(
                title = "Quote",
                onDismiss = { quoteFor = null },
                onPost = { vm.createPost(it, name, handle, quoting = q) },
            )
        }

        if (composing) {
            ComposerDialog(
                title = "New signal",
                onDismiss = { composing = false },
                onPost = { vm.createPost(it, name, handle) },
            )
        }

        deleteFor?.let { p ->
            AlertDialog(
                onDismissRequest = { deleteFor = null },
                title = { Text("Delete post?") },
                text = { Text("This can't be undone.") },
                confirmButton = {
                    TextButton(onClick = { vm.delete(p); deleteFor = null }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Cancel") } },
            )
        }
    }
}
