package com.nexora.circle.ui.drawer

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.components.FounderBadge
import com.nexora.circle.ui.components.isFounder
import com.nexora.circle.ui.theme.NexoraColors

private val DrawerPink = Color(0xFFFF4FB0)
private val DrawerPinkLight = Color(0xFFFF9AD5)

/**
 * X / Twitter style left drawer.
 * - slides in from the left, dim background, close button, tap outside or swipe left to close
 * - system Back button closes it first
 *
 * onProfile: open my profile page.
 * onNavigate: open one of the DrawerRoute screens.
 */
@Composable
fun NexoraDrawer(
    open: Boolean,
    onClose: () -> Unit,
    onProfile: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val currentClose by rememberUpdatedState(onClose)

    BackHandler(enabled = open) { currentClose() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dim background
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(animationSpec = tween(260)),
            exit = fadeOut(animationSpec = tween(220)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .pointerInput(Unit) {
                        detectTapGestures { currentClose() }
                    },
            )
        }

        // Sliding panel
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.CenterStart),
            enter = slideInHorizontally(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialOffsetX = { fullWidth -> -fullWidth },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                targetOffsetX = { fullWidth -> -fullWidth },
            ),
        ) {
            DrawerPanel(
                onClose = { currentClose() },
                onProfile = onProfile,
                onNavigate = onNavigate,
            )
        }
    }
}

@Composable
private fun DrawerPanel(
    onClose: () -> Unit,
    onProfile: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid.orEmpty()
    val db = remember { FirebaseFirestore.getInstance() }
    val prefs = remember { context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE) }

    var name by remember {
        mutableStateOf(
            prefs.getString("name", "")?.trim().orEmpty()
                .ifBlank { user?.displayName.orEmpty() },
        )
    }
    var username by remember { mutableStateOf(prefs.getString("username", "")?.trim().orEmpty()) }
    var following by remember { mutableStateOf(0) }
    var followers by remember { mutableStateOf(0) }

    val photo = remember {
        prefs.getString("photo_path", null)?.let { path ->
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
        }
    }

    DisposableEffect(uid) {
        if (uid.isBlank()) {
            return@DisposableEffect onDispose { }
        }
        val userRef = db.collection("users").document(uid)
        val r1 = userRef.addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                val n = snap.getString("name").orEmpty()
                if (n.isNotBlank()) name = n
                val u = snap.getString("username").orEmpty()
                if (u.isNotBlank()) username = u
            }
        }
        val r2 = userRef.collection("following").addSnapshotListener { snap, _ ->
            if (snap != null) following = snap.size()
        }
        val r3 = userRef.collection("followers").addSnapshotListener { snap, _ ->
            if (snap != null) followers = snap.size()
        }
        onDispose {
            r1.remove()
            r2.remove()
            r3.remove()
        }
    }

    val anon = AnonymousMode.enabled
    val shownName = if (anon) AnonymousMode.NAME else name.ifBlank { "Explorer" }
    val shownHandle = if (anon) "@anonymous" else if (username.isBlank()) "" else "@$username"

    Row(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.84f)
            .widthIn(max = 360.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(NexoraColors.SpaceBlack)
                // swallow taps on empty panel space so they do not reach the dim background
                .pointerInput(Unit) { detectTapGestures { } }
                .pointerInput(Unit) {
                    var drag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onDragEnd = { if (drag < -80f) onClose() },
                        onDragCancel = { drag = 0f },
                        onHorizontalDrag = { _, amount -> drag += amount },
                    )
                }
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ---- Title + close button ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NEXORA-CIRCLE",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(DrawerPink, DrawerPinkLight)),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp,
                        shadow = Shadow(
                            color = DrawerPink.copy(alpha = 0.55f),
                            offset = Offset(0f, 0f),
                            blurRadius = 14f,
                        ),
                    ),
                )
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close menu",
                        tint = NexoraColors.StarWhite,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- Profile block ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Box(modifier = Modifier.clickable { onProfile() }) {
                    if (anon) {
                        AnonAvatar(56.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(NexoraColors.PlasmaCyan),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (photo != null) {
                                Image(
                                    bitmap = photo,
                                    contentDescription = "Profile photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text(
                                    (name.trim().firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                    color = NexoraColors.SpaceBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shownName,
                        color = NexoraColors.StarWhite,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable { onProfile() },
                    )
                    if (!anon && isFounder(uid)) {
                        Spacer(Modifier.width(6.dp))
                        FounderBadge(size = 18.dp)
                    }
                }
                if (shownHandle.isNotEmpty()) {
                    Text(
                        shownHandle,
                        color = NexoraColors.Muted.copy(alpha = 0.65f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    CountText(following, "Following") { onNavigate(DrawerRoute.FOLLOWING) }
                    CountText(followers, "Followers") { onNavigate(DrawerRoute.FOLLOWERS) }
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NexoraColors.Divider),
            )

            // ---- Menu ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
            ) {
                DrawerItem(Icons.Outlined.Person, "Profile") { onProfile() }
                DrawerItem(Icons.Outlined.Mic, "NEXORA NEXUS") { onNavigate(DrawerRoute.NEXUS) }
                DrawerItem(Icons.Outlined.MenuBook, "My Library") { onNavigate(DrawerRoute.LIBRARY) }
                DrawerItem(Icons.Outlined.School, "Learn Skills") { onNavigate(DrawerRoute.SKILLS) }
                DrawerItem(Icons.Outlined.Palette, "Customize Theme") { onNavigate(DrawerRoute.THEME) }

                // Anonymous Mode: one toggle, works right here (the header above updates instantly)
                DrawerItem(
                    icon = Icons.Outlined.VisibilityOff,
                    label = "Anonymous Mode",
                    onClick = { AnonymousMode.set(context, !AnonymousMode.enabled) },
                    trailing = {
                        Switch(
                            checked = anon,
                            onCheckedChange = { AnonymousMode.set(context, it) },
                        )
                    },
                )
                if (anon) {
                    Text(
                        text = if (AnonymousMode.syncError) {
                            "ON. Hidden on this phone. Others will see your name until the Firestore rules for anonymousUsers are added."
                        } else {
                            "ON. Your name and photo are hidden in posts, comments and Nexus."
                        },
                        color = NexoraColors.Muted.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(start = 64.dp, end = 20.dp, bottom = 6.dp),
                    )
                }

                DrawerItem(Icons.Outlined.Settings, "Settings") { onNavigate(DrawerRoute.SETTINGS) }
                DrawerItem(Icons.Outlined.HelpOutline, "Help Centre") { onNavigate(DrawerRoute.HELP) }
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(NexoraColors.Divider),
        )
    }
}

@Composable
private fun CountText(count: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            count.toString(),
            color = NexoraColors.StarWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = NexoraColors.Muted.copy(alpha = 0.65f),
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NexoraColors.StarWhite,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.width(18.dp))
        Text(
            label,
            color = NexoraColors.StarWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) {
            trailing()
        }
    }
}
