package com.nexora.circle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.components.NexoraIcon
import com.nexora.circle.ui.components.NxIcon
import com.nexora.circle.ui.components.StarfieldBackground
import com.nexora.circle.ui.screens.CommsScreen
import com.nexora.circle.ui.screens.NexoraXScreen
import com.nexora.circle.ui.screens.OrbitScreen
import com.nexora.circle.ui.screens.PlaceholderScreen
import com.nexora.circle.ui.screens.ProfileScreen
import com.nexora.circle.ui.theme.NexoraColors

private enum class Tab(val label: String) {
    Orbit("Orbit"),
    Comms("Comms"),
    Arcade("Arcade"),
    NexoraX("Nexora-X"),
    Profile("Me"),
}

// Original icons for the bottom bar
@Composable
private fun TabIcon(tab: Tab, tint: Color) {
    when (tab) {
        Tab.Orbit -> NexoraIcon(NxIcon.Home, tint = tint, iconSize = 26.dp)
        Tab.Comms -> NexoraIcon(NxIcon.Chat, tint = tint, iconSize = 26.dp)
        Tab.Arcade -> Canvas(Modifier.size(26.dp)) {
            val w = size.width
            val sw = w * 0.085f
            val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawRoundRect(
                color = tint,
                topLeft = Offset(0.08f * w, 0.28f * w),
                size = Size(0.84f * w, 0.44f * w),
                cornerRadius = CornerRadius(0.2f * w),
                style = stroke,
            )
            drawLine(tint, Offset(0.2f * w, 0.5f * w), Offset(0.38f * w, 0.5f * w), sw, StrokeCap.Round)
            drawLine(tint, Offset(0.29f * w, 0.41f * w), Offset(0.29f * w, 0.59f * w), sw, StrokeCap.Round)
            drawCircle(tint, radius = 0.045f * w, center = Offset(0.64f * w, 0.5f * w))
            drawCircle(tint, radius = 0.045f * w, center = Offset(0.77f * w, 0.5f * w))
        }
        Tab.NexoraX -> Canvas(Modifier.size(26.dp)) {
            val w = size.width
            val c = 0.5f * w
            val star = Path().apply {
                moveTo(c, 0.08f * w)
                cubicTo(c, c, c, c, 0.92f * w, c)
                cubicTo(c, c, c, c, c, 0.92f * w)
                cubicTo(c, c, c, c, 0.08f * w, c)
                cubicTo(c, c, c, c, c, 0.08f * w)
                close()
            }
            drawPath(
                star,
                tint,
                style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        Tab.Profile -> Canvas(Modifier.size(26.dp)) {
            val w = size.width
            val sw = w * 0.085f
            val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawCircle(tint, radius = 0.17f * w, center = Offset(0.5f * w, 0.32f * w), style = stroke)
            val body = Path().apply {
                moveTo(0.18f * w, 0.88f * w)
                cubicTo(0.18f * w, 0.58f * w, 0.82f * w, 0.58f * w, 0.82f * w, 0.88f * w)
            }
            drawPath(body, tint, style = stroke)
        }
    }
}

@Composable
fun NexoraApp() {
    var selected by rememberSaveable { mutableStateOf(Tab.Orbit) }

    StarfieldBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = NexoraColors.Panel) {
                    Tab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selected = tab },
                            icon = {
                                TabIcon(
                                    tab,
                                    if (selected == tab) NexoraColors.SpaceBlack else NexoraColors.Muted,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NexoraColors.SpaceBlack,
                                selectedTextColor = NexoraColors.PlasmaCyan,
                                indicatorColor = NexoraColors.PlasmaCyan,
                                unselectedIconColor = NexoraColors.Muted,
                                unselectedTextColor = NexoraColors.Muted,
                            ),
                        )
                    }
                }
            },
        ) { padding: PaddingValues ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selected) {
                    Tab.Orbit -> OrbitScreen(
                        onOpenComms = { selected = Tab.Comms },
                        onOpenNexoraX = { selected = Tab.NexoraX },
                        onOpenProfile = { selected = Tab.Profile },
                    )
                    Tab.Comms -> CommsScreen()
                    Tab.Arcade -> PlaceholderScreen(
                        title = "Arcade (Games)",
                        stage = "Stage 6",
                        planned = listOf(
                            "Space puzzles, alien mysteries",
                            "Nexora-X decoding games",
                            "Multiplayer quiz battles",
                            "Friends leaderboard and daily challenges",
                        ),
                    )
                    Tab.NexoraX -> NexoraXScreen()
                    Tab.Profile -> ProfileScreen()
                }
            }
        }
    }
}
