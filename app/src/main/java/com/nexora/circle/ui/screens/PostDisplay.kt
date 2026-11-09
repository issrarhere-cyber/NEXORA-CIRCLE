package com.nexora.circle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.components.FounderBadge
import com.nexora.circle.ui.components.isFounder
import com.nexora.circle.ui.theme.NexoraColors

/** What one post card should show. */
data class PostDisplay(
    val repostedBy: String,   // "" = normal post, no "Re-Signaled by" line
    val uid: String,          // whose name / avatar is shown
    val name: String,
    val handle: String,
    val time: Long,
    val body: String,
    val comment: String,      // comment of the person who re-signaled ("" = none)
    val baseId: String,       // id of the original post
)

/** The original post that is being re-signaled. */
data class ResignalSource(
    val id: String,
    val authorUid: String,
    val name: String,
    val handle: String,
    val text: String,
    val time: Long,
    val visibility: String,
)

fun postAgo(time: Long): String {
    val m = (System.currentTimeMillis() - time) / 60000
    return when {
        m < 1 -> "now"
        m < 60 -> "${m}m"
        m < 1440 -> "${m / 60}h"
        else -> "${m / 1440}d"
    }
}

/**
 * Rules:
 * - own post (or a copy of own post): no "Re-Signaled by" line
 * - someone re-signals another person's post: "Re-Signaled by <re-signaler>" on top,
 *   the original poster's name below it
 * found* values come from the original post when the screen already has it (old data).
 */
fun buildPostDisplay(
    id: String,
    authorUid: String,
    authorName: String,
    authorHandle: String,
    text: String,
    time: Long,
    resignalOf: String,
    resignalFromUid: String,
    resignalFromName: String,
    resignalFromHandle: String,
    resignalFromTime: Long,
    quoteText: String,
    originalText: String,
    foundUid: String = "",
    foundName: String = "",
    foundHandle: String = "",
    foundTime: Long = 0L,
    foundText: String = "",
): PostDisplay {
    val normal = PostDisplay("", authorUid, authorName, authorHandle, time, text, "", id)
    if (resignalOf.isEmpty()) return normal

    val origUid = resignalFromUid.ifEmpty { foundUid }
    val origName = resignalFromName.ifEmpty { foundName }
    val origHandle = resignalFromHandle.ifEmpty { foundHandle }
    val origTime = when {
        resignalFromTime > 0L -> resignalFromTime
        foundTime > 0L -> foundTime
        else -> time
    }
    val origText = originalText.ifEmpty { foundText }

    val self = if (origUid.isNotEmpty()) {
        origUid == authorUid
    } else {
        origName.isNotEmpty() && origName == authorName
    }
    if (self) return normal

    val comment = when {
        quoteText.isNotEmpty() -> quoteText
        origText.isNotEmpty() && text.endsWith(origText) -> text.removeSuffix(origText).trim()
        else -> ""
    }
    return PostDisplay(
        repostedBy = authorName.ifBlank { "Explorer" },
        uid = origUid,
        name = origName.ifBlank { "Explorer" },
        handle = origHandle,
        time = origTime,
        body = if (origText.isNotEmpty()) origText else text,
        comment = comment,
        baseId = resignalOf,
    )
}

/** Re-signal (comment may be empty). Re-signaling your own post without a comment only counts. */
fun sendResignal(
    db: FirebaseFirestore,
    myUid: String,
    myName: String,
    myHandle: String,
    src: ResignalSource,
    comment: String,
) {
    val c = comment.trim()
    if (!(src.authorUid == myUid && c.isEmpty())) {
        db.collection("posts").add(
            hashMapOf<String, Any>(
                "authorUid" to myUid,
                "authorName" to myName,
                "authorHandle" to myHandle,
                "text" to (c + "\n\n" + src.text).trim(),
                "quoteText" to c,
                "originalText" to src.text,
                "time" to System.currentTimeMillis(),
                "likes" to emptyList<String>(),
                "resignals" to emptyList<String>(),
                "resignalOf" to src.id,
                "resignalFromUid" to src.authorUid,
                "resignalFromName" to src.name,
                "resignalFromHandle" to src.handle,
                "resignalFromTime" to src.time,
                "visibility" to src.visibility,
            )
        )
    }
    db.collection("posts").document(src.id)
        .update("resignals", FieldValue.arrayUnion(myUid))
}

/** Undo re-signal: remove my count and delete my re-signal entry. */
fun undoResignal(db: FirebaseFirestore, myUid: String, originalId: String) {
    db.collection("posts").document(originalId)
        .update("resignals", FieldValue.arrayRemove(myUid))
    db.collection("posts")
        .whereEqualTo("authorUid", myUid)
        .whereEqualTo("resignalOf", originalId)
        .get()
        .addOnSuccessListener { snap ->
            snap.documents.forEach { it.reference.delete() }
        }
}

/** The text part of a post card (label, name, handle, time, text, comment). */
@Composable
fun PostCardText(d: PostDisplay, onAuthorClick: () -> Unit) {
    Column {
        if (d.repostedBy.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Re-Signaled by",
                    color = NexoraColors.StarWhite.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    d.repostedBy,
                    color = NexoraColors.PlasmaCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Spacer(Modifier.height(2.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                com.nexora.circle.ui.drawer.AnonymousMode.shownName(d.uid, d.name.ifBlank { "Explorer" }),
                color = NexoraColors.StarWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable { onAuthorClick() },
            )
            if (isFounder(d.uid) && !com.nexora.circle.ui.drawer.AnonymousMode.isAnon(d.uid)) {
                Spacer(Modifier.width(6.dp))
                FounderBadge(size = 18.dp)
            }
        }
        Text(
            (if (d.handle.isNotBlank() && !com.nexora.circle.ui.drawer.AnonymousMode.isAnon(d.uid)) "X.${d.handle} · " else "") + postAgo(d.time),
            color = NexoraColors.StarWhite.copy(alpha = 0.6f),
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            d.body,
            color = NexoraColors.StarWhite,
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
        if (d.comment.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(NexoraColors.PlasmaCyan)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        d.repostedBy,
                        color = NexoraColors.PlasmaCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        d.comment,
                        color = NexoraColors.StarWhite,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
    }
}
