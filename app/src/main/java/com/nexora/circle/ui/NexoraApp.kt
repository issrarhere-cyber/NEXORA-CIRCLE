package com.nexora.circle.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.components.NexoraIcon
import com.nexora.circle.ui.components.NxIcon
import com.nexora.circle.ui.components.StarfieldBackground
import com.nexora.circle.ui.screens.CommsScreen
import com.nexora.circle.ui.screens.NexoraXScreen
import com.nexora.circle.ui.screens.NotificationsScreen
import com.nexora.circle.ui.screens.OrbitScreen
import com.nexora.circle.ui.screens.PlaceholderScreen
import com.nexora.circle.ui.screens.ProfileScreen
import com.nexora.circle.ui.screens.SearchScreen
import com.nexora.circle.ui.screens.SignalScreen
import com.nexora.circle.ui.screens.UserProfileScreen
import com.nexora.circle.ui.theme.NexoraColors

private enum class Tab(val label: String) {
    Orbit("Orbit"),
    Comms("Comms"),
    Arcade("Arcade"),
    NexoraX("Nexora-X"),
    Profile("Me"),
}

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
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showSignal by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var profileUid by rememberSaveable { mutableStateOf("") }
    var beamUid by rememberSaveable { mutableStateOf("") }

    // While typing in Comms: hide the bottom bar and drop its padding, because the
    // chat screen already adds keyboard padding itself (otherwise the gap is counted twice).
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val hideBar = imeVisible && selected == Tab.Comms
    val layoutDirection = LocalLayoutDirection.current

    // System back button: close the open sub-screen first, then return to the Orbit tab,
    // and only then let the app close. (Placed before the content so a screen that has
    // its own BackHandler, e.g. a chat, still takes priority.)
    val subScreenOpen = profileUid.isNotEmpty() || showSearch || showSignal || showNotifications
    BackHandler(enabled = subScreenOpen || selected != Tab.Orbit) {
        when {
            profileUid.isNotEmpty() -> profileUid = ""
            showSearch -> showSearch = false
            showSignal -> showSignal = false
            showNotifications -> showNotifications = false
            else -> selected = Tab.Orbit
        }
    }

    StarfieldBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (!hideBar) {
                    NavigationBar(containerColor = NexoraColors.Panel) {
                        Tab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = selected == tab,
                                onClick = {
                                    selected = tab
                                    showSearch = false
                                    showSignal = false
                                    showNotifications = false
                                    profileUid = ""
                                    beamUid = ""
                                },
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
                }
            },
        ) { padding: PaddingValues ->
            val contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(layoutDirection),
                bottom = if (hideBar) 0.dp else padding.calculateBottomPadding(),
            )
            Box(Modifier.fillMaxSize().padding(contentPadding)) {
                when (selected) {
                    Tab.Orbit -> {
                        if (profileUid.isNotEmpty()) {
                            UserProfileScreen(
                                uid = profileUid,
                                onBack = { profileUid = "" },
                                onBeam = { uid ->
                                    beamUid = uid
                                    profileUid = ""
                                    selected = Tab.Comms
                                },
                            )
                        } else if (showSearch) {
                            SearchScreen(
                                onBack = { showSearch = false },
                                onUserClick = { uid -> profileUid = uid },
                            )
                        } else if (showSignal) {
                            SignalScreen(onBack = { showSignal = false })
                        } else if (showNotifications) {
                            NotificationsScreen(onBack = { showNotifications = false })
                        } else {
                            OrbitScreen(
                                onOpenComms = { selected = Tab.Comms },
                                onOpenNexoraX = { selected = Tab.NexoraX },
                                onOpenProfile = { selected = Tab.Profile },
                                onOpenSearch = { showSearch = true },
                                onOpenSignal = { showSignal = true },
                                onOpenNotifications = { showNotifications = true },
                                onOpenUserProfile = { uid -> profileUid = uid },
                            )
                        }
                    }
                    Tab.Comms -> CommsScreen(startUid = beamUid.ifEmpty { null })
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
