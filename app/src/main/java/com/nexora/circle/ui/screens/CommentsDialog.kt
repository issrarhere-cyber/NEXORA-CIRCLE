package com.nexora.circle.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private data class CommentItem(val id: String, val name: String, val text: String)

@Composable
fun CommentsDialog(postId: String, myUid: String, myName: String, onDismiss: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    val comments = remember { mutableStateListOf<CommentItem>() }
    var draft by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    DisposableEffect(postId) {
        val reg = db.collection("posts").document(postId).collection("comments")
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
                                d.id,
                                d.getString("name") ?: "Explorer",
                                d.getString("text") ?: "",
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
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(comments, key = { it.id }) { c ->
                            Text(c.name, fontWeight = FontWeight.Bold)
                            Text(c.text)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
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
                    db.collection("posts").document(postId).collection("comments").add(
                        hashMapOf(
                            "uid" to myUid,
                            "name" to myName,
                            "text" to draft.trim(),
                            "time" to System.currentTimeMillis(),
                        )
                    ).addOnSuccessListener {
                        draft = ""
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
