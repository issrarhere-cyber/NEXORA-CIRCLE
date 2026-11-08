package com.nexora.circle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.nexora.circle.ui.theme.NexoraColors

private data class CommentItem(
    val id: String,
    val uid: String,
    val name: String,
    val text: String,
    val time: Long,
    val likedBy: List<String>,
    val replyTo: String,
)

private fun timeAgo(t: Long): String {
    if (t <= 0L) return ""
    val m = (System.currentTimeMillis() - t) / 60000
    return when {
        m < 1 -> "now"
        m < 60 -> "${m}m"
        m < 1440 -> "${m / 60}h"
        else -> "${m / 1440}d"
    }
}

@Composable
fun CommentsDialog(
    postId: String,
    myUid: String,
    myName: String,
    onOpenProfile: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val comments = remember { mutableStateListOf<CommentItem>() }
    var draft by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var replyTo by remember { mutableStateOf("") }

    fun commentsRef() = db.collection("posts").document(postId).collection("comments")

    fun openProfile(uid: String) {
        if (uid.isNotBlank()) {
            onDismiss()
            onOpenProfile(uid)
        }
    }

    fun toggleLike(c: CommentItem) {
        if (myUid.isBlank()) return
        val op = if (myUid in c.likedBy) FieldValue.arrayRemove(myUid) else FieldValue.arrayUnion(myUid)
        commentsRef().document(c.id).update("likedBy", op)
            .addOnFailureListener { e -> errorMsg = "Like failed: ${e.localizedMessage}" }
    }

    fun deleteComment(c: CommentItem) {
        commentsRef().document(c.id).delete()
            .addOnFailureListener { e -> errorMsg = "Delete failed: ${e.localizedMessage}" }
    }

    DisposableEffect(postId) {
        val reg = commentsRef()
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    errorMsg = "Load failed: ${err.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snap != null) {
                    comments.clear()
                    comments.addAll(
                        snap.documents.map { d ->
                            CommentItem(
                                id = d.id,
                                uid = d.getString("uid") ?: "",
                                name = d.getString("name") ?: "Explorer",
                                text = d.getString("text") ?: "",
                                time = d.getLong("time") ?: 0L,
                                likedBy = (d.get("likedBy") as? List<*>)
                                    ?.filterIsInstance<String>() ?: emptyList(),
                                replyTo = d.getString("replyTo") ?: "",
                            )
                        }
                    )
                }
            }
        onDispose { reg.remove() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comments") },
        text = {
            Column {
                if (comments.isEmpty()) {
                    Text("No comments yet.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(comments, key = { it.id }) { c ->
                            val liked = myUid in c.likedBy
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NexoraColors.PlasmaCyan.copy(alpha = 0.35f))
                                        .clickable { openProfile(c.uid) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (com.nexora.circle.ui.drawer.AnonymousMode.isAnon(c.uid)) {
                                        com.nexora.circle.ui.drawer.AnonAvatar(36.dp)
                                    } else {
                                        Text(c.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            com.nexora.circle.ui.drawer.AnonymousMode.shownName(c.uid, c.name),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { openProfile(c.uid) },
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(timeAgo(c.time), color = NexoraColors.Muted, fontSize = 12.sp)
                                    }
                                    if (c.replyTo.isNotEmpty()) {
                                        Text("↩ ${c.replyTo}", color = NexoraColors.Muted, fontSize = 12.sp)
                                    }
                                    Text(c.text)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            (if (liked) "♥ " else "♡ ") + c.likedBy.size,
                                            color = if (liked) Color(0xFFFF4D5E) else NexoraColors.Muted,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .clickable { toggleLike(c) }
                                                .padding(vertical = 4.dp, horizontal = 2.dp),
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            "Reply",
                                            color = NexoraColors.Muted,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .clickable { replyTo = c.name }
                                                .padding(vertical = 4.dp, horizontal = 2.dp),
                                        )
                                        if (c.uid == myUid && myUid.isNotBlank()) {
                                            Spacer(Modifier.width(14.dp))
                                            Text(
                                                "Delete",
                                                color = Color(0xFFFF4D5E),
                                                fontSize = 13.sp,
                                                modifier = Modifier
                                                    .clickable { deleteComment(c) }
                                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (replyTo.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Replying to $replyTo",
                            color = NexoraColors.Muted,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "✕",
                            color = NexoraColors.Muted,
                            modifier = Modifier
                                .clickable { replyTo = "" }
                                .padding(8.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Write a comment") },
                )
                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg, color = Color(0xFFFF4D5E))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending,
                onClick = {
                    if (draft.isBlank()) return@TextButton
                    if (myUid.isBlank()) {
                        errorMsg = "You are not signed in."
                        return@TextButton
                    }
                    sending = true
                    errorMsg = ""
                    commentsRef().add(
                        hashMapOf(
                            "uid" to myUid,
                            "name" to myName,
                            "text" to draft.trim(),
                            "time" to System.currentTimeMillis(),
                            "replyTo" to replyTo,
                            "likedBy" to emptyList<String>(),
                        )
                    ).addOnSuccessListener {
                        draft = ""
                        replyTo = ""
                        sending = false
                    }.addOnFailureListener { e ->
                        errorMsg = "Send failed: ${e.localizedMessage}"
                        sending = false
                    }
                },
            ) { Text(if (sending) "Sending..." else "Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
