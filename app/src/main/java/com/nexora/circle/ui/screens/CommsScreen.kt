package com.nexora.circle.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.nexora.circle.R
import com.nexora.circle.ui.theme.NexoraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ChatMessage(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val text: String,
    val time: Long,
    val delivered: Boolean = false,
    val seen: Boolean = false,
    val pending: Boolean = false,
)

private data class Friend(val id: String, val name: String)

private fun chatIdOf(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")

private fun docToMessage(d: com.google.firebase.firestore.DocumentSnapshot): ChatMessage =
    ChatMessage(
        id = d.id,
        senderUid = d.getString("senderUid") ?: "",
        senderName = d.getString("senderName") ?: "",
        text = d.getString("text") ?: "",
        time = d.getLong("time") ?: 0L,
        delivered = d.getBoolean("delivered") == true,
        seen = d.getBoolean("seen") == true,
        pending = d.metadata.hasPendingWrites(),
    )

// Kept so other screens that call it still compile. Friends now come from Firebase.
@Suppress("UNUSED_PARAMETER")
fun addFriendLocal(context: Context, id: String, name: String) {
}

private fun myDisplayName(context: Context): String {
    val p = context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE)
    val name = p.getString("name", "") ?: ""
    val user = p.getString("username", "") ?: ""
    val handle = if (user.isNotBlank()) "X.$user" else ""
    return listOf(name, handle).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun formatTime(time: Long): String =
    if (time == 0L) "" else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(time))

private fun toast(context: Context, text: String = "Coming soon") {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

// Plays res/raw/message_tone.* if you added it, otherwise the phone's default notification sound.
private fun playMessageTone(context: Context) {
    try {
        val resId = context.resources.getIdentifier("message_tone", "raw", context.packageName)
        if (resId != 0) {
            val mp = MediaPlayer.create(context, resId)
            if (mp != null) {
                mp.setOnCompletionListener { it.release() }
                mp.start()
                return
            }
        }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    } catch (e: Exception) {
        // Sound is optional; never crash the chat because of it.
    }
}

// Plays res/raw/send_tone.* when I send a message. If the file is not added, stays silent.
private fun playSendTone(context: Context) {
    try {
        val resId = context.resources.getIdentifier("send_tone", "raw", context.packageName)
        if (resId != 0) {
            val mp = MediaPlayer.create(context, resId)
            if (mp != null) {
                mp.setOnCompletionListener { it.release() }
                mp.start()
            }
        }
    } catch (e: Exception) {
        // Sound is optional; never crash the chat because of it.
    }
}

// ---------- Icons ----------

private enum class BarKind { Emoji, Attach, Camera, Mic }

@Composable
private fun BarIcon(kind: BarKind, tint: Color, iconSize: Dp = 24.dp) {
    Canvas(Modifier.size(iconSize)) {
        val w = size.width
        val sw = w * 0.085f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            BarKind.Emoji -> {
                drawCircle(tint, radius = 0.4f * w, center = Offset(0.5f * w, 0.5f * w), style = stroke)
                drawCircle(tint, radius = 0.045f * w, center = Offset(0.38f * w, 0.42f * w))
                drawCircle(tint, radius = 0.045f * w, center = Offset(0.62f * w, 0.42f * w))
                drawArc(
                    color = tint,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(0.3f * w, 0.3f * w),
                    size = Size(0.4f * w, 0.4f * w),
                    style = stroke,
                )
            }
            BarKind.Attach -> {
                val p = Path().apply {
                    moveTo(0.70f * w, 0.30f * w)
                    lineTo(0.70f * w, 0.68f * w)
                    cubicTo(0.70f * w, 0.90f * w, 0.30f * w, 0.90f * w, 0.30f * w, 0.68f * w)
                    lineTo(0.30f * w, 0.26f * w)
                    cubicTo(0.30f * w, 0.10f * w, 0.58f * w, 0.10f * w, 0.58f * w, 0.26f * w)
                    lineTo(0.58f * w, 0.64f * w)
                }
                drawPath(p, tint, style = stroke)
            }
            BarKind.Camera -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.10f * w, 0.26f * w),
                    size = Size(0.80f * w, 0.56f * w),
                    cornerRadius = CornerRadius(0.12f * w),
                    style = stroke,
                )
                drawCircle(tint, radius = 0.14f * w, center = Offset(0.5f * w, 0.54f * w), style = stroke)
                drawLine(tint, Offset(0.38f * w, 0.18f * w), Offset(0.62f * w, 0.18f * w), sw, StrokeCap.Round)
            }
            BarKind.Mic -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0.38f * w, 0.10f * w),
                    size = Size(0.24f * w, 0.46f * w),
                    cornerRadius = CornerRadius(0.12f * w),
                )
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(0.24f * w, 0.30f * w),
                    size = Size(0.52f * w, 0.44f * w),
                    style = stroke,
                )
                drawLine(tint, Offset(0.5f * w, 0.74f * w), Offset(0.5f * w, 0.88f * w), sw, StrokeCap.Round)
                drawLine(tint, Offset(0.36f * w, 0.88f * w), Offset(0.64f * w, 0.88f * w), sw, StrokeCap.Round)
            }
        }
    }
}

// ---------- Message status (clock / sent / delivered / seen) ----------

private enum class MsgStatus { Pending, Sent, Delivered, Seen }

private fun statusOf(m: ChatMessage): MsgStatus = when {
    m.seen -> MsgStatus.Seen
    m.delivered -> MsgStatus.Delivered
    m.pending -> MsgStatus.Pending
    else -> MsgStatus.Sent
}

@Composable
private fun StatusTicks(status: MsgStatus) {
    val color = when (status) {
        MsgStatus.Seen -> NexoraColors.NebulaPink
        else -> NexoraColors.Muted
    }
    val double = status == MsgStatus.Delivered || status == MsgStatus.Seen
    Canvas(Modifier.size(width = 20.dp, height = 12.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = h * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // Pending: small clock icon
        if (status == MsgStatus.Pending) {
            val c = Offset(w * 0.5f, h * 0.5f)
            val r = h * 0.45f
            drawCircle(color, radius = r, center = c, style = stroke)
            drawLine(color, c, Offset(c.x, c.y - r * 0.6f), h * 0.14f, StrokeCap.Round)
            drawLine(color, c, Offset(c.x + r * 0.5f, c.y), h * 0.14f, StrokeCap.Round)
            return@Canvas
        }

        fun tick(dx: Float) {
            val p = Path().apply {
                moveTo(dx, 0.55f * h)
                lineTo(dx + 0.2f * w, 0.85f * h)
                lineTo(dx + 0.62f * w, 0.1f * h)
            }
            drawPath(p, color, style = stroke)
        }
        if (double) {
            tick(0f)
            tick(0.3f * w)
        } else {
            tick(0.15f * w)
        }
    }
}

// ---------- Screens ----------

@Composable
fun CommsScreen(
    onSendSignal: () -> Unit = {},
    startUid: String? = null,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val friendIds = remember { mutableStateListOf<String>() }
    val names = remember { mutableStateMapOf<String, String>() }
    var selectedId by remember { mutableStateOf(startUid) }
    var loadError by remember { mutableStateOf("") }

    LaunchedEffect(startUid) {
        if (startUid != null) selectedId = startUid
    }

    fun fetchName(uid: String) {
        if (names.containsKey(uid)) return
        db.collection("users").document(uid).get().addOnSuccessListener { s ->
            names[uid] = s.getString("name")?.ifBlank { null } ?: "Explorer"
        }
    }

    DisposableEffect(myUid) {
        if (myUid.isEmpty()) return@DisposableEffect onDispose { }
        val reg = db.collection("users").document(myUid).collection("friends")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    loadError = "Could not load friends: ${err.localizedMessage}"
                    return@addSnapshotListener
                }
                loadError = ""
                val ids = snap?.documents?.map { it.id } ?: emptyList()
                friendIds.clear()
                friendIds.addAll(ids)
                ids.forEach { fetchName(it) }
            }
        onDispose { reg.remove() }
    }

    LaunchedEffect(selectedId) {
        selectedId?.let { fetchName(it) }
    }

    val current = selectedId
    if (current == null) {
        FriendsList(
            myUid = myUid,
            friends = friendIds.map { Friend(it, names[it] ?: "Explorer") },
            loadError = loadError,
            onOpen = { selectedId = it.id },
            onSendSignal = onSendSignal,
        )
    } else {
        ChatView(
            myUid = myUid,
            friend = Friend(current, names[current] ?: "Explorer"),
            onBack = { selectedId = null },
        )
    }
}

@Composable
private fun FriendsList(
    myUid: String,
    friends: List<Friend>,
    loadError: String,
    onOpen: (Friend) -> Unit,
    onSendSignal: () -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Comms", style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)

        if (loadError.isNotEmpty()) {
            Text(loadError, color = NexoraColors.NebulaPink)
        }

        if (friends.isEmpty()) {
            Text("No signals yet... 👽", color = NexoraColors.Muted)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(NexoraColors.PlasmaCyan)
                    .clickable { onSendSignal() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Send Signal ➤", color = NexoraColors.SpaceBlack, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(friends, key = { it.id }) { f ->
                    var last by remember(f.id) { mutableStateOf<ChatMessage?>(null) }
                    DisposableEffect(f.id) {
                        val reg = db.collection("chats").document(chatIdOf(myUid, f.id))
                            .collection("messages")
                            .orderBy("time", Query.Direction.DESCENDING)
                            .limit(1)
                            .addSnapshotListener { s, _ ->
                                val d = s?.documents?.firstOrNull()
                                last = d?.let { docToMessage(it) }
                                // Friend's message reached my phone -> mark Delivered.
                                if (d != null && d.getString("toUid") == myUid &&
                                    d.getBoolean("delivered") != true &&
                                    !d.metadata.hasPendingWrites()
                                ) {
                                    d.reference.update("delivered", true)
                                }
                            }
                        onDispose { reg.remove() }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NexoraColors.Panel, RoundedCornerShape(16.dp))
                            .clickable { onOpen(f) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NexoraColors.PlasmaCyan),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                f.name.take(1).uppercase(),
                                color = NexoraColors.SpaceBlack,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, color = NexoraColors.StarWhite, fontWeight = FontWeight.Bold)
                            Text(
                                last?.text ?: "Tap to start chatting",
                                color = NexoraColors.Muted,
                                maxLines = 1,
                            )
                        }
                        val t = formatTime(last?.time ?: 0L)
                        if (t.isNotEmpty()) {
                            Text(t, style = MaterialTheme.typography.labelSmall, color = NexoraColors.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatView(myUid: String, friend: Friend, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val chatId = remember(friend.id) { chatIdOf(myUid, friend.id) }
    val messages = remember(chatId) { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Keyboard height. Changes while the keyboard opens/closes.
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    DisposableEffect(chatId) {
        var firstLoad = true
        val reg = db.collection("chats").document(chatId).collection("messages")
            .orderBy("time", Query.Direction.ASCENDING)
            // INCLUDE metadata changes so the clock icon turns into a tick once the server confirms.
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
                if (err != null) {
                    errorMsg = "Load failed: ${err.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snap != null) {
                    val fresh = snap.documents.map { docToMessage(it) }.filter { it.text.isNotBlank() }
                    val newest = fresh.lastOrNull()
                    // Sound only for a NEW message from the friend (not on first load, not for my own).
                    if (!firstLoad && newest != null && newest.senderUid != myUid &&
                        fresh.size > messages.size
                    ) {
                        playMessageTone(context)
                    }
                    firstLoad = false
                    // Chat is open on my phone -> friend's messages are Delivered and Seen.
                    snap.documents.forEach { d ->
                        if (d.getString("toUid") == myUid &&
                            d.getBoolean("seen") != true &&
                            !d.metadata.hasPendingWrites()
                        ) {
                            d.reference.update(mapOf("delivered" to true, "seen" to true))
                        }
                    }
                    messages.clear()
                    messages.addAll(fresh)
                }
            }
        onDispose { reg.remove() }
    }

    // New message -> scroll to the bottom.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Keyboard opens/closes -> stay on the last message so it is not hidden.
    LaunchedEffect(imeBottom) {
        if (messages.isNotEmpty()) listState.scrollToItem(messages.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        if (myUid.isEmpty()) {
            errorMsg = "You are not signed in."
            return
        }
        errorMsg = ""
        input = ""
        playSendTone(context)
        db.collection("chats").document(chatId).collection("messages").add(
            hashMapOf(
                "senderUid" to myUid,
                "senderName" to myDisplayName(context),
                "toUid" to friend.id,
                "text" to text,
                "time" to System.currentTimeMillis(),
                "delivered" to false,
                "seen" to false,
            )
        ).addOnFailureListener { e ->
            errorMsg = "Send failed: ${e.localizedMessage}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←",
                style = MaterialTheme.typography.headlineMedium,
                color = NexoraColors.StarWhite,
                modifier = Modifier.clickable { onBack() }.padding(end = 16.dp),
            )
            Text(friend.name, style = MaterialTheme.typography.headlineSmall, color = NexoraColors.StarWhite)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) {
                item { Text("Say hi to ${friend.name} 👋", color = NexoraColors.Muted) }
            }
            items(messages, key = { it.id }) { msg ->
                val mine = msg.senderUid == myUid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(
                                if (mine) NexoraColors.PlasmaCyan.copy(alpha = 0.18f)
                                else NexoraColors.Panel,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(12.dp),
                    ) {
                        if (!mine && msg.senderName.isNotBlank()) {
                            Text(
                                msg.senderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = NexoraColors.PlasmaCyan,
                            )
                        }
                        Text(msg.text, color = NexoraColors.StarWhite)
                        val t = formatTime(msg.time)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (t.isNotEmpty()) {
                                Text(t, style = MaterialTheme.typography.labelSmall, color = NexoraColors.Muted)
                            }
                            if (mine) StatusTicks(statusOf(msg))
                        }
                    }
                }
            }
        }

        if (errorMsg.isNotEmpty()) {
            Text(errorMsg, color = NexoraColors.NebulaPink)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(NexoraColors.Panel, RoundedCornerShape(28.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.clickable { toast(context, "Use your keyboard's emoji key") }) {
                    BarIcon(BarKind.Emoji, NexoraColors.Muted)
                }

                Box(Modifier.weight(1f)) {
                    if (input.isEmpty()) {
                        Text(
                            "Message",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NexoraColors.Muted,
                        )
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it.take(500) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = NexoraColors.StarWhite),
                        cursorBrush = SolidColor(NexoraColors.PlasmaCyan),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Box(Modifier.clickable { toast(context) }) {
                    BarIcon(BarKind.Attach, NexoraColors.Muted)
                }
                if (input.isBlank()) {
                    Box(Modifier.clickable { toast(context) }) {
                        BarIcon(BarKind.Camera, NexoraColors.Muted)
                    }
                }
            }

            if (input.isNotBlank()) {
                Image(
                    painter = painterResource(R.drawable.ic_send_pink),
                    contentDescription = "Send",
                    modifier = Modifier
                        .width(62.dp)
                        .height(42.dp)
                        .clickable { send() },
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(NexoraColors.PlasmaCyan)
                        .clickable { toast(context) },
                    contentAlignment = Alignment.Center,
                ) {
                    BarIcon(BarKind.Mic, NexoraColors.SpaceBlack, 26.dp)
                }
            }
        }
    }
}
