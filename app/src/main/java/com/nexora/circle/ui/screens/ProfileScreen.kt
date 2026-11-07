package com.nexora.circle.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nexora.circle.ui.theme.NexoraColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

private const val PREFS_NAME = "nexora_profile"
private const val KEY_NAME = "name"
private const val KEY_USERNAME = "username"
private const val KEY_BIO = "bio"
private const val KEY_PHOTO = "photo_path"
private const val KEY_NAME_TIME = "name_changed_at"
private const val KEY_USERNAME_TIME = "username_changed_at"
private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

private const val NAME_MAX = 50
private const val USERNAME_PREFIX = "@"
private const val USERNAME_MIN = 3
private const val USERNAME_MAX = 28

private val CardBg = Color(0xFF121212)
private val LogoutRed = Color(0xFFFF4D6D)

/** Screens the Me tab can open. Connect these to your navigation (see onOpen). */
enum class ProfileDestination(val label: String) {
    Settings("Settings"),
    Posts("Posts"),
    Followers("Followers"),
    Following("Following"),
    Achievements("Achievements"),
    GameStats("Game Stats"),
    Saved("Saved"),
    Activity("Activity"),
    Privacy("Privacy & Security"),
    Notifications("Notifications"),
    Appearance("Appearance & Theme"),
    Help("Help & Support"),
    About("About Nexora Circle"),
}

private fun daysLeft(lastChange: Long): Int {
    if (lastChange == 0L) return 0
    val left = lastChange + WEEK_MS - System.currentTimeMillis()
    return if (left <= 0) 0 else ((left + 86_399_999L) / 86_400_000L).toInt()
}

private fun cleanUsername(input: String): String =
    input.lowercase()
        .removePrefix("x.")
        .filter { it in 'a'..'z' || it in '0'..'9' || it == '.' || it == '_' }
        .take(USERNAME_MAX)

private fun usernameError(name: String): String? = when {
    name.isEmpty() -> null
    name.length < USERNAME_MIN -> "At least $USERNAME_MIN characters required"
    name.contains("..") -> "Two dots in a row are not allowed"
    name.startsWith(".") || name.endsWith(".") -> "Cannot start or end with a dot"
    else -> null
}

@Composable
fun ProfileScreen(onOpen: ((ProfileDestination) -> Unit)? = null) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid
    val db = remember { FirebaseFirestore.getInstance() }

    // If navigation is not connected yet, tell the user honestly instead of doing nothing.
    val open: (ProfileDestination) -> Unit = onOpen ?: { dest ->
        Toast.makeText(context, "${dest.label} screen is not connected yet", Toast.LENGTH_SHORT).show()
    }

    var savedName by remember { mutableStateOf(prefs.getString(KEY_NAME, "") ?: "") }
    var savedUsername by remember { mutableStateOf(prefs.getString(KEY_USERNAME, "") ?: "") }
    var savedBio by remember { mutableStateOf(prefs.getString(KEY_BIO, "") ?: "") }
    var name by remember { mutableStateOf(savedName) }
    var username by remember { mutableStateOf(savedUsername) }
    var bio by remember { mutableStateOf(savedBio) }
    var editing by remember { mutableStateOf(false) }
    var syncMsg by remember { mutableStateOf("") }
    var showLogout by remember { mutableStateOf(false) }

    var nameChangedAt by remember { mutableStateOf(prefs.getLong(KEY_NAME_TIME, 0L)) }
    var usernameChangedAt by remember { mutableStateOf(prefs.getLong(KEY_USERNAME_TIME, 0L)) }

    // Values below are only READ from Firestore. "verified" must be set by an admin, never by the app.
    var verified by remember { mutableStateOf(false) }
    var userTitle by remember { mutableStateOf("") }
    var postsCount by remember { mutableStateOf(0L) }
    var followersCount by remember { mutableStateOf(0L) }
    var followingCount by remember { mutableStateOf(0L) }

    // Live sync: updates on every device whenever the profile changes in Firestore.
    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose { }
        val ref = db.collection("users").document(uid)
        val reg = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                syncMsg = "Offline: showing saved data"
                return@addSnapshotListener
            }
            if (snap != null && snap.exists()) {
                savedName = snap.getString("name") ?: ""
                savedUsername = snap.getString("username") ?: ""
                savedBio = snap.getString("bio") ?: ""
                nameChangedAt = snap.getLong("nameChangedAt") ?: 0L
                usernameChangedAt = snap.getLong("usernameChangedAt") ?: 0L
                verified = snap.getBoolean("verified") == true
                userTitle = snap.getString("title") ?: ""
                if (!editing) {
                    name = savedName
                    username = savedUsername
                    bio = savedBio
                }
                prefs.edit()
                    .putString(KEY_NAME, savedName)
                    .putString(KEY_USERNAME, savedUsername)
                    .putString(KEY_BIO, savedBio)
                    .putLong(KEY_NAME_TIME, nameChangedAt)
                    .putLong(KEY_USERNAME_TIME, usernameChangedAt)
                    .apply()

                // Lowercase search fields for older accounts (written once, so no loop).
                if (snap.getString("nameLower") == null || snap.getString("usernameLower") == null) {
                    ref.set(
                        mapOf(
                            "nameLower" to savedName.lowercase(),
                            "usernameLower" to savedUsername.lowercase(),
                        ),
                        SetOptions.merge()
                    )
                }
            } else {
                if (savedName.isBlank()) {
                    val dn = user?.displayName.orEmpty()
                    if (dn.isNotBlank()) {
                        savedName = dn
                        if (!editing) name = dn
                    }
                }
                if (savedName.isNotBlank() || savedUsername.isNotBlank()) {
                    ref.set(
                        mapOf(
                            "name" to savedName,
                            "username" to savedUsername,
                            "nameLower" to savedName.lowercase(),
                            "usernameLower" to savedUsername.lowercase(),
                            "bio" to savedBio,
                            "nameChangedAt" to nameChangedAt,
                            "usernameChangedAt" to usernameChangedAt,
                        ),
                        SetOptions.merge()
                    )
                }
            }
        }
        onDispose { reg.remove() }
    }

    // Real counts, counted live from the actual data (the old profile count fields were never written).
    DisposableEffect(uid) {
        if (uid == null) return@DisposableEffect onDispose { }
        val postsReg = db.collection("posts").whereEqualTo("authorUid", uid)
            .addSnapshotListener { s, _ -> if (s != null) postsCount = s.size().toLong() }
        val followersReg = db.collection("users").document(uid).collection("followers")
            .addSnapshotListener { s, _ -> if (s != null) followersCount = s.size().toLong() }
        val followingReg = db.collection("users").document(uid).collection("following")
            .addSnapshotListener { s, _ -> if (s != null) followingCount = s.size().toLong() }
        onDispose {
            postsReg.remove()
            followersReg.remove()
            followingReg.remove()
        }
    }

    val initial = (savedName.trim().firstOrNull()?.uppercaseChar() ?: '?').toString()

    var photoPath by remember { mutableStateOf(prefs.getString(KEY_PHOTO, null)) }
    val photo = remember(photoPath) {
        photoPath?.let {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(it, opts)?.asImageBitmap()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            photoPath?.let { File(it).delete() }
            prefs.edit().putString(KEY_PHOTO, file.absolutePath).apply()
            photoPath = file.absolutePath
        }
    }

    val nameDaysLeft = daysLeft(nameChangedAt)
    val usernameDaysLeft = daysLeft(usernameChangedAt)
    val usernameErr = usernameError(username)
    val canSave = name.trim().isNotEmpty() &&
        username.length >= USERNAME_MIN &&
        usernameErr == null

    fun startEditing() {
        name = savedName
        username = savedUsername
        bio = savedBio
        editing = true
        scope.launch { scrollState.animateScrollTo(0) }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("Log out?") },
            text = { Text("Are you sure you want to sign out of Nexora Circle?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogout = false
                    FirebaseAuth.getInstance().signOut()
                    photoPath?.let { File(it).delete() }
                    prefs.edit().clear().apply()
                    (context as? Activity)?.recreate()
                }) { Text("Logout", color = LogoutRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- Title + settings ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "My Profile",
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineMedium,
                color = NexoraColors.StarWhite,
            )
            IconButton(onClick = { open(ProfileDestination.Settings) }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = NexoraColors.PlasmaCyan)
            }
        }

        // ---------- Header card ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, NexoraColors.PlasmaCyan.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .border(2.dp, NexoraColors.PlasmaCyan, CircleShape)
                    .padding(5.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(NexoraColors.PlasmaCyan)
                        .clickable(enabled = editing) {
                            picker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
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
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = NexoraColors.SpaceBlack,
                        )
                    }
                }
            }

            if (editing) {
                Spacer(Modifier.height(8.dp))
                Text("Tap photo to change", color = NexoraColors.NebulaPink)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(NAME_MAX) },
                    label = { Text("Display name") },
                    singleLine = false,
                    maxLines = 2,
                    enabled = nameDaysLeft == 0,
                    textStyle = TextStyle(color = NexoraColors.StarWhite),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${name.length}/$NAME_MAX",
                    color = NexoraColors.Muted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
                if (nameDaysLeft > 0) Text(
                    "You can change your name again in $nameDaysLeft day(s)",
                    color = NexoraColors.NebulaPink,
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = cleanUsername(it) },
                    label = { Text("Username") },
                    prefix = { Text(USERNAME_PREFIX, color = NexoraColors.PlasmaCyan) },
                    singleLine = true,
                    isError = usernameErr != null,
                    enabled = usernameDaysLeft == 0,
                    textStyle = TextStyle(color = NexoraColors.StarWhite),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    usernameErr ?: "Lowercase letters, numbers, . and _ only",
                    color = if (usernameErr != null) MaterialTheme.colorScheme.error
                    else NexoraColors.Muted,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (usernameDaysLeft > 0) Text(
                    "You can change your username again in $usernameDaysLeft day(s)",
                    color = NexoraColors.NebulaPink,
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(160) },
                    label = { Text("Bio") },
                    textStyle = TextStyle(color = NexoraColors.StarWhite),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        enabled = canSave,
                        onClick = {
                            val now = System.currentTimeMillis()
                            val newName = name.trim().replace(Regex("\\s+"), " ")
                            val editor = prefs.edit()
                            if (newName != savedName) {
                                if (savedName.isNotBlank()) {
                                    nameChangedAt = now
                                    editor.putLong(KEY_NAME_TIME, now)
                                }
                                savedName = newName
                            }
                            if (username != savedUsername) {
                                if (savedUsername.isNotBlank()) {
                                    usernameChangedAt = now
                                    editor.putLong(KEY_USERNAME_TIME, now)
                                }
                                savedUsername = username
                            }
                            savedBio = bio.trim()
                            editor.putString(KEY_NAME, savedName)
                                .putString(KEY_USERNAME, savedUsername)
                                .putString(KEY_BIO, savedBio)
                                .apply()

                            if (uid != null) {
                                val data = mapOf(
                                    "name" to savedName,
                                    "username" to savedUsername,
                                    "nameLower" to savedName.lowercase(),
                                    "usernameLower" to savedUsername.lowercase(),
                                    "bio" to savedBio,
                                    "nameChangedAt" to nameChangedAt,
                                    "usernameChangedAt" to usernameChangedAt,
                                )
                                scope.launch {
                                    try {
                                        db.collection("users").document(uid)
                                            .set(data, SetOptions.merge()).await()
                                        syncMsg = "Saved to your account"
                                    } catch (e: Exception) {
                                        syncMsg = "Could not sync: ${e.localizedMessage}"
                                    }
                                }
                            }
                            focus.clearFocus()
                            editing = false
                        },
                    ) { Text("Save") }
                    OutlinedButton(onClick = {
                        name = savedName
                        username = savedUsername
                        bio = savedBio
                        focus.clearFocus()
                        editing = false
                    }) { Text("Cancel") }
                }
                if (syncMsg.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(syncMsg, color = NexoraColors.NebulaPink)
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (savedName.isBlank()) "No name set yet" else savedName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexoraColors.StarWhite,
                        textAlign = TextAlign.Center,
                    )
                    if (verified) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = NexoraColors.PlasmaCyan,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    if (savedUsername.isBlank()) "Username not set"
                    else USERNAME_PREFIX + savedUsername,
                    color = NexoraColors.PlasmaCyan,
                    textAlign = TextAlign.Center,
                )
                if (userTitle.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        userTitle,
                        color = NexoraColors.PlasmaCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(NexoraColors.PlasmaCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
                if (savedBio.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(savedBio, color = NexoraColors.Muted, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { startEditing() }) { Text("Edit Profile") }
            }
        }

        // ---------- Social stats ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatItem(postsCount, "Posts", Modifier.weight(1f)) { open(ProfileDestination.Posts) }
            StatItem(followersCount, "Followers", Modifier.weight(1f)) { open(ProfileDestination.Followers) }
            StatItem(followingCount, "Following", Modifier.weight(1f)) { open(ProfileDestination.Following) }
        }

        // ---------- My Nexora ----------
        SectionTitle("My Nexora")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MenuCard(
                Icons.Default.Star, "Achievements", "Badges & trophies",
                NexoraColors.PlasmaCyan, Modifier.weight(1f),
            ) { open(ProfileDestination.Achievements) }
            MenuCard(
                Icons.Default.PlayArrow, "Game Stats", "Arcade levels & scores",
                NexoraColors.NebulaPink, Modifier.weight(1f),
            ) { open(ProfileDestination.GameStats) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MenuCard(
                Icons.Default.Favorite, "Saved", "Saved posts & content",
                NexoraColors.NebulaPink, Modifier.weight(1f),
            ) { open(ProfileDestination.Saved) }
            MenuCard(
                Icons.Default.DateRange, "Activity", "Likes, comments & posts",
                NexoraColors.PlasmaCyan, Modifier.weight(1f),
            ) { open(ProfileDestination.Activity) }
        }

        // ---------- Account ----------
        SectionTitle("Account")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .border(1.dp, NexoraColors.PlasmaCyan.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
        ) {
            SettingsRow(Icons.Default.Person, "Personal Information") { startEditing() }
            SettingsRow(Icons.Default.Lock, "Privacy & Security") { open(ProfileDestination.Privacy) }
            SettingsRow(Icons.Default.Notifications, "Notifications") { open(ProfileDestination.Notifications) }
            SettingsRow(Icons.Default.Face, "Appearance & Theme") { open(ProfileDestination.Appearance) }
            SettingsRow(Icons.Default.Email, "Help & Support") { open(ProfileDestination.Help) }
            SettingsRow(Icons.Default.Info, "About Nexora Circle") { open(ProfileDestination.About) }
        }

        // ---------- Logout ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, LogoutRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .clickable { showLogout = true }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = LogoutRed)
            Spacer(Modifier.width(8.dp))
            Text("Logout", color = LogoutRed, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = NexoraColors.StarWhite,
    )
}

@Composable
private fun StatItem(count: Long, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, NexoraColors.PlasmaCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            count.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NexoraColors.StarWhite,
        )
        Text(label, fontSize = 13.sp, color = NexoraColors.Muted)
    }
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .height(124.dp)
            .clip(shape)
            .background(CardBg)
            .border(1.dp, tint.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        IconBubble(icon, tint)
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NexoraColors.StarWhite)
            Text(subtitle, fontSize = 12.sp, color = NexoraColors.Muted)
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon, NexoraColors.PlasmaCyan)
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 15.sp, color = NexoraColors.StarWhite, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = NexoraColors.Muted)
    }
}
