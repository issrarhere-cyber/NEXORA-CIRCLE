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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.theme.NexoraColors

private data class MyPost(
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
    val resignalFromUid: String,
    val resignalFromHandle: String,
    val resignalFromTime: Long,
    val quoteText: String,
    val originalText: String,
    val views: Int,
    val saved: List<String>,
    val visibility: String,
)

/** Original post details, loaded once for re-signal entries. */
private data class OrigInfo(
    val uid: String,
    val name: String,
    val handle: String,
    val time: Long,
    val text: String,
    val resignals: List<String>,
)

@Composable
private fun MyPostAvatar(photo: ImageBitmap?, initial: String, size: Dp) {
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
            Text(
                initial,
                color = NexoraColors.SpaceBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

/**
 * Orbit History screen, same card style as the Orbit feed.
 * uid = "" means my own history. Pass another user's uid to show their public signals.
 */
@Composable
fun MyPostsFeedScreen(
    onBack: () -> Unit,
    uid: String = "",
    onOpenUserProfile: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val targetUid = if (uid.isBlank()) myUid else uid
    val isMe = targetUid == myUid

    val profile = remember { context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE) }
    val myName = remember {
        profile.getString("name", "")?.trim().orEmpty()
            .ifBlank { FirebaseAuth.getInstance().currentUser?.displayName.orEmpty() }
    }
    val myHandle = remember { profile.getString("username", "")?.trim().orEmpty() }
    val myPhoto = remember {
        profile.getString("photo_path", null)?.let {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(it, opts)?.asImageBitmap()
        }
    }

    val posts = remember { mutableStateListOf<MyPost>() }
    val originals = remember { mutableStateMapOf<String, OrigInfo>() }
    var errorMsg by remember { mutableStateOf("") }
    var commentsFor by remember { mutableStateOf<String?>(null) }
    var quoteSource by remember { mutableStateOf<ResignalSource?>(null) }
    var quoteText by remember { mutableStateOf("") }

    DisposableEffect(targetUid) {
        if (targetUid.isBlank()) {
            onDispose { }
        } else {
            val reg = db.collection("posts")
                .whereEqualTo("authorUid", targetUid)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        errorMsg = "Could not load history: ${err.localizedMessage}"
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
                            @Suppress("UNCHECKED_CAST")
                            val saved = (d.get("saved") as? List<String>) ?: emptyList()
                            MyPost(
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
                                resignalFromUid = d.getString("resignalFromUid") ?: "",
                                resignalFromHandle = d.getString("resignalFromHandle") ?: "",
                                resignalFromTime = d.getLong("resignalFromTime") ?: 0L,
                                quoteText = d.getString("quoteText") ?: "",
                                originalText = d.getString("originalText") ?: "",
                                views = (d.getLong("views") ?: 0L).toInt(),
                                saved = saved,
                                visibility = d.getString("visibility") ?: "Public",
                            )
                        }
                            .filter { isMe || it.visibility == "Public" }
                            .sortedByDescending { it.time }
                        posts.clear()
                        posts.addAll(list)

                        // Load the original post once for every re-signal entry
                        list.forEach { p ->
                            if (p.resignalOf.isNotEmpty() && !originals.containsKey(p.resignalOf)) {
                                db.collection("posts").document(p.resignalOf).get()
                                    .addOnSuccessListener { od ->
                                        val t = od.getString("text")
                                        if (od.exists() && t != null) {
                                            @Suppress("UNCHECKED_CAST")
                                            val rs = (od.get("resignals") as? List<String>) ?: emptyList()
                                            originals[p.resignalOf] = OrigInfo(
                                                uid = od.getString("authorUid") ?: "",
                                                name = od.getString("authorName") ?: "",
                                                handle = od.getString("authorHandle") ?: "",
                                                time = od.getLong("time") ?: 0L,
                                                text = t,
                                                resignals = rs,
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }
            onDispose { reg.remove() }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NexoraColors.SpaceBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header: back arrow, title, signal count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NexoraColors.StarWhite,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Orbit History",
                        color = NexoraColors.StarWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${posts.size} signals",
                        color = NexoraColors.StarWhite.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0x1FFFFFFF))
            )

            if (errorMsg.isNotEmpty()) {
                Text(
                    errorMsg,
                    color = NexoraColors.NebulaPink,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            if (posts.isEmpty() && errorMsg.isEmpty()) {
                Text(
                    "Your orbit history is empty. Send your first signal from Orbit.",
                    color = NexoraColors.StarWhite,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts, key = { it.id }) { post ->
                        val mine = post.authorUid == myUid
                        val found = originals[post.resignalOf]
                        val d = buildPostDisplay(
                            id = post.id,
                            authorUid = post.authorUid,
                            authorName = post.name,
                            authorHandle = post.handle,
                            text = post.text,
                            time = post.time,
                            resignalOf = post.resignalOf,
                            resignalFromUid = post.resignalFromUid,
                            resignalFromName = post.resignalFromName,
                            resignalFromHandle = post.resignalFromHandle,
                            resignalFromTime = post.resignalFromTime,
                            quoteText = post.quoteText,
                            originalText = post.originalText,
                            foundUid = found?.uid.orEmpty(),
                            foundName = found?.name.orEmpty(),
                            foundHandle = found?.handle.orEmpty(),
                            foundTime = found?.time ?: 0L,
                            foundText = found?.text.orEmpty(),
                        )
                        val isRepost = d.repostedBy.isNotEmpty()
                        val liked = post.likes.contains(myUid)
                        val resignaled = if (isRepost) {
                            mine || found?.resignals?.contains(myUid) == true
                        } else {
                            post.resignals.contains(myUid)
                        }
                        val source = ResignalSource(
                            id = d.baseId,
                            authorUid = d.uid,
                            name = d.name,
                            handle = d.handle,
                            text = d.body,
                            time = d.time,
                            visibility = post.visibility,
                        )
                        val openAuthor: () -> Unit = {
                            if (d.uid.isNotBlank() && d.uid != myUid) {
                                onOpenUserProfile(d.uid)
                            }
                        }

                        LaunchedEffect(post.id) {
                            if (!mine && myUid.isNotEmpty()) {
                                db.collection("posts").document(post.id)
                                    .update("views", FieldValue.increment(1))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(modifier = Modifier.clickable { openAuthor() }) {
                                MyPostAvatar(
                                    if (d.uid.isNotEmpty() && d.uid == myUid) myPhoto else null,
                                    (d.name.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                    44.dp,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                PostCardText(d) { openAuthor() }
                                Spacer(Modifier.height(8.dp))
                                PostActionBar(
                                    liked = liked,
                                    resignaled = resignaled,
                                    likeCount = post.likes.size,
                                    resignalCount = post.resignals.size,
                                    onComment = { commentsFor = post.id },
                                    onLike = {
                                        db.collection("posts").document(post.id).update(
                                            "likes",
                                            if (liked) FieldValue.arrayRemove(myUid)
                                            else FieldValue.arrayUnion(myUid),
                                        )
                                    },
                                    onResignal = {
                                        if (resignaled) {
                                            undoResignal(db, myUid, d.baseId)
                                        } else {
                                            sendResignal(db, myUid, myName, myHandle, source, "")
                                        }
                                    },
                                    onShare = {
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, d.body)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(send, "Share")
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                    viewCount = post.views,
                                    saved = post.saved.contains(myUid),
                                    visibility = post.visibility,
                                    isOwnPost = mine,
                                    onQuote = { quoteSource = source },
                                    onReactWithVideo = {
                                        Toast.makeText(
                                            context,
                                            "Video reactions coming soon",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    },
                                    onSave = {
                                        val isSaved = post.saved.contains(myUid)
                                        db.collection("posts").document(post.id).update(
                                            "saved",
                                            if (isSaved) FieldValue.arrayRemove(myUid)
                                            else FieldValue.arrayUnion(myUid),
                                        )
                                    },
                                    onVisibilityChange = { v ->
                                        db.collection("posts").document(post.id)
                                            .update("visibility", v)
                                    },
                                    onDelete = {
                                        db.collection("posts").document(post.id).delete()
                                        if (mine && post.resignalOf.isNotEmpty()) {
                                            db.collection("posts").document(post.resignalOf)
                                                .update("resignals", FieldValue.arrayRemove(myUid))
                                        }
                                    },
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x1FFFFFFF))
                        )
                    }
                }
            }
        }

        commentsFor?.let { CommentsDialog(it, myUid, myName) { commentsFor = null } }

        quoteSource?.let { q ->
            AlertDialog(
                onDismissRequest = { quoteSource = null; quoteText = "" },
                containerColor = Color(0xFF14161C),
                titleContentColor = NexoraColors.StarWhite,
                textContentColor = NexoraColors.StarWhite,
                title = { Text("Quote", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = quoteText,
                        onValueChange = { if (it.length <= 280) quoteText = it },
                        placeholder = {
                            Text(
                                "Add your thoughts...",
                                color = NexoraColors.StarWhite.copy(alpha = 0.45f),
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NexoraColors.StarWhite,
                            unfocusedTextColor = NexoraColors.StarWhite,
                            cursorColor = NexoraColors.PlasmaCyan,
                            focusedBorderColor = NexoraColors.PlasmaCyan,
                            unfocusedBorderColor = NexoraColors.StarWhite.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        sendResignal(db, myUid, myName, myHandle, q, quoteText)
                        quoteSource = null
                        quoteText = ""
                    }) {
                        Text(
                            "Send signal",
                            color = NexoraColors.PlasmaCyan,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { quoteSource = null; quoteText = "" }) {
                        Text("Cancel", color = NexoraColors.StarWhite.copy(alpha = 0.7f))
                    }
                },
            )
        }
    }
}
