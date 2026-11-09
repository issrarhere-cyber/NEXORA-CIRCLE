package com.nexora.circle.ui.drawer

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Anonymous Mode.
 *
 * - My own switch is saved on this phone, so it works instantly.
 * - The switch is also written to Firestore (anonymousUsers/{uid}) so other people's
 *   phones can hide my name and photo too. If Firestore rules block that write,
 *   [syncError] becomes true and the screen tells the user honestly.
 * - Every place that shows a person's name asks [isAnon] / [shownName].
 */
object AnonymousMode {
    const val NAME = "Anonymous User"

    private const val PREFS = "nexora_anon"

    /** My own switch. */
    var enabled by mutableStateOf(false)
        private set

    /** True when the last save to Firestore failed (others cannot see my switch yet). */
    var syncError by mutableStateOf(false)
        private set

    /** Uids of other people whose Anonymous Mode is ON (from Firestore). */
    private var others by mutableStateOf<Set<String>>(emptySet())

    private var currentUid: String = ""
    private var registration: ListenerRegistration? = null

    /** Call once the user is signed in. Safe to call again. */
    fun start(context: Context, uid: String) {
        if (uid.isBlank() || uid == currentUid) return
        currentUid = uid
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        enabled = prefs.getBoolean("enabled_$uid", false)
        syncError = false

        if (registration == null) {
            registration = FirebaseFirestore.getInstance()
                .collection("anonymousUsers")
                .addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        others = snap.documents.map { it.id }.toSet()
                    }
                }
        }
        // Make sure Firestore matches the saved switch (e.g. after reinstall of the listener).
        if (enabled) writeRemote(uid, true)
    }

    fun set(context: Context, on: Boolean) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        enabled = on
        if (uid.isNotBlank()) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("enabled_$uid", on).apply()
            writeRemote(uid, on)
        }
    }

    private fun writeRemote(uid: String, on: Boolean) {
        val ref = FirebaseFirestore.getInstance().collection("anonymousUsers").document(uid)
        val task = if (on) {
            ref.set(mapOf("on" to true, "time" to System.currentTimeMillis()))
        } else {
            ref.delete()
        }
        task.addOnSuccessListener { syncError = false }
        task.addOnFailureListener { syncError = true }
    }

    /** Is this person currently hidden behind Anonymous Mode? */
    fun isAnon(uid: String): Boolean {
        if (uid.isBlank()) return false
        val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        return if (uid == me) enabled else others.contains(uid)
    }

    /** The name to show for this person. */
    fun shownName(uid: String, realName: String): String =
        if (isAnon(uid)) NAME else realName
}

/** Grey avatar with a person icon, used whenever a person is anonymous. */
@Composable
fun AnonAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF6B6F76)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = "Anonymous",
            tint = Color(0xFFD6D8DC),
            modifier = Modifier.size(size * 0.62f),
        )
    }
}
