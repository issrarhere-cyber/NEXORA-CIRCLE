package com.nexora.circle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.nexora.circle.ui.theme.NexoraColors

@Composable
fun OrbitTopBar(
    signalCount: Int = 0,
    beamCount: Int = 0,
    notifCount: Int = 0,
    onProfile: () -> Unit = {},
    onSearch: () -> Unit = {},
    onSignal: () -> Unit = {},
    onBeam: () -> Unit = {},
    onNotif: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexoraColors.SpaceBlack)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onProfile) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(NexoraColors.PlasmaCyan, CircleShape)
            )
        }
        Spacer(Modifier.weight(1f))
        BarIcon(Icons.Outlined.Search, "Search", 0, onSearch)
        BarIcon(Icons.Outlined.PersonAdd, "Signal", signalCount, onSignal)
        BarIcon(Icons.AutoMirrored.Outlined.Send, "Beam", beamCount, onBeam)
        BarIcon(Icons.Outlined.Notifications, "Notifications", notifCount, onNotif)
    }
}

@Composable
private fun BarIcon(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(
                        containerColor = NexoraColors.PlasmaCyan,
                        contentColor = NexoraColors.SpaceBlack
                    ) {
                        Text(if (count > 99) "99+" else count.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NexoraColors.StarWhite
            )
        }
    }
}
