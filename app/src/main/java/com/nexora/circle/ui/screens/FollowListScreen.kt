package com.nexora.circle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.theme.NexoraColors

private data class ListUserInfo(val name: String, val username: String)

private data class MyPostRow(
    val id: String,
    val text: String,
    val time: Long,
    val likes: Int,
)

private fun listAgo(time: Long): String {
    val m = (System.currentTimeMillis() - time) / 60000
    return when {
        m < 1 -> "now"
        m < 60 -> "${m}m"
        m < 1440 -> "${m / 60}h"
        else -> "${m / 1440}d"
    }
}

@Composable
private fun ListHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text("Back", color = NexoraColors.PlasmaCyan, fontSize = 16.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = NexoraColors.StarWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Shows the people in users/{ownerUid}/{collection}.
 * Use collection = "followers" for Followers, "following" for Following.
 */
@Composable
fun UserListScreen(
    title: String,
    ownerUid: String,
    collection: String,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var ids by remember { mutableStateOf<List<String>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val infos = remember { mutableStateMapOf<String, ListUserInfo>() }

    DisposableEffect(ownerUid, collection) {
        if (ownerUid.isBlank()) {
            loaded = true
            onDispose { }
        } else {
            val reg = db.collection("users").document(ownerUid).collection(collection)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        error = "Could not load this list."
                        loaded = true
                        return@addSnapshotListener
                    }
                    if (snap != null) {
                        error = ""
                        ids = snap.documents
                            .sortedByDescending { it.getLong("time") ?: 0L }
                            .map { it.id }
                        loaded = true
                    }
                }
            onDispose { reg.remove() }
        }
    }

    // Load the name and username of every person in the list (once each)
    LaunchedEffect(ids) {
        ids.filter { !infos.containsKey(it) }.forEach { id ->
            db.collection("users").document(id).get().addOnSuccessListener { s ->
                infos[id] = ListUserInfo(
                    name = s.getString("name") ?: "",
                    username = s.getString("username") ?: "",
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ListHeader(title, onBack)

        if (error.isNotEmpty()) {
            Text(
                error,
                color = NexoraColors.NebulaPink,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        } else if (loaded && ids.isEmpty()) {
            Text(
                if (collection == "followers") "No followers yet." else "You are not following anyone yet.",
                color = NexoraColors.StarWhite,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(ids, key = { it }) { id ->
                    val info = infos[id]
                    val shown = (info?.name ?: "").ifBlank { "Explorer" }
                    val handle = info?.username ?: ""
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserClick(id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NexoraColors.PlasmaCyan),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (shown.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                color = NexoraColors.SpaceBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                shown,
                                color = NexoraColors.StarWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (handle.isNotBlank()) {
                                Text(
                                    "X.$handle",
                                    color = NexoraColors.PlasmaCyan,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shows the signed-in user's own posts (newest first). */
@Composable
fun MyPostsScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    var rows by remember { mutableStateOf<List<MyPostRow>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(myUid) {
        if (myUid.isBlank()) {
            loaded = true
            onDispose { }
        } else {
            val reg = db.collection("posts").whereEqualTo("authorUid", myUid)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        error = "Could not load your posts."
                        loaded = true
                        return@addSnapshotListener
                    }
                    if (snap != null) {
                        error = ""
                        rows = snap.documents.mapNotNull { d ->
                            val text = d.getString("text") ?: return@mapNotNull null
                            @Suppress("UNCHECKED_CAST")
                            val likes = (d.get("likes") as? List<String>) ?: emptyList()
                            MyPostRow(
                                id = d.id,
                                text = text,
                                time = d.getLong("time") ?: 0L,
                                likes = likes.size,
                            )
                        }.sortedByDescending { it.time }
                        loaded = true
                    }
                }
            onDispose { reg.remove() }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ListHeader("Posts", onBack)

        if (error.isNotEmpty()) {
            Text(
                error,
                color = NexoraColors.NebulaPink,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        } else if (loaded && rows.isEmpty()) {
            Text(
                "No posts yet. Go to Orbit and tap + to share your first signal.",
                color = NexoraColors.StarWhite,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rows, key = { it.id }) { post ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            post.text,
                            color = NexoraColors.StarWhite,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            listAgo(post.time) + " · " + post.likes + " likes",
                            color = NexoraColors.StarWhite.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
}
