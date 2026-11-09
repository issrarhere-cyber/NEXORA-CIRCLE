package com.nexora.circle.ui.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.circle.ui.theme.NexoraColors

/** Routes of the screens that open from the drawer. */
object DrawerRoute {
    const val NEXUS = "nexus"
    const val LIBRARY = "library"
    const val SKILLS = "skills"
    const val THEME = "theme"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val FOLLOWING = "following"
    const val FOLLOWERS = "followers"
}

/** Common page frame for every drawer screen: back button, title, divider. */
@Composable
fun DrawerScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraColors.SpaceBlack)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NexoraColors.StarWhite,
                )
            }
            Text(
                title,
                color = NexoraColors.StarWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            actions()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NexoraColors.Divider),
        )
        content()
    }
}

/** Rounded card used inside drawer screens. */
@Composable
fun NxCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(NexoraColors.Panel)
    val clickable = if (onClick != null) base.clickable { onClick() } else base
    Column(modifier = clickable.padding(16.dp), content = content)
}

/** Small section title. */
@Composable
fun NxSectionLabel(text: String) {
    Text(
        text.uppercase(),
        color = NexoraColors.Muted.copy(alpha = 0.7f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
    )
}

/** Selectable pill used for tabs and choices. */
@Composable
fun NxChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    val accent = NexoraColors.PlasmaCyan
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accent else NexoraColors.Panel)
            .border(1.dp, if (selected) accent else NexoraColors.Divider, shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) NexoraColors.SpaceBlack else NexoraColors.StarWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** Empty-state message with an emoji. */
@Composable
fun NxEmpty(emoji: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, fontSize = 40.sp)
        Text(text, color = NexoraColors.Muted, fontSize = 15.sp)
    }
}

@Composable
fun HGap(w: Int) {
    Spacer(Modifier.width(w.dp))
}

@Composable
fun VGap(h: Int) {
    Spacer(Modifier.height(h.dp))
}
