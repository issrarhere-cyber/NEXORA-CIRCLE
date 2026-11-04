package com.nexora.circle.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.components.GlassCard
import com.nexora.circle.ui.theme.NexoraColors

private const val PREFS_NAME = "nexora_profile"
private const val KEY_NAME = "name"
private const val KEY_BIO = "bio"

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var savedName by remember { mutableStateOf(prefs.getString(KEY_NAME, "") ?: "") }
    var savedBio by remember { mutableStateOf(prefs.getString(KEY_BIO, "") ?: "") }
    var name by remember { mutableStateOf(savedName) }
    var bio by remember { mutableStateOf(savedBio) }
    var message by remember { mutableStateOf("") }

    val initial = (savedName.trim().firstOrNull()?.uppercaseChar() ?: '?').toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)

        GlassCard {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(NexoraColors.PlasmaCyan),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, style = MaterialTheme.typography.headlineLarge, color = NexoraColors.SpaceBlack)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (savedName.isBlank()) "No name set yet" else savedName,
                style = MaterialTheme.typography.titleLarge,
                color = NexoraColors.StarWhite,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (savedBio.isBlank()) "No bio yet" else savedBio,
                color = NexoraColors.Muted,
            )
        }

        GlassCard {
            Text("Edit profile", style = MaterialTheme.typography.titleSmall, color = NexoraColors.NebulaPink)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("Display name") },
                singleLine = true,
                textStyle = TextStyle(color = NexoraColors.StarWhite),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(160) },
                label = { Text("Bio") },
                textStyle = TextStyle(color = NexoraColors.StarWhite),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                prefs.edit().putString(KEY_NAME, name.trim()).putString(KEY_BIO, bio.trim()).apply()
                savedName = name.trim()
                savedBio = bio.trim()
                message = "Profile saved."
            }) {
                Text("Save")
            }
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(message, color = NexoraColors.PlasmaCyan)
            }
        }

        GlassCard {
            Text("About", style = MaterialTheme.typography.titleSmall, color = NexoraColors.NebulaPink)
            Spacer(Modifier.height(6.dp))
            Text("Nexora Circle", color = NexoraColors.StarWhite)
            Text("This profile is stored on this device only.", color = NexoraColors.Muted)
        }
    }
}
