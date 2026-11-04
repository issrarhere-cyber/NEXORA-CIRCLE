package com.nexora.circle.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nexora.circle.ui.components.StarfieldBackground
import com.nexora.circle.ui.screens.CommsScreen
import com.nexora.circle.ui.screens.NexoraXScreen
import com.nexora.circle.ui.screens.PlaceholderScreen
import com.nexora.circle.ui.screens.ProfileScreen
import com.nexora.circle.ui.theme.NexoraColors

private enum class Tab(val label: String, val icon: ImageVector) {
    Orbit("Orbit", Icons.Filled.Home),
    Comms("Comms", Icons.Filled.Email),
    Arcade("Arcade", Icons.Filled.PlayArrow),
    NexoraX("Nexora-X", Icons.Filled.Star),
    Profile("Me", Icons.Filled.Person),
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
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
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
                    Tab.Orbit -> PlaceholderScreen(
                        title = "Orbit (Feed)",
                        stage = "Stage 3",
                        planned = listOf(
                            "Text, image and video posts",
                            "Likes, comments, sharing",
                            "Stories",
                            "Groups and communities, search and discovery",
                            "Notifications",
                        ),
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
