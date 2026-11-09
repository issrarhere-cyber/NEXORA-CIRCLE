package com.nexora.circle.ui.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class DrawerDestination(val label: String, val icon: ImageVector) {
    Nexus("Nexus", Icons.Filled.Star),
    Library("Library", Icons.Filled.Search),
    LearnSkills("Learn Skills", Icons.Filled.Build),
    Theme("Theme", Icons.Filled.Favorite),
    Anonymous("Anonymous Mode", Icons.Filled.Lock),
    Settings("Settings", Icons.Filled.Settings),
    Help("Help", Icons.Filled.Info)
}

/** Wrap your main app content with this. Open it with: scope.launch { drawerState.open() } */
@Composable
fun NexoraDrawerHost(
    drawerState: DrawerState,
    onDestination: (DrawerDestination) -> Unit,
    gesturesEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "NEXORA",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    DrawerDestination.values().forEach { d ->
                        if (d == DrawerDestination.Settings) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }
                        NavigationDrawerItem(
                            icon = { Icon(d.icon, contentDescription = null) },
                            label = { Text(d.label) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onDestination(d)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        },
        content = content
    )
}
