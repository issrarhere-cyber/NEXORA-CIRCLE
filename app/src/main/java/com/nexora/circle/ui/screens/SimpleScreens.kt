package com.nexora.circle.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.theme.NexoraColors

@Composable
private fun SimpleScreen(title: String, message: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NexoraColors.StarWhite)
            }
            Text(title, color = NexoraColors.StarWhite, style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(message, color = NexoraColors.Muted)
        }
    }
}

@Composable
fun SignalScreen(onBack: () -> Unit) {
    SimpleScreen("Signal", "Friend requests yahan aayengi", onBack)
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    SimpleScreen("Notifications", "Abhi koi notification nahi", onBack)
}
