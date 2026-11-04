package com.nexora.circle.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.components.GlassCard
import com.nexora.circle.ui.theme.NexoraColors

@Composable
fun OrbitScreen(
    onOpenComms: () -> Unit,
    onOpenNexoraX: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val context = LocalContext.current
    val userName = remember {
        context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE)
            .getString("name", "")
            ?.trim()
            .orEmpty()
    }
    val greeting = if (userName.isBlank()) "Welcome, Explorer" else "Welcome, $userName"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Orbit", style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)

        GlassCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.titleLarge,
                    color = NexoraColors.StarWhite,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "CONNECT BEYOND THE UNIVERSE",
                    style = MaterialTheme.typography.titleSmall,
                    color = NexoraColors.PlasmaCyan,
                    textAlign = TextAlign.Center,
                )
            }
        }

        GlassCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Quick launch", style = MaterialTheme.typography.titleSmall, color = NexoraColors.NebulaPink)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onOpenNexoraX, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Nexora-X")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenComms, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Comms")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenProfile, modifier = Modifier.fillMaxWidth()) {
                    Text("My profile")
                }
            }
        }

        GlassCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Feed - coming in Stage 3", style = MaterialTheme.typography.titleSmall, color = NexoraColors.NebulaPink)
                Spacer(Modifier.height(6.dp))
                Text("Posts, stories, groups and notifications are not built yet.", color = NexoraColors.Muted)
            }
        }
    }
}
