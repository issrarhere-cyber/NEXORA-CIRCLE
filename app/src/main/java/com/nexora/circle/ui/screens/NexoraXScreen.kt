package com.nexora.circle.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nexora.circle.language.NexoraXRepository
import com.nexora.circle.language.NexoraXTranslator
import com.nexora.circle.language.TranslationResult
import com.nexora.circle.ui.components.GlassCard
import com.nexora.circle.ui.theme.NexoraColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NexoraXScreen() {
    val context = LocalContext.current
    val table = remember { NexoraXRepository.load(context) }
    val translator = remember(table) { NexoraXTranslator(table) }

    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<TranslationResult?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Nexora-X", style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)

        if (table.isEmpty) {
            GlassCard {
                Text(
                    "Language tables are empty",
                    style = MaterialTheme.typography.titleMedium,
                    color = NexoraColors.NebulaPink,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Add your real alphabet and numbers to app/src/main/assets/nexora_x/alphabet.json " +
                        "and numbers.json. The translator and keyboard below will use them automatically. " +
                        "Until then, text is passed through unchanged.",
                    color = NexoraColors.Muted,
                )
            }
        }

        if (table.warnings.isNotEmpty()) {
            GlassCard {
                Text("Table warnings", style = MaterialTheme.typography.titleSmall, color = NexoraColors.NebulaPink)
                table.warnings.forEach { Text("• $it", color = NexoraColors.Muted) }
            }
        }

        GlassCard {
            Text("Translator", style = MaterialTheme.typography.titleMedium, color = NexoraColors.PlasmaCyan)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Type or paste text") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { result = translator.englishToNexora(input) }) {
                    Text("English → Nexora-X")
                }
                Button(onClick = { result = translator.nexoraToEnglish(input) }) {
                    Text("Nexora-X → English")
                }
            }
            result?.let { r ->
                Spacer(Modifier.height(10.dp))
                Text("Result", style = MaterialTheme.typography.labelLarge, color = NexoraColors.Muted)
                Text(r.output, style = MaterialTheme.typography.titleLarge, color = NexoraColors.StarWhite)
                if (r.unmapped.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "No table entry for: ${r.unmapped.joinToString(" ")}",
                        color = NexoraColors.NebulaPink,
                    )
                }
            }
        }

        GlassCard {
            Text("Keyboard", style = MaterialTheme.typography.titleMedium, color = NexoraColors.PlasmaCyan)
            Spacer(Modifier.height(8.dp))
            if (table.isEmpty) {
                Text("No symbols loaded yet.", color = NexoraColors.Muted)
            } else {
                Text("Tap a symbol to add it to the text box above.", color = NexoraColors.Muted)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    table.all.forEach { g ->
                        AssistChip(
                            onClick = { input += g.symbol },
                            label = { Text(g.symbol, style = MaterialTheme.typography.titleMedium) },
                        )
                    }
                }
            }
        }
    }
}
