package com.nexora.circle.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import org.json.JSONArray

private const val CHAT_PREFS = "nexora_comms"
private const val KEY_MESSAGES = "messages"

private fun loadMessages(context: Context): List<String> {
    val prefs = context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveMessages(context: Context, messages: List<String>) {
    val arr = JSONArray()
    messages.forEach { arr.put(it) }
    context.getSharedPreferences(CHAT_PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_MESSAGES, arr.toString()).apply()
}

@Composable
fun CommsScreen() {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<String>().apply { addAll(loadMessages(context)) } }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexoraColors.Panel, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                ) {
                    Text(msg, color = NexoraColors.StarWhite)
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
            Button(onClick = {
                val text = input.trim()
                if (text.isNotEmpty()) {
                    messages.add(text)
                    saveMessages(context, messages)
                    input = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}
