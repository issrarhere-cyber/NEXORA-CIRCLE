package com.nexora.circle.ui.drawer

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.nexora.circle.ui.components.isFounder
import com.nexora.circle.ui.theme.NexoraColors

private val LiveRed = Color(0xFFFF3B5C)
private val DangerRed = Color(0xFFFF4D5E)

/** Limits for one Nexus room. */
object NexusConfig {
    const val MAX_MEMBERS = 5000L
    const val CHAT_LIMIT = 100L
    const val LIST_LIMIT = 100L
}

/**
 * Plug-in point for a real-time voice service (Agora, LiveKit, ZEGOCLOUD ...).
 * While no engine is set, rooms, roles, speaking permission, mute, remove and
 * text chat all work for real, but no audio is carried. The room screen says so.
 */
interface NexusVoiceEngine {
    fun connect(roomId: String, uid: String, canSpeak: Boolean)
    fun setMicOn(on: Boolean)
    fun disconnect()
}

object NexusVoice {
    var engine: NexusVoiceEngine? = null
}

data class NexusRoom(
    val id: String,
    val title: String,
    val description: String,
    val hostUid: String,
    val hostName: String,
    val createdAt: Long,
    val memberCount: Long,
    val maxMembers: Long,
    val status: String,
)

data class NexusMember(
    val uid: String,
    val name: String,
    val role: String,
    val canSpeak: Boolean,
    val muted: Boolean,
    val handRaised: Boolean,
    val micOn: Boolean,
)

data class NexusMessage(
    val id: String,
    val uid: String,
    val name: String,
    val text: String,
    val time: Long,
)

private fun DocumentSnapshot.toRoom(): NexusRoom = NexusRoom(
    id = id,
    title = getString("title").orEmpty(),
    description = getString("description").orEmpty(),
    hostUid = getString("hostUid").orEmpty(),
    hostName = getString("hostName").orEmpty(),
    createdAt = getLong("createdAt") ?: 0L,
    memberCount = getLong("memberCount") ?: 0L,
    maxMembers = getLong("maxMembers") ?: NexusConfig.MAX_MEMBERS,
    status = getString("status") ?: "live",
)

private fun DocumentSnapshot.toMember(): NexusMember = NexusMember(
    uid = id,
    name = getString("name").orEmpty(),
    role = getString("role") ?: "listener",
    canSpeak = getBoolean("canSpeak") == true,
    muted = getBoolean("muted") == true,
    handRaised = getBoolean("handRaised") == true,
    micOn = getBoolean("micOn") == true,
)

private fun DocumentSnapshot.toMessage(): NexusMessage = NexusMessage(
    id = id,
    uid = getString("uid").orEmpty(),
    name = getString("name").orEmpty(),
    text = getString("text").orEmpty(),
    time = getLong("time") ?: 0L,
)

private fun myDisplayName(context: Context): String {
    val prefs = context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE)
    val saved = prefs.getString("name", "")?.trim().orEmpty()
    if (saved.isNotBlank()) return saved
    val auth = FirebaseAuth.getInstance().currentUser?.displayName.orEmpty()
    return auth.ifBlank { "Explorer" }
}

private fun formatCount(n: Long): String = "%,d".format(n)

/** True for the founder or any account that has isAdmin = true in users/{uid}. */
@Composable
private fun rememberIsAppAdmin(uid: String): Boolean {
    var admin by remember(uid) { mutableStateOf(isFounder(uid)) }
    LaunchedEffect(uid) {
        if (uid.isNotBlank() && !admin) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { d ->
                    if (d.getBoolean("isAdmin") == true) admin = true
                }
        }
    }
    return admin
}

// -------------------------------------------------------------------------------------
// Firestore actions
// -------------------------------------------------------------------------------------

private fun joinRoom(
    db: FirebaseFirestore,
    roomId: String,
    uid: String,
    name: String,
    anon: Boolean,
    onDone: (String?) -> Unit,
) {
    val roomRef = db.collection("nexusRooms").document(roomId)
    val memberRef = roomRef.collection("members").document(uid)
    val kickedRef = roomRef.collection("kicked").document(uid)
    db.runTransaction { tx ->
        val room = tx.get(roomRef)
        if (!room.exists() || room.getString("status") != "live") {
            throw FirebaseFirestoreException("This room has ended", FirebaseFirestoreException.Code.ABORTED)
        }
        if (tx.get(kickedRef).exists()) {
            throw FirebaseFirestoreException(
                "You were removed from this room",
                FirebaseFirestoreException.Code.PERMISSION_DENIED,
            )
        }
        val existing = tx.get(memberRef)
        if (!existing.exists()) {
            val count = room.getLong("memberCount") ?: 0L
            val max = room.getLong("maxMembers") ?: NexusConfig.MAX_MEMBERS
            if (count >= max) {
                throw FirebaseFirestoreException(
                    "This room is full (${formatCount(max)} members)",
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
                )
            }
            val isHostHere = room.getString("hostUid") == uid
            tx.set(
                memberRef,
                hashMapOf<String, Any>(
                    "name" to name,
                    "anon" to anon,
                    "role" to (if (isHostHere) "host" else "listener"),
                    "canSpeak" to isHostHere,
                    "muted" to false,
                    "handRaised" to false,
                    "micOn" to false,
                    "joinedAt" to System.currentTimeMillis(),
                ),
            )
            tx.update(roomRef, "memberCount", FieldValue.increment(1))
        }
        null
    }.addOnSuccessListener {
        onDone(null)
    }.addOnFailureListener { e ->
        onDone(e.localizedMessage ?: "Could not join the room")
    }
}

private fun leaveRoom(db: FirebaseFirestore, roomId: String, uid: String) {
    val roomRef = db.collection("nexusRooms").document(roomId)
    val memberRef = roomRef.collection("members").document(uid)
    db.runTransaction { tx ->
        val room = tx.get(roomRef)
        val member = tx.get(memberRef)
        if (member.exists()) {
            tx.delete(memberRef)
            if (room.exists()) {
                tx.update(roomRef, "memberCount", FieldValue.increment(-1))
            }
        }
        null
    }
}

private fun removeMember(db: FirebaseFirestore, roomId: String, targetUid: String) {
    val roomRef = db.collection("nexusRooms").document(roomId)
    val memberRef = roomRef.collection("members").document(targetUid)
    val kickedRef = roomRef.collection("kicked").document(targetUid)
    db.runTransaction { tx ->
        val room = tx.get(roomRef)
        val member = tx.get(memberRef)
        tx.set(kickedRef, hashMapOf<String, Any>("time" to System.currentTimeMillis()))
        if (member.exists()) {
            tx.delete(memberRef)
            if (room.exists()) {
                tx.update(roomRef, "memberCount", FieldValue.increment(-1))
            }
        }
        null
    }
}

// -------------------------------------------------------------------------------------
// Screens
// -------------------------------------------------------------------------------------

/** NEXORA NEXUS: lobby (live rooms) and the room itself. */
@Composable
fun NexusScreen(onBack: () -> Unit) {
    var roomId by remember { mutableStateOf("") }
    var autoJoin by remember { mutableStateOf(false) }

    if (roomId.isEmpty()) {
        NexusLobby(
            onBack = onBack,
            onOpen = { id, auto ->
                roomId = id
                autoJoin = auto
            },
        )
    } else {
        NexusRoomScreen(
            roomId = roomId,
            autoJoin = autoJoin,
            onExit = {
                roomId = ""
                autoJoin = false
            },
        )
    }
}

@Composable
private fun LivePill() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(LiveRed),
        )
        Spacer(Modifier.width(5.dp))
        Text("LIVE", color = LiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NexusLobby(onBack: () -> Unit, onOpen: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val isAdmin = rememberIsAppAdmin(myUid)

    var rooms by remember { mutableStateOf<List<NexusRoom>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var createOpen by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val reg = db.collection("nexusRooms")
            .whereEqualTo("status", "live")
            .addSnapshotListener { snap, err ->
                loading = false
                if (err != null) {
                    error = err.localizedMessage ?: "Could not load rooms"
                    return@addSnapshotListener
                }
                if (snap != null) {
                    error = ""
                    rooms = snap.documents.map { it.toRoom() }.sortedByDescending { it.createdAt }
                }
            }
        onDispose { reg.remove() }
    }

    DrawerScreenScaffold(
        title = "NEXORA NEXUS",
        onBack = onBack,
        actions = {
            if (isAdmin) {
                IconButton(onClick = { createOpen = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Nexus Room", tint = NexoraColors.StarWhite)
                }
            }
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                "Live voice and text rooms. Admins host the room and choose who speaks.",
                color = NexoraColors.Muted.copy(alpha = 0.75f),
                fontSize = 14.sp,
            )
            if (isAdmin) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = { createOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create Nexus Room")
                }
            }
        }

        when {
            loading -> NxEmpty("⏳", "Loading rooms...")
            error.isNotEmpty() -> NxEmpty(
                "⚠️",
                "Could not load rooms: $error\n\nIf this says permission denied, add the Nexus rules to Firestore (see NEXUS_AND_ANON_RULES.txt).",
            )
            rooms.isEmpty() -> NxEmpty(
                "🎙️",
                if (isAdmin) "No live rooms right now. Tap Create Nexus Room to start one."
                else "No live rooms right now. When an admin starts a room it will appear here.",
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(rooms, key = { it.id }) { room ->
                    val hostShown = AnonymousMode.shownName(room.hostUid, room.hostName.ifBlank { "Admin" })
                    NxCard(onClick = { onOpen(room.id, false) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LivePill()
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${formatCount(room.memberCount)} listening",
                                color = NexoraColors.Muted.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            room.title,
                            color = NexoraColors.StarWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (room.description.isNotBlank()) {
                            Text(
                                room.description,
                                color = NexoraColors.Muted.copy(alpha = 0.75f),
                                fontSize = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Hosted by $hostShown",
                            color = NexoraColors.PlasmaCyan,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onOpen(room.id, false) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Join") }
                    }
                }
            }
        }
    }

    if (createOpen) {
        CreateRoomDialog(
            onDismiss = { createOpen = false },
            onCreate = { title, description, done ->
                val data = hashMapOf<String, Any>(
                    "title" to title,
                    "description" to description,
                    "hostUid" to myUid,
                    "hostName" to myDisplayName(context),
                    "createdAt" to System.currentTimeMillis(),
                    "status" to "live",
                    "memberCount" to 0L,
                    "maxMembers" to NexusConfig.MAX_MEMBERS,
                )
                db.collection("nexusRooms").add(data)
                    .addOnSuccessListener { ref ->
                        createOpen = false
                        done(null)
                        onOpen(ref.id, true)
                    }
                    .addOnFailureListener { e ->
                        done(e.localizedMessage ?: "Could not create the room")
                    }
            },
        )
    }
}

@Composable
private fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, done: (String?) -> Unit) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Create Nexus Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80); error = "" },
                    label = { Text("Room title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(200) },
                    label = { Text("What is this room about? (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Up to ${formatCount(NexusConfig.MAX_MEMBERS)} people can join. You choose who can speak.",
                    color = NexoraColors.Muted.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
                if (error.isNotEmpty()) {
                    Text(error, color = DangerRed, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    if (title.isBlank()) {
                        error = "Please write a room title"
                    } else {
                        busy = true
                        onCreate(title.trim(), description.trim()) { err ->
                            busy = false
                            if (err != null) error = err
                        }
                    }
                },
            ) { Text(if (busy) "Creating..." else "Go live") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SmallAvatar(uid: String, name: String, size: Int) {
    if (AnonymousMode.isAnon(uid)) {
        AnonAvatar(size.dp)
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(NexoraColors.PlasmaCyan),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (name.trim().firstOrNull()?.uppercaseChar() ?: '?').toString(),
                color = NexoraColors.SpaceBlack,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.4f).sp,
            )
        }
    }
}

@Composable
private fun AdminMenu(
    member: NexusMember,
    onAllow: () -> Unit,
    onRevoke: () -> Unit,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Admin options", tint = NexoraColors.StarWhite)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (member.canSpeak) {
                DropdownMenuItem(
                    text = { Text("Move to listeners") },
                    onClick = { open = false; onRevoke() },
                )
                if (member.muted) {
                    DropdownMenuItem(
                        text = { Text("Unmute") },
                        onClick = { open = false; onUnmute() },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Mute") },
                        onClick = { open = false; onMute() },
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text("Allow to speak") },
                    onClick = { open = false; onAllow() },
                )
            }
            DropdownMenuItem(
                text = { Text("Remove from room", color = DangerRed) },
                onClick = { open = false; onRemove() },
            )
        }
    }
}

@Composable
private fun PersonRow(
    member: NexusMember,
    isMe: Boolean,
    showMenu: Boolean,
    onAllow: () -> Unit,
    onRevoke: () -> Unit,
    onMute: () -> Unit,
    onUnmute: () -> Unit,
    onRemove: () -> Unit,
) {
    val shown = AnonymousMode.shownName(member.uid, member.name.ifBlank { "Explorer" })
    val roleText = when {
        member.role == "host" -> "Host"
        member.canSpeak -> "Speaker"
        else -> "Listener"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallAvatar(member.uid, member.name, 42)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                if (isMe) "$shown (You)" else shown,
                color = NexoraColors.StarWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val extras = buildString {
                append(roleText)
                if (member.canSpeak && member.muted) append("  ·  Muted")
                else if (member.canSpeak && member.micOn) append("  ·  Mic on")
                if (member.handRaised) append("  ·  ✋ wants to speak")
            }
            Text(extras, color = NexoraColors.Muted.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        if (showMenu && member.role != "host" && !isMe) {
            AdminMenu(member, onAllow, onRevoke, onMute, onUnmute, onRemove)
        }
    }
}

@Composable
private fun NexusRoomScreen(roomId: String, autoJoin: Boolean, onExit: () -> Unit) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val isAppAdmin = rememberIsAppAdmin(myUid)

    val roomRef = remember(roomId) { db.collection("nexusRooms").document(roomId) }
    val memberRef = remember(roomId, myUid) { roomRef.collection("members").document(myUid) }

    var room by remember { mutableStateOf<NexusRoom?>(null) }
    var roomError by remember { mutableStateOf("") }
    var joined by remember { mutableStateOf(false) }
    var joining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf("") }
    var me by remember { mutableStateOf<NexusMember?>(null) }
    var speakers by remember { mutableStateOf<List<NexusMember>>(emptyList()) }
    var listeners by remember { mutableStateOf<List<NexusMember>>(emptyList()) }
    var requests by remember { mutableStateOf<List<NexusMember>>(emptyList()) }
    var messages by remember { mutableStateOf<List<NexusMessage>>(emptyList()) }
    var tab by remember { mutableStateOf(0) } // 0 = People, 1 = Chat
    var draft by remember { mutableStateOf("") }
    var endConfirm by remember { mutableStateOf(false) }

    // Remembers whether I am inside the room, so I leave it when this screen closes.
    val joinedFlag = remember { BooleanArray(1) }
    val ended = room?.status == "ended"
    val isHost = room?.hostUid == myUid && myUid.isNotBlank()
    val canManage = isHost || isAppAdmin

    fun startJoin() {
        if (joining || joined) return
        joining = true
        joinError = ""
        joinRoom(
            db = db,
            roomId = roomId,
            uid = myUid,
            name = myDisplayName(context),
            anon = AnonymousMode.enabled,
        ) { err ->
            joining = false
            if (err == null) {
                joinedFlag[0] = true
                joined = true
            } else {
                joinError = err
            }
        }
    }

    // Room details (live count, ended state)
    DisposableEffect(roomId) {
        val reg = roomRef.addSnapshotListener { snap, err ->
            if (err != null) {
                roomError = err.localizedMessage ?: "Could not load the room"
                return@addSnapshotListener
            }
            if (snap != null) {
                if (snap.exists()) {
                    room = snap.toRoom()
                    roomError = ""
                } else {
                    roomError = "This room no longer exists"
                }
            }
        }
        onDispose { reg.remove() }
    }

    // Leave the room when this screen closes
    DisposableEffect(roomId) {
        onDispose {
            if (joinedFlag[0]) {
                joinedFlag[0] = false
                leaveRoom(db, roomId, myUid)
            }
        }
    }

    // Host who just created the room joins straight away
    LaunchedEffect(roomId, autoJoin) {
        if (autoJoin) startJoin()
    }

    // People, requests and chat (only while I am inside the room)
    DisposableEffect(joined) {
        if (!joined) {
            return@DisposableEffect onDispose { }
        }
        val regMe = memberRef.addSnapshotListener { snap, _ ->
            if (snap != null) {
                if (snap.exists()) {
                    me = snap.toMember()
                } else if (joinedFlag[0]) {
                    // my member document disappeared: the admin removed me
                    joinedFlag[0] = false
                    Toast.makeText(context, "You were removed from this room by the admin", Toast.LENGTH_LONG).show()
                    onExit()
                }
            }
        }
        val regSpeakers = roomRef.collection("members")
            .whereEqualTo("canSpeak", true)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    speakers = snap.documents.map { it.toMember() }
                        .sortedWith(compareBy({ if (it.role == "host") 0 else 1 }, { it.name }))
                }
            }
        val regListeners = roomRef.collection("members")
            .whereEqualTo("canSpeak", false)
            .limit(NexusConfig.LIST_LIMIT)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    listeners = snap.documents.map { it.toMember() }.sortedBy { it.name }
                }
            }
        val regRequests = roomRef.collection("members")
            .whereEqualTo("handRaised", true)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    requests = snap.documents.map { it.toMember() }.filter { !it.canSpeak }
                }
            }
        val regMessages = roomRef.collection("messages")
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(NexusConfig.CHAT_LIMIT)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    messages = snap.documents.map { it.toMessage() }
                }
            }
        onDispose {
            regMe.remove()
            regSpeakers.remove()
            regListeners.remove()
            regRequests.remove()
            regMessages.remove()
        }
    }

    // Voice provider hook (does nothing until an engine is plugged in)
    val iCanSpeak = me?.canSpeak == true
    DisposableEffect(joined, iCanSpeak) {
        val engine = NexusVoice.engine
        if (joined && engine != null) {
            engine.connect(roomId, myUid, iCanSpeak)
        }
        onDispose { engine?.disconnect() }
    }

    // The room ended while I was inside
    LaunchedEffect(ended) {
        if (ended && joined) {
            Toast.makeText(context, "This room has ended", Toast.LENGTH_LONG).show()
            onExit()
        }
    }

    val title = room?.title ?: "Nexus room"

    DrawerScreenScaffold(
        title = title,
        onBack = onExit,
    ) {
        val r = room
        when {
            roomError.isNotEmpty() && r == null -> {
                NxEmpty("⚠️", roomError)
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) { Text("Back to rooms") }
            }
            r == null -> NxEmpty("⏳", "Opening room...")
            !joined -> {
                // ---- Before joining ----
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    NxCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!ended) {
                                LivePill()
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                if (ended) "This room has ended" else "${formatCount(r.memberCount)} listening",
                                color = NexoraColors.Muted.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            r.title,
                            color = NexoraColors.StarWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (r.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(r.description, color = NexoraColors.Muted.copy(alpha = 0.8f), fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Hosted by " + AnonymousMode.shownName(r.hostUid, r.hostName.ifBlank { "Admin" }),
                            color = NexoraColors.PlasmaCyan,
                            fontSize = 13.sp,
                        )
                        Text(
                            "Room limit: ${formatCount(r.maxMembers)} members",
                            color = NexoraColors.Muted.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                    if (joinError.isNotEmpty()) {
                        Text(joinError, color = DangerRed, fontSize = 14.sp)
                    }
                    if (!ended) {
                        Button(
                            onClick = { startJoin() },
                            enabled = !joining,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (joining) "Joining..." else "Join") }
                    }
                    OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                        Text("Back to rooms")
                    }
                }
            }
            else -> {
                // ---- Inside the room ----
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LivePill()
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${formatCount(r.memberCount)} listening",
                            color = NexoraColors.StarWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        if (canManage) {
                            TextButton(onClick = { endConfirm = true }) {
                                Text("End room", color = DangerRed)
                            }
                        }
                    }
                    if (NexusVoice.engine == null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Voice audio is not connected yet. Speaking permission, mute, remove and text chat work, but no sound is carried until a voice service is plugged in.",
                            color = NexoraColors.Muted.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NxChip("People", tab == 0, { tab = 0 })
                        NxChip("Chat", tab == 1, { tab = 1 })
                    }
                }

                if (tab == 0) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        if (canManage && requests.isNotEmpty()) {
                            item { NxSectionLabel("Requests to speak (${requests.size})") }
                            items(requests, key = { "req_" + it.uid }) { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    SmallAvatar(m.uid, m.name, 42)
                                    Text(
                                        AnonymousMode.shownName(m.uid, m.name.ifBlank { "Explorer" }),
                                        color = NexoraColors.StarWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                    )
                                    Button(onClick = {
                                        roomRef.collection("members").document(m.uid).update(
                                            mapOf<String, Any>(
                                                "canSpeak" to true,
                                                "role" to "speaker",
                                                "handRaised" to false,
                                                "muted" to false,
                                            ),
                                        )
                                    }) { Text("Allow") }
                                }
                            }
                        }

                        item { NxSectionLabel("Speakers (${speakers.size})") }
                        items(speakers, key = { "spk_" + it.uid }) { m ->
                            PersonRow(
                                member = m,
                                isMe = m.uid == myUid,
                                showMenu = canManage,
                                onAllow = { },
                                onRevoke = {
                                    roomRef.collection("members").document(m.uid).update(
                                        mapOf<String, Any>(
                                            "canSpeak" to false,
                                            "role" to "listener",
                                            "micOn" to false,
                                            "muted" to false,
                                        ),
                                    )
                                },
                                onMute = {
                                    roomRef.collection("members").document(m.uid).update(
                                        mapOf<String, Any>("muted" to true, "micOn" to false),
                                    )
                                },
                                onUnmute = {
                                    roomRef.collection("members").document(m.uid).update("muted", false)
                                },
                                onRemove = { removeMember(db, roomId, m.uid) },
                            )
                        }

                        val shownListeners = listeners.size.toLong()
                        val totalListeners = (r.memberCount - speakers.size).coerceAtLeast(shownListeners)
                        item {
                            NxSectionLabel(
                                if (totalListeners > shownListeners) {
                                    "Listeners (showing $shownListeners of ${formatCount(totalListeners)})"
                                } else {
                                    "Listeners ($shownListeners)"
                                },
                            )
                        }
                        items(listeners, key = { "lis_" + it.uid }) { m ->
                            PersonRow(
                                member = m,
                                isMe = m.uid == myUid,
                                showMenu = canManage,
                                onAllow = {
                                    roomRef.collection("members").document(m.uid).update(
                                        mapOf<String, Any>(
                                            "canSpeak" to true,
                                            "role" to "speaker",
                                            "handRaised" to false,
                                            "muted" to false,
                                        ),
                                    )
                                },
                                onRevoke = { },
                                onMute = { },
                                onUnmute = { },
                                onRemove = { removeMember(db, roomId, m.uid) },
                            )
                        }
                        item { Spacer(Modifier.height(12.dp)) }
                    }
                } else {
                    // ---- Chat ----
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (messages.isEmpty()) {
                            item { NxEmpty("💬", "No messages yet. Say hello!") }
                        }
                        items(messages, key = { it.id }) { msg ->
                            Row(verticalAlignment = Alignment.Top) {
                                SmallAvatar(msg.uid, msg.name, 32)
                                Column(modifier = Modifier.padding(start = 10.dp)) {
                                    Text(
                                        AnonymousMode.shownName(msg.uid, msg.name.ifBlank { "Explorer" }),
                                        color = NexoraColors.PlasmaCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(msg.text, color = NexoraColors.StarWhite, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it.take(500) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Message the room") },
                            maxLines = 3,
                            shape = RoundedCornerShape(22.dp),
                        )
                        IconButton(
                            enabled = draft.isNotBlank(),
                            onClick = {
                                val text = draft.trim()
                                if (text.isNotEmpty()) {
                                    draft = ""
                                    roomRef.collection("messages").add(
                                        hashMapOf<String, Any>(
                                            "uid" to myUid,
                                            "name" to myDisplayName(context),
                                            "text" to text,
                                            "time" to System.currentTimeMillis(),
                                        ),
                                    )
                                }
                            },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (draft.isNotBlank()) NexoraColors.PlasmaCyan else NexoraColors.Muted.copy(alpha = 0.4f),
                            )
                        }
                    }
                }

                // ---- Bottom bar: Leave + raise hand / mic ----
                val mine = me
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexoraColors.Panel)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.weight(1f),
                    ) { Text("Leave", color = DangerRed) }

                    if (mine != null && mine.canSpeak) {
                        if (mine.muted) {
                            OutlinedButton(onClick = { }, enabled = false, modifier = Modifier.weight(1f)) {
                                Text("Muted by admin")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val next = !mine.micOn
                                    memberRef.update("micOn", next)
                                    NexusVoice.engine?.setMicOn(next)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(if (mine.micOn) "Mic on" else "Mic off") }
                        }
                    } else if (mine != null) {
                        OutlinedButton(
                            onClick = { memberRef.update("handRaised", !mine.handRaised) },
                            modifier = Modifier.weight(1f),
                        ) { Text(if (mine.handRaised) "Lower hand" else "Raise hand ✋") }
                    }
                }
            }
        }
    }

    if (endConfirm) {
        AlertDialog(
            onDismissRequest = { endConfirm = false },
            title = { Text("End this room?") },
            text = { Text("Everyone will be removed and the room will close.") },
            confirmButton = {
                TextButton(onClick = {
                    endConfirm = false
                    roomRef.update("status", "ended")
                }) { Text("End room", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { endConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
