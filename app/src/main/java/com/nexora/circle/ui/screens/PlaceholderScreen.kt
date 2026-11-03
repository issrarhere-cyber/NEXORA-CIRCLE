package com.nexora.circle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.components.GlassCard
import com.nexora.circle.ui.theme.NexoraColors

/**
 * Honest placeholder for sections that are not built yet.
 * It says clearly which stage will deliver the feature, and lists planned items.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    stage: String,
    planned: List<String>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)
        GlassCard {
            Text(
                "Not built yet — arrives in $stage",
                style = MaterialTheme.typography.titleMedium,
                color = NexoraColors.PlasmaCyan,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This section is a placeholder. No fake data is shown.",
                color = NexoraColors.Muted,
            )
        }
        GlassCard {
            Text("Planned", style = MaterialTheme.typography.titleSmall, color = NexoraColors.NebulaPink)
            Spacer(Modifier.height(6.dp))
            planned.forEach {
                Text("• $it", color = NexoraColors.StarWhite, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
