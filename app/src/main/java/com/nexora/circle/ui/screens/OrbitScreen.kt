package com.nexora.circle.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.nexora.circle.ui.components.FounderBadge
import com.nexora.circle.ui.components.NexoraIcon
import com.nexora.circle.ui.components.NxIcon
import com.nexora.circle.ui.components.isFounder
import com.nexora.circle.ui.theme.NexoraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private data class Post(
    val id: String,
    val authorUid: String,
    val name: String,
    val handle: String,
    val text: String,
    val time: Long,
    val likes: List<String>,
    val resignals: List<String>,
    val resignalOf: String,
    val resignalFromName: String,
    val resignalFromUid: String,
    val resignalFromHandle: String,
    val resignalFromTime: Long,
    val quoteText: String,
    val originalText: String,
    val views: Int,
    val saved: List<String>,
    val visibility: String,
)

@Composable
private fun Avatar(photo: ImageBitmap?, initial: String, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
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
                initial,
                color = NexoraColors.SpaceBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun HeaderIcon(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        BadgedBox(
            badge = {
                if (count > 0) {
                    Badge(
                        containerColor = NexoraColors.PlasmaCyan,
                        contentColor = NexoraColors.SpaceBlack,
                    ) {
                        Text(if (count > 99) "99+" else count.toString())
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NexoraColors.StarWhite,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Top-bar title: pink orbit logo + "NEXORA".
 * - Pull karne par logo thoda aage khiskta hai.
 * - Refresh chalte waqt logo aage-peeche hilta hai aur phone halka vibrate karta hai.
 */
@Composable
private fun NexoraTitle(refreshing: Boolean, pullProgress: Float) {
    val haptic = LocalHapticFeedback.current

    val infinite = rememberInfiniteTransition(label = "orbitSwing")
    val swing by infinite.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "swing",
    )

    LaunchedEffect(refreshing) {
        if (refreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            while (isActive) {
                delay(320)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val shift = if (refreshing) swing else pullProgress.coerceIn(0f, 1f) * 8f

    Row(
        modifier = Modifier.offset(x = shift.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        NexoraIcon(NxIcon.Orbit, tint = NexoraColors.NebulaPink, iconSize = 24.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            "NEXORA",
            color = NexoraColors.StarWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Professional dialog used for "New signal" and "Quote". */
@Composable
private fun SignalDialog(
    title: String,
    placeholder: String,
    actionLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    allowBlank: Boolean = false,
    maxChars: Int = 280,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val canSend = allowBlank || value.isNotBlank()
    val nearLimit = value.length >= maxChars - 20
    val shape = RoundedCornerShape(28.dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(shape)
                .background(NexoraColors.Panel)
                .border(1.dp, NexoraColors.PlasmaCyan.copy(alpha = 0.25f), shape)
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NexoraIcon(NxIcon.Orbit, tint = NexoraColors.NebulaPink, iconSize = 24.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    color = NexoraColors.StarWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= maxChars) onValueChange(it) },
                placeholder = {
                    Text(
                        placeholder,
                        color = NexoraColors.StarWhite.copy(alpha = 0.45f),
                        fontSize = 17.sp,
                    )
                },
                textStyle = LocalTextStyle.current.copy(fontSize = 17.sp, lineHeight = 24.sp),
                shape = RoundedCornerShape(16.dp),
                minLines = 4,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NexoraColors.StarWhite,
                    unfocusedTextColor = NexoraColors.StarWhite,
                    cursorColor = NexoraColors.PlasmaCyan,
                    focusedBorderColor = NexoraColors.PlasmaCyan,
                    unfocusedBorderColor = NexoraColors.StarWhite.copy(alpha = 0.2f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${value.length}/$maxChars",
                    color = if (nearLimit) NexoraColors.NebulaPink
                    else NexoraColors.StarWhite.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        color = NexoraColors.StarWhite.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = onSend,
                    enabled = canSend,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NexoraColors.PlasmaCyan,
                        contentColor = NexoraColors.SpaceBlack,
                        disabledContainerColor = NexoraColors.PlasmaCyan.copy(alpha = 0.25f),
                        disabledContentColor = NexoraColors.SpaceBlack.copy(alpha = 0.5f),
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                ) {
                    Text(actionLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbitScreen(
    onOpenComms: () -> Unit,
    onOpenNexoraX: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSearch: () -> Unit = {},
    onOpenSignal: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    signalCount: Int = 0,
    beamCount: Int = 0,
    notifCount: Int = 0,
) {
    val context = LocalContext.current
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val myUid = user?.uid.orEmpty()
    val db = remember { FirebaseFirestore.getInstance() }
    val profile = remember { context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE) }
    val name = remember {
        profile.getString("name", "")?.trim().orEmpty()
            .ifBlank { user?.displayName.orEmpty() }
    }
    val handle = remember { profile.getString("username", "")?.trim().orEmpty() }
    val photo = remember {
        profile.getString("photo_path", null)?.let {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(it, opts)?.asImageBitmap()
        }
    }
    val initial = (name.firstOrNull()?.uppercaseChar() ?: '?').toString()

    val posts = remember { mutableStateListOf<Post>() }
    var tab by remember { mutableStateOf(0) }
    var composing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var commentsFor by remember { mutableStateOf<String?>(null) }
    var quoteSource by remember { mutableStateOf<ResignalSource?>(null) }
    var quoteText by remember { mutableStateOf("") }

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

    // TODO: fill this with matched users' uids when Signal/match data is connected
    val matches = remember { emptySet<String>() }
    val visiblePosts = posts.filter {
        it.authorUid == myUid ||
            it.visibility == "Public" ||
            (it.visibility == "Only Match Signaled" && matches.contains(it.authorUid))
    }

    // Following tab: uids of people I follow (users/{myUid}/following/{uid})
    var followingUids by remember { mutableStateOf<Set<String>>(emptySet()) }

    DisposableEffect(myUid) {
        if (myUid.isBlank()) {
            onDispose { }
        } else {
            val followReg = db.collection("users").document(myUid)
                .collection("following")
                .addSnapshotListener { snap, _ ->
                    if (snap != null) {
                        followingUids = snap.documents.map { it.id }.toSet()
                    }
                }
            onDispose { followReg.remove() }
        }
    }

    // For you = everything visible; Following = only posts by people I follow
    val feedPosts = if (tab == 1) {
        visiblePosts.filter { followingUids.contains(it.authorUid) }
    } else {
        visiblePosts
    }

    DisposableEffect(myUid) {
        val reg = db.collection("posts")
            .orderBy("time", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    errorMsg = "Could not load posts: ${err.localizedMessage}"
                    return@addSnapshotListener
                }
                if (snap != null) {
                    errorMsg = ""
                    val list = snap.documents.mapNotNull { d ->
                        val text = d.getString("text") ?: return@mapNotNull null
                        @Suppress("UNCHECKED_CAST")
                        val likes = (d.get("likes") as? List<String>) ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val resignals = (d.get("resignals") as? List<String>) ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val saved = (d.get("saved") as? List<String>) ?: emptyList()
                        Post(
                            id = d.id,
                            authorUid = d.getString("authorUid") ?: "",
                            name = d.getString("authorName") ?: "",
                            handle = d.getString("authorHandle") ?: "",
                            text = text,
                            time = d.getLong("time") ?: 0L,
                            likes = likes,
                            resignals = resignals,
                            resignalOf = d.getString("resignalOf") ?: "",
                            resignalFromName = d.getString("resignalFromName") ?: "",
                            resignalFromUid = d.getString("resignalFromUid") ?: "",
                            resignalFromHandle = d.getString("resignalFromHandle") ?: "",
                            resignalFromTime = d.getLong("resignalFromTime") ?: 0L,
                            quoteText = d.getString("quoteText") ?: "",
                            originalText = d.getString("originalText") ?: "",
                            views = (d.getLong("views") ?: 0L).toInt(),
                            saved = saved,
                            visibility = d.getString("visibility") ?: "Public",
                        )
                    }
                    posts.clear()
                    posts.addAll(list)
                }
            }
        onDispose { reg.remove() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.clickable { onOpenDrawer() }) {
                    if (com.nexora.circle.ui.drawer.AnonymousMode.enabled) {
                        com.nexora.circle.ui.drawer.AnonAvatar(36.dp)
                    } else {
                        Avatar(photo, initial, 36.dp)
                    }
                }
                // Center: pink orbit logo + NEXORA
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    NexoraTitle(
                        refreshing = isRefreshing,
                        pullProgress = pullState.distanceFraction,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeaderIcon(Icons.Outlined.Search, "Search", 0) { onOpenSearch() }
                    HeaderIcon(Icons.Outlined.PersonAdd, "Signal", signalCount) { onOpenSignal() }
                    BeamHeaderIcon(beamCount) { onOpenComms() }
                    HeaderIcon(Icons.Outlined.Notifications, "Notifications", notifCount) { onOpenNotifications() }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("For you", "Following").forEachIndexed { i, label ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { tab = i }
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            label,
                            color = NexoraColors.StarWhite,
                            fontSize = 16.sp,
                            fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(48.dp)
                                .background(
                                    if (tab == i) NexoraColors.PlasmaCyan else Color.Transparent,
                                    RoundedCornerShape(2.dp),
                                )
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NexoraColors.Divider)
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    // Server se fresh posts mango; listener list ko khud update kar dega.
                    db.collection("posts")
                        .orderBy("time", Query.Direction.DESCENDING)
                        .limit(100)
                        .get(Source.SERVER)
                        .addOnCompleteListener { isRefreshing = false }
                },
                state = pullState,
                modifier = Modifier.fillMaxSize(),
                indicator = {}, // spinner nahi, logo hi indicator hai
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    if (errorMsg.isNotEmpty()) {
                        Text(
                            errorMsg,
                            color = NexoraColors.NebulaPink,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }

                    if (feedPosts.isEmpty()) {
                        Text(
                            if (tab == 1) {
                                if (followingUids.isEmpty())
                                    "You are not following anyone yet. Follow people to see their posts here."
                                else
                                    "No posts from people you follow yet."
                            } else {
                                "No posts yet. Tap + to share your first signal."
                            },
                            color = NexoraColors.StarWhite,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(feedPosts, key = { it.id }) { post ->
                                val mine = post.authorUid == myUid
                                val found = if (post.resignalOf.isNotEmpty()) {
                                    posts.firstOrNull { it.id == post.resignalOf }
                                } else {
                                    null
                                }
                                val d = buildPostDisplay(
                                    id = post.id,
                                    authorUid = post.authorUid,
                                    authorName = post.name,
                                    authorHandle = post.handle,
                                    text = post.text,
                                    time = post.time,
                                    resignalOf = post.resignalOf,
                                    resignalFromUid = post.resignalFromUid,
                                    resignalFromName = post.resignalFromName,
                                    resignalFromHandle = post.resignalFromHandle,
                                    resignalFromTime = post.resignalFromTime,
                                    quoteText = post.quoteText,
                                    originalText = post.originalText,
                                    foundUid = found?.authorUid.orEmpty(),
                                    foundName = found?.name.orEmpty(),
                                    foundHandle = found?.handle.orEmpty(),
                                    foundTime = found?.time ?: 0L,
                                    foundText = found?.text.orEmpty(),
                                )
                                val isRepost = d.repostedBy.isNotEmpty()
                                val liked = post.likes.contains(myUid)
                                val resignaled = if (isRepost) {
                                    mine || found?.resignals?.contains(myUid) == true
                                } else {
                                    post.resignals.contains(myUid)
                                }
                                val source = ResignalSource(
                                    id = d.baseId,
                                    authorUid = d.uid,
                                    name = d.name,
                                    handle = d.handle,
                                    text = d.body,
                                    time = d.time,
                                    visibility = post.visibility,
                                )
                                val openAuthor: () -> Unit = {
                                    if (d.uid.isNotBlank()) {
                                        if (d.uid == myUid) onOpenProfile()
                                        else onOpenUserProfile(d.uid)
                                    }
                                }

                                LaunchedEffect(post.id) {
                                    if (!mine && myUid.isNotEmpty()) {
                                        db.collection("posts").document(post.id)
                                            .update("views", FieldValue.increment(1))
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(modifier = Modifier.clickable { openAuthor() }) {
                                        if (com.nexora.circle.ui.drawer.AnonymousMode.isAnon(d.uid)) {
                                            com.nexora.circle.ui.drawer.AnonAvatar(44.dp)
                                        } else {
                                            Avatar(
                                                if (d.uid.isNotEmpty() && d.uid == myUid) photo else null,
                                                (d.name.firstOrNull()?.uppercaseChar() ?: '?').toString(),
                                                44.dp,
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        PostCardText(d) { openAuthor() }
                                        Spacer(Modifier.height(8.dp))
                                        PostActionBar(
                                            liked = liked,
                                            resignaled = resignaled,
                                            likeCount = post.likes.size,
                                            resignalCount = post.resignals.size,
                                            onComment = { commentsFor = post.id },
                                            onLike = {
                                                db.collection("posts").document(post.id).update(
                                                    "likes",
                                                    if (liked) FieldValue.arrayRemove(myUid)
                                                    else FieldValue.arrayUnion(myUid),
                                                )
                                            },
                                            onResignal = {
                                                if (resignaled) {
                                                    undoResignal(db, myUid, d.baseId)
                                                } else {
                                                    sendResignal(db, myUid, name, handle, source, "")
                                                }
                                            },
                                            onShare = {
                                                val send = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, d.body)
                                                }
                                                context.startActivity(
                                                    Intent.createChooser(send, "Share")
                                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            },
                                            viewCount = post.views,
                                            saved = post.saved.contains(myUid),
                                            visibility = post.visibility,
                                            isOwnPost = mine,
                                            onQuote = { quoteSource = source },
                                            onReactWithVideo = {
                                                Toast.makeText(
                                                    context,
                                                    "Video reactions coming soon",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            },
                                            onSave = {
                                                val isSaved = post.saved.contains(myUid)
                                                db.collection("posts").document(post.id).update(
                                                    "saved",
                                                    if (isSaved) FieldValue.arrayRemove(myUid)
                                                    else FieldValue.arrayUnion(myUid),
                                                )
                                            },
                                            onVisibilityChange = { v ->
                                                db.collection("posts").document(post.id)
                                                    .update("visibility", v)
                                            },
                                            onDelete = {
                                                db.collection("posts").document(post.id).delete()
                                                if (mine && post.resignalOf.isNotEmpty()) {
                                                    db.collection("posts").document(post.resignalOf)
                                                        .update("resignals", FieldValue.arrayRemove(myUid))
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(NexoraColors.PlasmaCyan)
                .clickable { composing = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "+",
                color = NexoraColors.SpaceBlack,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        commentsFor?.let { CommentsDialog(it, myUid, name) { commentsFor = null } }

        quoteSource?.let { q ->
            SignalDialog(
                title = "Quote",
                placeholder = "Add your thoughts...",
                actionLabel = "Send signal",
                value = quoteText,
                onValueChange = { quoteText = it },
                onDismiss = { quoteSource = null; quoteText = "" },
                allowBlank = true,
                onSend = {
                    sendResignal(db, myUid, name, handle, q, quoteText)
                    quoteSource = null
                    quoteText = ""
                },
            )
        }

        if (composing) {
            SignalDialog(
                title = "New signal",
                placeholder = "What's your signal...?",
                actionLabel = "Send signal",
                value = draft,
                onValueChange = { draft = it },
                onDismiss = { composing = false },
                onSend = {
                    if (draft.isNotBlank()) {
                        db.collection("posts").add(
                            hashMapOf(
                                "authorUid" to myUid,
                                "authorName" to name,
                                "authorHandle" to handle,
                                "text" to draft.trim(),
                                "time" to System.currentTimeMillis(),
                                "likes" to emptyList<String>(),
                                "resignals" to emptyList<String>(),
                            )
                        )
                    }
                    draft = ""
                    composing = false
                },
            )
        }
    }
}
