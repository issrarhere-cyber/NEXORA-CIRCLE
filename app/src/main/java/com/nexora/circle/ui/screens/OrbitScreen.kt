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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.components.NexoraIcon
import com.nexora.circle.ui.components.NxIcon
import com.nexora.circle.ui.theme.NexoraColors
import org.json.JSONArray
import org.json.JSONObject

private const val ORBIT_PREFS = "nexora_orbit"
private const val KEY_POSTS = "posts"

private data class Post(
    val id: Long,
    val text: String,
    val time: Long,
    val liked: Boolean,
)

private fun loadPosts(context: Context): List<Post> {
    val raw = context.getSharedPreferences(ORBIT_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_POSTS, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            Post(
                id = o.optLong("id"),
                text = o.optString("text"),
                time = o.optLong("time"),
                liked = o.optBoolean("liked"),
            )
        }.filter { it.text.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun savePosts(context: Context, posts: List<Post>) {
    val arr = JSONArray()
    posts.forEach {
        arr.put(
            JSONObject()
                .put("id", it.id)
                .put("text", it.text)
                .put("time", it.time)
                .put("liked", it.liked)
        )
    }
    context.getSharedPreferences(ORBIT_PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_POSTS, arr.toString()).apply()
}

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
fun OrbitScreen(
    onOpenComms: () -> Unit,
    onOpenNexoraX: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val context = LocalContext.current
    val profile = remember { context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE) }
    val name = remember { profile.getString("name", "")?.trim().orEmpty() }
    val handle = remember { profile.getString("username", "")?.trim().orEmpty() }
    val photo = remember {
        profile.getString("photo_path", null)?.let {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(it, opts)?.asImageBitmap()
        }
    }
    val initial = (name.firstOrNull()?.uppercaseChar() ?: '?').toString()

    val posts = remember { mutableStateListOf<Post>().apply { addAll(loadPosts(context)) } }
    var tab by remember { mutableStateOf(0) }
    var composing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar: profile photo | logo | chat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(modifier = Modifier.clickable { onOpenProfile() }) {
                    Avatar(photo, initial, 36.dp)
                }
                NexoraIcon(NxIcon.Orbit, tint = NexoraColors.PlasmaCyan, iconSize = 32.dp)
                Box(
                    modifier = Modifier.size(36.dp).clickable { onOpenComms() },
                    contentAlignment = Alignment.Center,
                ) {
                    NexoraIcon(NxIcon.Chat, tint = NexoraColors.StarWhite, iconSize = 26.dp)
                }
            }

            // Tabs
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
                            color = if (tab == i) NexoraColors.StarWhite else NexoraColors.Muted,
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
                    .background(NexoraColors.Muted.copy(alpha = 0.25f))
            )

            // Feed
            if (tab == 1) {
                Text(
                    "Posts from people you follow will appear here. This needs online accounts, coming soon.",
                    color = NexoraColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            } else if (posts.isEmpty()) {
                Text(
                    "No posts yet. Tap + to share your first signal.",
                    color = NexoraColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts, key = { it.id }) { post ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Avatar(photo, initial, 40.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    name.ifBlank { "Explorer" },
                                    color = NexoraColors.StarWhite,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    (if (handle.isNotBlank()) "X.$handle · " else "") + ago(post.time),
                                    color = NexoraColors.Muted,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(post.text, color = NexoraColors.StarWhite)
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                                ) {
                                    Box(
                                        modifier = Modifier.clickable {
                                            val i = posts.indexOfFirst { it.id == post.id }
                                            if (i >= 0) {
                                                posts[i] = posts[i].copy(liked = !posts[i].liked)
                                                savePosts(context, posts)
                                            }
                                        }
                                    ) {
                                        NexoraIcon(
                                            NxIcon.Heart,
                                            tint = if (post.liked) NexoraColors.NebulaPink else NexoraColors.Muted,
                                            iconSize = 22.dp,
                                            filled = post.liked,
                                        )
                                    }
                                    Text(
                                        "Delete",
                                        color = NexoraColors.Muted,
                                        modifier = Modifier.clickable {
                                            posts.removeAll { it.id == post.id }
                                            savePosts(context, posts)
                                        },
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(NexoraColors.Muted.copy(alpha = 0.15f))
                        )
                    }
                }
            }
        }

        // Floating + button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(NexoraColors.PlasmaCyan)
                .clickable {
                    draft = ""
                    composing = true
                },
            contentAlignment = Alignment.Center,
        ) {
            NexoraIcon(NxIcon.Plus, tint = NexoraColors.SpaceBlack, iconSize = 28.dp)
        }
    }

    if (composing) {
        AlertDialog(
            onDismissRequest = { composing = false },
            title = { Text("New post") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(280) },
                    label = { Text("What's happening in orbit?") },
                    textStyle = TextStyle(color = NexoraColors.StarWhite),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        val now = System.currentTimeMillis()
                        posts.add(0, Post(now, draft.trim(), now, false))
                        savePosts(context, posts)
                        composing = false
                    },
                ) { Text("Post") }
            },
            dismissButton = {
                TextButton(onClick = { composing = false }) { Text("Cancel") }
            },
        )
    }
}
