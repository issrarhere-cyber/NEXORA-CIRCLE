package com.nexora.circle.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private val EMOJIS: List<String> = (
    "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 😉 😍 🥰 😘 😋 " +
        "😎 🤩 🥳 😏 😒 😞 😔 😢 😭 😤 😡 🤯 😱 😴 🤔 🤗 " +
        "🙄 😬 🤐 😷 🤒 👍 👎 👏 🙌 🙏 💪 👋 🤝 ✌️ 🤞 👌 " +
        "❤️ 🧡 💛 💚 💙 💜 🖤 💔 🔥 ✨ 🎉 💯 👀 🚀 🌙 ⭐ 👽 🛸"
    ).split(" ")

// ---------- Chat options stored on this phone (pin, hide, lock, block, best friend, theme) ----------

private val CHAT_THEMES: List<Pair<String, Color>> = listOf(
    "cyan" to Color(0xFF00E5FF),
    "pink" to Color(0xFFFF4DA6),
    "purple" to Color(0xFFA855F7),
    "green" to Color(0xFF22C55E),
    "orange" to Color(0xFFFF9800),
)

private fun themeColor(key: String): Color = when (key) {
    "cyan" -> NexoraColors.PlasmaCyan
    else -> CHAT_THEMES.firstOrNull { it.first == key }?.second ?: NexoraColors.PlasmaCyan
}

private class ChatPrefs(context: Context) {
    private val p = context.getSharedPreferences("nexora_chat_prefs", Context.MODE_PRIVATE)

    private fun load(key: String): SnapshotStateList<String> =
        mutableStateListOf<String>().apply { addAll(p.getStringSet(key, emptySet()) ?: emptySet()) }

    val pinned = load("pinned")
    val hidden = load("hidden")
    val locked = load("locked")
    val blocked = load("blocked")
    val bff = load("bff")

    fun toggle(list: SnapshotStateList<String>, key: String, id: String) {
        if (id in list) list.remove(id) else list.add(id)
        p.edit().putStringSet(key, list.toSet()).apply()
    }

    fun theme(chatId: String): String = p.getString("theme_$chatId", "cyan") ?: "cyan"

    fun setTheme(chatId: String, key: String) {
        p.edit().putString("theme_$chatId", key).apply()
    }

    fun hasPin(): Boolean = !p.getString("pin", null).isNullOrEmpty()

    fun setPin(pin: String) {
        p.edit().putString("pin", pin.hashCode().toString()).apply()
    }

    fun checkPin(pin: String): Boolean = p.getString("pin", null) == pin.hashCode().toString()
}

@Composable
private fun PinDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    // Return an error message, or null when the PIN was accepted.
    onSubmit: (String) -> String?,
) {
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter { c -> c.isDigit() }.take(6)
                        err = ""
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    placeholder = { Text("4-6 digit PIN") },
                )
                if (err.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = NexoraColors.NebulaPink)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = onSubmit(pin)
                if (e != null) err = e
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
    onOpenProfile: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val prefs = remember { ChatPrefs(context) }
    val friendIds = remember { mutableStateListOf<String>() }
    val names = remember { mutableStateMapOf<String, String>() }
    var selectedId by remember { mutableStateOf(startUid) }
    var unlockedId by remember { mutableStateOf<String?>(null) }
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
            prefs = prefs,
            loadError = loadError,
            onOpen = { selectedId = it.id },
            onSendSignal = onSendSignal,
        )
    } else if (current in prefs.locked && prefs.hasPin() && unlockedId != current) {
        // Locked chat: ask for the PIN before showing anything.
        Box(Modifier.fillMaxSize())
        PinDialog(
            title = "Enter PIN to open this chat",
            confirmLabel = "Unlock",
            onDismiss = { selectedId = null },
            onSubmit = { pin ->
                if (prefs.checkPin(pin)) {
                    unlockedId = current
                    null
                } else {
                    "Wrong PIN"
                }
            },
        )
    } else {
        ChatView(
            myUid = myUid,
            friend = Friend(current, names[current] ?: "Explorer"),
            prefs = prefs,
            onBack = {
                selectedId = null
                unlockedId = null
            },
            onOpenProfile = onOpenProfile,
            onLockedHere = { unlockedId = current },
        )
    }
}

@Composable
private fun FriendsList(
    myUid: String,
    friends: List<Friend>,
    prefs: ChatPrefs,
    loadError: String,
    onOpen: (Friend) -> Unit,
    onSendSignal: () -> Unit,
) {
    val db = remember { FirebaseFirestore.getInstance() }
    var showHidden by remember { mutableStateOf(false) }
    var askPin by remember { mutableStateOf(false) }

    val shown = friends
        .filter { (it.id in prefs.hidden) == showHidden }
        .sortedByDescending { it.id in prefs.pinned }

    if (askPin) {
        PinDialog(
            title = "Enter PIN to see hidden chats",
            confirmLabel = "Open",
            onDismiss = { askPin = false },
            onSubmit = { pin ->
                if (prefs.checkPin(pin)) {
                    askPin = false
                    showHidden = true
                    null
                } else {
                    "Wrong PIN"
                }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (showHidden) "Hidden chats" else "Comms",
            style = MaterialTheme.typography.headlineMedium,
            color = NexoraColors.StarWhite,
        )

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
                if (showHidden) {
                    item {
                        Text(
                            "← Back to chats",
                            color = NexoraColors.PlasmaCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showHidden = false }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
                items(shown, key = { it.id }) { f ->
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

                    val isLocked = f.id in prefs.locked
                    val isBlocked = f.id in prefs.blocked

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    f.name,
                                    color = NexoraColors.StarWhite,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (f.id in prefs.bff) Text(" 💛")
                                if (f.id in prefs.pinned) Text(" 📌")
                                if (isLocked) Text(" 🔒")
                            }
                            Text(
                                when {
                                    isBlocked -> "Blocked"
                                    isLocked -> "Locked chat"
                                    else -> last?.text ?: "Tap to start chatting"
                                },
                                color = NexoraColors.Muted,
                                maxLines = 1,
                            )
                        }
                        val t = formatTime(last?.time ?: 0L)
                        if (t.isNotEmpty() && !isLocked) {
                            Text(t, style = MaterialTheme.typography.labelSmall, color = NexoraColors.Muted)
                        }
                    }
                }
                if (!showHidden && prefs.hidden.isNotEmpty()) {
                    item {
                        Text(
                            "Hidden chats (${prefs.hidden.size})",
                            color = NexoraColors.Muted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (prefs.hasPin()) askPin = true else showHidden = true
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatView(
    myUid: String,
    friend: Friend,
    prefs: ChatPrefs,
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onLockedHere: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val db = remember { FirebaseFirestore.getInstance() }
    val chatId = remember(friend.id) { chatIdOf(myUid, friend.id) }
    val messages = remember(chatId) { mutableStateListOf<ChatMessage>() }
    var input by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var showEmoji by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showBlock by remember { mutableStateOf(false) }
    var showSetPin by remember { mutableStateOf(false) }
    var selectedMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var themeKey by remember(chatId) { mutableStateOf(prefs.theme(chatId)) }
    val accent = themeColor(themeKey)
    val isBlocked = friend.id in prefs.blocked
    val listState = rememberLazyListState()

    // Messages I removed "for me" (kept on this phone only).
    val hiddenMsgs = remember(chatId) {
        mutableStateListOf<String>().apply {
            addAll(
                context.getSharedPreferences("nexora_hidden", Context.MODE_PRIVATE)
                    .getStringSet("h_$chatId", emptySet()) ?: emptySet()
            )
        }
    }

    fun saveHidden() {
        context.getSharedPreferences("nexora_hidden", Context.MODE_PRIVATE)
            .edit().putStringSet("h_$chatId", hiddenMsgs.toSet()).apply()
    }

    fun hideMessage(id: String) {
        if (id !in hiddenMsgs) {
            hiddenMsgs.add(id)
            saveHidden()
        }
    }

    val visible = messages.filter {
        it.id !in hiddenMsgs && !(isBlocked && it.senderUid == friend.id)
    }

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
                        friend.id !in prefs.blocked && fresh.size > messages.size
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
    LaunchedEffect(visible.size) {
        if (visible.isNotEmpty()) listState.animateScrollToItem(visible.size - 1)
    }

    // Keyboard opens/closes -> stay on the last message so it is not hidden.
    LaunchedEffect(imeBottom) {
        if (visible.isNotEmpty()) listState.scrollToItem(visible.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        if (isBlocked) return
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

    // ----- Dialogs -----

    selectedMsg?.let { m ->
        val mine = m.senderUid == myUid
        AlertDialog(
            onDismissRequest = { selectedMsg = null },
            title = { Text("Delete message?") },
            text = {
                Text(
                    if (mine) "This will delete the message for everyone."
                    else "This will remove the message from your phone only."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedMsg = null
                    if (mine) {
                        db.collection("chats").document(chatId).collection("messages")
                            .document(m.id).delete()
                            .addOnFailureListener { e ->
                                hideMessage(m.id)
                                errorMsg = "Could not delete for everyone (${e.localizedMessage}). Hidden on your phone."
                            }
                    } else {
                        hideMessage(m.id)
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { selectedMsg = null }) { Text("Cancel") } },
        )
    }

    if (showTheme) {
        AlertDialog(
            onDismissRequest = { showTheme = false },
            title = { Text("Chat theme") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CHAT_THEMES.forEach { (key, color) ->
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    themeKey = key
                                    prefs.setTheme(chatId, key)
                                    showTheme = false
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (themeKey == key) {
                                Text("✓", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTheme = false }) { Text("Close") } },
        )
    }

    if (showBlock) {
        AlertDialog(
            onDismissRequest = { showBlock = false },
            title = { Text("Block ${friend.name}?") },
            text = {
                Text("You will not see their messages and you cannot send them any. This works on your phone only.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showBlock = false
                    prefs.toggle(prefs.blocked, "blocked", friend.id)
                }) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showBlock = false }) { Text("Cancel") } },
        )
    }

    if (showSetPin) {
        PinDialog(
            title = "Set a PIN to lock chats",
            confirmLabel = "Set PIN",
            onDismiss = { showSetPin = false },
            onSubmit = { pin ->
                if (pin.length < 4) {
                    "Use at least 4 digits"
                } else {
                    prefs.setPin(pin)
                    prefs.toggle(prefs.locked, "locked", friend.id)
                    onLockedHere()
                    showSetPin = false
                    null
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(accent.copy(alpha = 0.05f))
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ----- Top bar: back, profile pic + name (tap = open profile), menu -----
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←",
                style = MaterialTheme.typography.headlineMedium,
                color = NexoraColors.StarWhite,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenProfile(friend.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        friend.name.take(1).uppercase(),
                        color = NexoraColors.SpaceBlack,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    friend.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = NexoraColors.StarWhite,
                    maxLines = 1,
                )
            }
            Box {
                Text(
                    "⋮",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NexoraColors.StarWhite,
                    modifier = Modifier.clickable { menuOpen = true }.padding(horizontal = 8.dp),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (friend.id in prefs.pinned) "Unpin chat" else "Pin chat") },
                        onClick = {
                            menuOpen = false
                            prefs.toggle(prefs.pinned, "pinned", friend.id)
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (friend.id in prefs.bff) "Remove best friend 💛" else "Best friend 💛")
                        },
                        onClick = {
                            menuOpen = false
                            prefs.toggle(prefs.bff, "bff", friend.id)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Chat theme") },
                        onClick = {
                            menuOpen = false
                            showTheme = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (friend.id in prefs.locked) "Unlock chat" else "Lock chat") },
                        onClick = {
                            menuOpen = false
                            if (friend.id in prefs.locked) {
                                prefs.toggle(prefs.locked, "locked", friend.id)
                            } else if (!prefs.hasPin()) {
                                showSetPin = true
                            } else {
                                prefs.toggle(prefs.locked, "locked", friend.id)
                                onLockedHere()
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (friend.id in prefs.hidden) "Unhide chat" else "Hide chat") },
                        onClick = {
                            menuOpen = false
                            val wasHidden = friend.id in prefs.hidden
                            prefs.toggle(prefs.hidden, "hidden", friend.id)
                            if (!wasHidden) onBack()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (isBlocked) "Unblock" else "Block") },
                        onClick = {
                            menuOpen = false
                            if (isBlocked) {
                                prefs.toggle(prefs.blocked, "blocked", friend.id)
                            } else {
                                showBlock = true
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Clear chat (for me)") },
                        onClick = {
                            menuOpen = false
                            messages.forEach { if (it.id !in hiddenMsgs) hiddenMsgs.add(it.id) }
                            saveHidden()
                        },
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visible.isEmpty()) {
                item { Text("Say hi to ${friend.name} 👋", color = NexoraColors.Muted) }
            }
            items(visible, key = { it.id }) { msg ->
                val mine = msg.senderUid == myUid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .pointerInput(msg.id) {
                                detectTapGestures(onLongPress = { selectedMsg = msg })
                            }
                            .background(
                                if (mine) accent.copy(alpha = 0.22f) else NexoraColors.Panel,
                                RoundedCornerShape(16.dp),
                            )
                            .padding(12.dp),
                    ) {
                        if (!mine && msg.senderName.isNotBlank()) {
                            Text(
                                msg.senderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
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

        if (isBlocked) {
            Text(
                "You blocked ${friend.name}. Open the ⋮ menu and tap Unblock to chat again.",
                color = NexoraColors.Muted,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        } else {
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
                    Box(
                        Modifier.clickable {
                            showEmoji = !showEmoji
                            if (showEmoji) focusManager.clearFocus()
                        }
                    ) {
                        BarIcon(BarKind.Emoji, if (showEmoji) accent else NexoraColors.Muted)
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
                            cursorBrush = SolidColor(accent),
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) showEmoji = false },
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
                            .background(accent)
                            .clickable { toast(context) },
                        contentAlignment = Alignment.Center,
                    ) {
                        BarIcon(BarKind.Mic, NexoraColors.SpaceBlack, 26.dp)
                    }
                }
            }

            // ----- Emoji panel (opens under the message box) -----
            if (showEmoji) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(NexoraColors.Panel, RoundedCornerShape(16.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                ) {
                    EMOJIS.chunked(8).forEach { rowEmojis ->
                        Row(Modifier.fillMaxWidth()) {
                            rowEmojis.forEach { e ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { input = (input + e).take(500) }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(e, fontSize = 26.sp)
                                }
                            }
                            repeat(8 - rowEmojis.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}
