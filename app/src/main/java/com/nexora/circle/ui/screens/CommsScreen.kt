package com.nexora.circle.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.theme.NexoraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private const val CHAT_PREFS = "nexora_comms"
private const val KEY_MESSAGES = "messages"

private data class ChatMessage(
    val sender: String,
    val text: String,
    val time: Long,
)

private fun loadMessages(context: Context): List<ChatMessage> {
    val prefs = context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<ChatMessage>()
        for (i in 0 until arr.length()) {
            val item = arr.get(i)
            val msg = if (item is JSONObject) {
                ChatMessage(
                    sender = item.optString("sender", ""),
                    text = item.optString("text", ""),
                    time = item.optLong("time", 0L),
                )
            } else {
                // old format: plain text
                ChatMessage("", item.toString(), 0L)
            }
            if (msg.text.isNotBlank()) list.add(msg)
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveMessages(context: Context, messages: List<ChatMessage>) {
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
        .edit().putString(KEY_MESSAGES, arr.toString()).apply()
}

// Reads the saved profile and builds "Name · X.username"
private fun senderLabel(context: Context): String {
    val p = context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE)
    val name = p.getString("name", "") ?: ""
    val user = p.getString("username", "") ?: ""
    val handle = if (user.isNotBlank()) "X.$user" else ""
    return listOf(name, handle).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun formatTime(time: Long): String =
    if (time == 0L) "" else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(time))

@Composable
fun CommsScreen() {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(loadMessages(context)) } }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Comms", style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (messages.isEmpty()) {
                item { Text("No signals yet... 👽", color = NexoraColors.Muted) }
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
                        Text(
                            t,
                            style = MaterialTheme.typography.labelSmall,
                            color = NexoraColors.Muted,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(500) },
                label = { Text("Message") },
                textStyle = TextStyle(color = NexoraColors.StarWhite),
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = input.isNotBlank(),
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        messages.add(ChatMessage(senderLabel(context), text, System.currentTimeMillis()))
                        saveMessages(context, messages)
                        input = ""
                    }
                },
            ) {
                Text("Send")
            }
        }
    }
}
