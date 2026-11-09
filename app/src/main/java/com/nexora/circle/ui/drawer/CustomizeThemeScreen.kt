package com.nexora.circle.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.circle.ui.theme.AppAccent
import com.nexora.circle.ui.theme.FontSizeChoice
import com.nexora.circle.ui.theme.FontStyleChoice
import com.nexora.circle.ui.theme.NexoraColors
import com.nexora.circle.ui.theme.ThemeMode
import com.nexora.circle.ui.theme.ThemeState

/** Every change here is applied to the whole app instantly and saved on the phone. */
@Composable
fun CustomizeThemeScreen(onBack: () -> Unit) {
    val accent = ThemeState.accent
    val mode = ThemeState.mode
    val fontStyle = ThemeState.fontStyle
    val fontSize = ThemeState.fontSize

    DrawerScreenScaffold(title = "Customize Theme", onBack = onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ---- Live preview ----
            NxSectionLabel("Preview")
            NxCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NexoraColors.PlasmaCyan),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("N", color = NexoraColors.SpaceBlack, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            "Nexora Explorer",
                            color = NexoraColors.StarWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "@explorer · now",
                            color = NexoraColors.Muted.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "This is how your posts and messages will look with these settings.",
                    color = NexoraColors.StarWhite,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { }) { Text("Accent button") }
            }

            // ---- App colour ----
            NxSectionLabel("App colour")
            val all = AppAccent.values().toList()
            all.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { a ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { ThemeState.setAccent(a) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val selected = a == accent
                            val ringColor = if (selected) NexoraColors.StarWhite else Color(0xFF8A8A8A)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(a.swatch)
                                    .border(if (selected) 3.dp else 1.dp, ringColor, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Text(
                                        "✓",
                                        color = if (a == AppAccent.Black) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (a == AppAccent.Cyan) "Cyan (default)" else a.label,
                                color = NexoraColors.StarWhite,
                                fontSize = 12.sp,
                                maxLines = 1,
                            )
                        }
                    }
                    // keep the last row aligned with the rows above
                    repeat(4 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (accent == AppAccent.Black) {
                Text(
                    "Black is monochrome: white buttons in Dark mode, black buttons in Light mode.",
                    color = NexoraColors.Muted.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }

            // ---- Theme mode ----
            NxSectionLabel("Theme mode")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NxChip("Light", mode == ThemeMode.Light, { ThemeState.setMode(ThemeMode.Light) })
                NxChip("Dark", mode == ThemeMode.Dark, { ThemeState.setMode(ThemeMode.Dark) })
            }

            // ---- Font style ----
            NxSectionLabel("Font style")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontStyleChoice.values().forEach { f ->
                    NxChip(f.label, fontStyle == f, { ThemeState.setFontStyle(f) })
                }
            }

            // ---- Font size ----
            NxSectionLabel("Font size")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FontSizeChoice.values().forEach { f ->
                    NxChip(f.label, fontSize == f, { ThemeState.setFontSize(f) })
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Changes apply instantly to the whole app and are saved on this phone.",
                color = NexoraColors.Muted.copy(alpha = 0.65f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { ThemeState.resetAll() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset to default") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
