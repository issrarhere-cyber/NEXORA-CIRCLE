package com.nexora.circle.ui.screens

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.theme.NexoraColors

@Composable
fun UserProfileScreen(
    uid: String,
    onBack: () -> Unit,
    onBeam: (String) -> Unit = {},
) {
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }

    // Guard: an empty uid makes Firestore's document("") throw and crash the app
    if (myUid.isEmpty() || uid.isBlank()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            TextButton(onClick = onBack) { Text("Back", color = NexoraColors.PlasmaCyan) }
            Spacer(Modifier.height(16.dp))
            Text("Please sign in to view profiles.", color = NexoraColors.StarWhite)
        }
        return
    }
    ProfileContent(uid, myUid, onBack, onBeam)
}

@Composable
private fun ProfileContent(
    uid: String,
    myUid: String,
    onBack: () -> Unit,
    onBeam: (String) -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val isMe = uid == myUid

    var loading by remember { mutableStateOf(true) }
    var notFound by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    var isFriend by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var received by remember { mutableStateOf(false) }
    var following by remember { mutableStateOf(false) }

    var friendsCount by remember { mutableStateOf(0L) }
    var followersCount by remember { mutableStateOf(0L) }
    var followingCount by remember { mutableStateOf(0L) }

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmReport by remember { mutableStateOf(false) }

    val sentRef = remember(uid) { db.collection("signals").document("${myUid}_$uid") }
    val recvRef = remember(uid) { db.collection("signals").document("${uid}_$myUid") }
    val myFriendRef = remember(uid) { db.collection("users").document(myUid).collection("friends").document(uid) }
    val theirFriendRef = remember(uid) { db.collection("users").document(uid).collection("friends").document(myUid) }
    val myFollowingRef = remember(uid) { db.collection("users").document(myUid).collection("following").document(uid) }
    val theirFollowersRef = remember(uid) { db.collection("users").document(uid).collection("followers").document(myUid) }

    DisposableEffect(uid) {
        val regs = mutableListOf(
            db.collection("users").document(uid).addSnapshotListener { s, e ->
                if (e != null) {
                    Log.e("UserProfile", "profile load failed", e)
                    loadFailed = true; loading = false
                    return@addSnapshotListener
                }
                if (s != null) {
                    loadFailed = false
                    notFound = !s.exists()
                    name = s.getString("name") ?: ""
                    username = s.getString("username") ?: ""
                    bio = s.getString("bio") ?: ""
                    loading = false
                }
            }
        )
        if (!isMe) {
            regs += myFriendRef.addSnapshotListener { s, _ -> isFriend = s?.exists() == true }
            regs += sentRef.addSnapshotListener { s, _ -> sent = s?.exists() == true }
            regs += recvRef.addSnapshotListener { s, _ -> received = s?.exists() == true }
            regs += myFollowingRef.addSnapshotListener { s, _ -> following = s?.exists() == true }
        }
        onDispose { regs.forEach { it.remove() } }
    }

    // Counts use server-side aggregation: 1 read each, instead of downloading every document
    LaunchedEffect(uid, isFriend, following, busy) {
        if (busy) return@LaunchedEffect
        val base = db.collection("users").document(uid)
        fun count(col: String, set: (Long) -> Unit) {
            base.collection(col).count().get(AggregateSource.SERVER)
                .addOnSuccessListener { set(it.count) }
        }
        count("friends") { friendsCount = it }
        count("followers") { followersCount = it }
        count("following") { followingCount = it }
    }

    // Runs a write, blocks double taps, shows a friendly error (raw error goes to Logcat)
    fun <T> track(task: Task<T>, success: String = "") {
        busy = true; error = ""; notice = ""
        task.addOnCompleteListener {
            busy = false
            if (it.isSuccessful) notice = success
            else {
                Log.e("UserProfile", "action failed", it.exception)
                error = "Something went wrong. Please try again."
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Back", color = NexoraColors.PlasmaCyan) }
            if (!isMe && !loading && !notFound) {
                TextButton(onClick = { confirmReport = true }) {
                    Text("Report", color = NexoraColors.Muted)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) {
                CircularProgressIndicator(color = NexoraColors.PlasmaCyan)
            }

            loadFailed -> Text(
                "Couldn't load this profile. Check your connection and try again.",
                color = NexoraColors.NebulaPink,
            )

            notFound -> Text("This user doesn't exist.", color = NexoraColors.StarWhite)

            else -> {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(NexoraColors.PlasmaCyan),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (name.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                        color = NexoraColors.SpaceBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    name.ifBlank { "Explorer" },
                    color = NexoraColors.StarWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
                if (username.isNotBlank()) {
                    Text("X.$username", color = NexoraColors.PlasmaCyan)
                }
                Spacer(Modifier.height(6.dp))
                Text(if (bio.isBlank()) "No bio yet" else bio, color = NexoraColors.Muted)
                Spacer(Modifier.height(16.dp))

                // Stats are always visible now
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("Signaled", friendsCount)
                    Stat("Followers", followersCount)
                    Stat("Following", followingCount)
                }
                Spacer(Modifier.height(20.dp))

                if (isMe) {
                    Text("This is you", color = NexoraColors.Muted)
                } else {
                    val full = Modifier.fillMaxWidth()
                    when {
                        isFriend -> OutlinedButton(
                            onClick = { confirmRemove = true },
                            enabled = !busy,
                            modifier = full,
                        ) { Text("Signaled (tap to remove)") }

                        received -> {
                            Button(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val b = db.batch()
                                    b.set(myFriendRef, mapOf("time" to now))
                                    b.set(theirFriendRef, mapOf("time" to now))
                                    b.delete(recvRef)
                                    track(b.commit())
                                },
                                enabled = !busy,
                                modifier = full,
                            ) { Text("Accept signal") }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { track(recvRef.delete()) },
                                enabled = !busy,
                                modifier = full,
                            ) { Text("Decline") }
                        }

                        sent -> OutlinedButton(
                            onClick = { track(sentRef.delete()) },
                            enabled = !busy,
                            modifier = full,
                        ) { Text("Cancel signal") }

                        else -> Button(
                            onClick = {
                                track(
                                    sentRef.set(
                                        mapOf(
                                            "from" to myUid,
                                            "to" to uid,
                                            "time" to System.currentTimeMillis(),
                                        )
                                    )
                                )
                            },
                            enabled = !busy,
                            modifier = full,
                        ) { Text("Send Signal") }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val pad = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        if (following) {
                            OutlinedButton(
                                onClick = {
                                    val b = db.batch()
                                    b.delete(myFollowingRef)
                                    b.delete(theirFollowersRef)
                                    track(b.commit())
                                },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                                contentPadding = pad,
                            ) { Text("Following", fontSize = 13.sp, maxLines = 1) }
                        } else {
                            Button(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    val b = db.batch()
                                    b.set(myFollowingRef, mapOf("time" to now))
                                    b.set(theirFollowersRef, mapOf("time" to now))
                                    track(b.commit())
                                },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                                contentPadding = pad,
                            ) { Text("Follow", fontSize = 13.sp, maxLines = 1) }
                        }
                        BeamButton(
                            onClick = { onBeam(uid) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = NexoraColors.NebulaPink)
                }
                if (notice.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(notice, color = NexoraColors.StarWhite)
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove signal?") },
            text = { Text("You will no longer be signaled with ${name.ifBlank { "this user" }}.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    val b = db.batch()
                    b.delete(myFriendRef)
                    b.delete(theirFriendRef)
                    track(b.commit())
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
        )
    }

    if (confirmReport) {
        AlertDialog(
            onDismissRequest = { confirmReport = false },
            title = { Text("Report this user?") },
            text = { Text("Our team will review this profile.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReport = false
                    track(
                        db.collection("reports").add(
                            mapOf(
                                "reporterUid" to myUid,
                                "reportedUid" to uid,
                                "time" to System.currentTimeMillis(),
                            )
                        ),
                        success = "Report sent. Thank you.",
                    )
                }) { Text("Report") }
            },
            dismissButton = { TextButton(onClick = { confirmReport = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Stat(label: String, value: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            color = NexoraColors.StarWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Text(label, color = NexoraColors.Muted, fontSize = 12.sp)
    }
}
