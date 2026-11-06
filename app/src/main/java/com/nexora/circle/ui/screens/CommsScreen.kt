package com.nexora.circle.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nexora.circle.R
import com.nexora.circle.ui.theme.NexoraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private const val CHAT_PREFS = "nexora_comms"
private const val KEY_FRIENDS = "friends"

private data class ChatMessage(
    val sender: String,
    val text: String,
    val time: Long,
)

private data class Friend(val id: String, val name: String)

// ---------- Friends storage ----------

private fun loadFriends(context: Context): List<Friend> {
    val prefs = context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_FRIENDS, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<Friend>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Friend(o.optString("id", ""), o.optString("name", "")))
        }
        list.filter { it.id.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveFriends(context: Context, friends: List<Friend>) {
    val arr = JSONArray()
    friends.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name)) }
    context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_FRIENDS, arr.toString()).apply()
}

// Public: SearchScreen se friend add karne ke liye (abhi local, baad me Firebase)
fun addFriendLocal(context: Context, id: String, name: String) {
    val current = loadFriends(context).toMutableList()
    if (current.none { it.id == id }) {
        current.add(Friend(id, name))
        saveFriends(context, current)
    }
}

// ---------- Messages storage (har friend ki alag chat) ----------

private fun loadMessages(context: Context, friendId: String): List<ChatMessage> {
    val prefs = context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString("messages_$friendId", null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val msg = ChatMessage(
                sender = item.optString("sender", ""),
                text = item.optString("text", ""),
                time = item.optLong("time", 0L),
            )
            if (msg.text.isNotBlank()) list.add(msg)
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveMessages(context: Context, friendId: String, messages: List<ChatMessage>) {
    val arr = JSONArray()
    messages.forEach {
        arr.put(
            JSONObject()
                .put("sender", it.sender)
                .put("text", it.text)
                .put("time", it.time)
        )
    }
    context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
        .edit().putString("messages_$friendId", arr.toString()).apply()
}

private fun senderLabel(context: Context): String {
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

// ---------- Screens ----------

@Composable
fun CommsScreen(onSendSignal: () -> Unit = {}) {
    val context = LocalContext.current
    val friends = remember { mutableStateListOf<Friend>().apply { addAll(loadFriends(context)) } }
    var selected by remember { mutableStateOf<Friend?>(null) }

    val current = selected
    if (current == null) {
        FriendsList(
            friends = friends,
            onOpen = { selected = it },
            onSendSignal = onSendSignal,
        )
    } else {
        ChatView(friend = current, onBack = { selected = null })
    }
}

@Composable
private fun FriendsList(
    friends: List<Friend>,
    onOpen: (Friend) -> Unit,
    onSendSignal: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Comms", style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)

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
            // chat wale friends upar, baaki neeche
            val rows = friends
                .map { f -> f to loadMessages(context, f.id).lastOrNull() }
                .sortedByDescending { it.second?.time ?: 0L }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows) { (f, last) ->
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
private fun ChatView(friend: Friend, onBack: () -> Unit) {
    val context = LocalContext.current
    val messages = remember(friend.id) {
        mutableStateListOf<ChatMessage>().apply { addAll(loadMessages(context, friend.id)) }
    }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun send() {
        val text = input.trim()
        if (text.isNotEmpty()) {
            messages.add(ChatMessage(senderLabel(context), text, System.currentTimeMillis()))
            saveMessages(context, friend.id, messages)
            input = ""
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
            items(messages) { msg ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexoraColors.Panel, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    if (msg.sender.isNotBlank()) {
                        Text(
                            msg.sender,
                            style = MaterialTheme.typography.labelMedium,
                            color = NexoraColors.PlasmaCyan,
                        )
                    }
                    Text(msg.text, color = NexoraColors.StarWhite)
                    val t = formatTime(msg.time)
                    if (t.isNotEmpty()) {
                        Text(t, style = MaterialTheme.typography.labelSmall, color = NexoraColors.Muted)
                    }
                }
            }
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
                // Pink send icon
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
