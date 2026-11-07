package com.nexora.circle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.theme.NexoraColors

@Composable
fun UserProfileScreen(
    uid: String,
    onBack: () -> Unit,
    onBeam: (String) -> Unit = {},
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isFriend by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var received by remember { mutableStateOf(false) }
    var following by remember { mutableStateOf(false) }
    var friendsCount by remember { mutableStateOf(0) }
    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }
    var showDetails by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    val sentRef = db.collection("signals").document("${myUid}_$uid")
    val recvRef = db.collection("signals").document("${uid}_$myUid")
    val myFriendRef = db.collection("users").document(myUid).collection("friends").document(uid)
    val theirFriendRef = db.collection("users").document(uid).collection("friends").document(myUid)
    val myFollowingRef = db.collection("users").document(myUid).collection("following").document(uid)
    val theirFollowersRef = db.collection("users").document(uid).collection("followers").document(myUid)

    DisposableEffect(uid) {
        val r1 = db.collection("users").document(uid).addSnapshotListener { s, _ ->
            if (s != null) {
                name = s.getString("name") ?: ""
                username = s.getString("username") ?: ""
                bio = s.getString("bio") ?: ""
            }
        }
        val r2 = myFriendRef.addSnapshotListener { s, _ -> isFriend = s?.exists() == true }
        val r3 = sentRef.addSnapshotListener { s, _ -> sent = s?.exists() == true }
        val r4 = recvRef.addSnapshotListener { s, _ -> received = s?.exists() == true }
        val r5 = myFollowingRef.addSnapshotListener { s, _ -> following = s?.exists() == true }
        val r6 = db.collection("users").document(uid).collection("friends")
            .addSnapshotListener { s, _ -> friendsCount = s?.size() ?: 0 }
        val r7 = db.collection("users").document(uid).collection("followers")
            .addSnapshotListener { s, _ -> followersCount = s?.size() ?: 0 }
        val r8 = db.collection("users").document(uid).collection("following")
            .addSnapshotListener { s, _ -> followingCount = s?.size() ?: 0 }
        onDispose {
            r1.remove(); r2.remove(); r3.remove(); r4.remove()
            r5.remove(); r6.remove(); r7.remove(); r8.remove()
        }
    }

    fun fail(e: Exception) { msg = "Error: ${e.localizedMessage}" }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("Back", color = NexoraColors.PlasmaCyan) }
        Spacer(Modifier.height(16.dp))
        Text(name, color = NexoraColors.StarWhite)
        Text("X.$username", color = NexoraColors.PlasmaCyan)
        Spacer(Modifier.height(6.dp))
        Text(if (bio.isBlank()) "No bio yet" else bio, color = NexoraColors.Muted)
        Spacer(Modifier.height(16.dp))

        if (uid == myUid || myUid.isEmpty()) {
            Text("This is you", color = NexoraColors.Muted)
        } else {
            // Signal (friend request) button
            if (isFriend) {
                OutlinedButton(
                    onClick = {
                        val b = db.batch()
                        b.delete(myFriendRef)
                        b.delete(theirFriendRef)
                        b.commit().addOnFailureListener { fail(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Signaled (tap to remove)") }
            } else if (received) {
                Button(
                    onClick = {
                        val b = db.batch()
                        b.set(myFriendRef, mapOf("time" to System.currentTimeMillis()))
                        b.set(theirFriendRef, mapOf("time" to System.currentTimeMillis()))
                        b.delete(recvRef)
                        b.commit().addOnFailureListener { fail(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Accept signal") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { recvRef.delete().addOnFailureListener { fail(it) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Decline") }
            } else if (sent) {
                OutlinedButton(
                    onClick = { sentRef.delete().addOnFailureListener { fail(it) } },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Cancel signal") }
            } else {
                Button(
                    onClick = {
                        sentRef.set(
                            mapOf(
                                "from" to myUid,
                                "to" to uid,
                                "time" to System.currentTimeMillis(),
                            )
                        ).addOnFailureListener { fail(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Send Signal") }
            }

            Spacer(Modifier.height(8.dp))

            // Follow / View profile / Beam
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
                            b.commit().addOnFailureListener { fail(it) }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = pad,
                    ) { Text("Following", fontSize = 13.sp, maxLines = 1) }
                } else {
                    Button(
                        onClick = {
                            val b = db.batch()
                            b.set(myFollowingRef, mapOf("time" to System.currentTimeMillis()))
                            b.set(theirFollowersRef, mapOf("time" to System.currentTimeMillis()))
                            b.commit().addOnFailureListener { fail(it) }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = pad,
                    ) { Text("Follow", fontSize = 13.sp, maxLines = 1) }
                }
                OutlinedButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.weight(1f),
                    contentPadding = pad,
                ) { Text("View profile", fontSize = 13.sp, maxLines = 1) }
                BeamButton(
                    onClick = { onBeam(uid) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (showDetails) {
            Spacer(Modifier.height(16.dp))
            Text("Signaled: $friendsCount", color = NexoraColors.StarWhite)
            Text("Followers: $followersCount", color = NexoraColors.StarWhite)
            Text("Following: $followingCount", color = NexoraColors.StarWhite)
        }

        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(msg, color = NexoraColors.NebulaPink)
        }
    }
}
