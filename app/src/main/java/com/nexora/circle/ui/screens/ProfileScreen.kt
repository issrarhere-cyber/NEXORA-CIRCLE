package com.nexora.circle.ui.screens

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nexora.circle.ui.components.GlassCard
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

private fun daysLeft(lastChange: Long): Int {
    if (lastChange == 0L) return 0
    val left = lastChange + WEEK_MS - System.currentTimeMillis()
    return if (left <= 0) 0 else ((left + 86_399_999L) / 86_400_000L).toInt()
}

private const val NAME_MAX = 50
private const val USERNAME_PREFIX = "X."
private const val USERNAME_MIN = 3
private const val USERNAME_MAX = 28

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
fun ProfileScreen() {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid
    val db = remember { FirebaseFirestore.getInstance() }

    var savedName by remember { mutableStateOf(prefs.getString(KEY_NAME, "") ?: "") }
    var savedUsername by remember { mutableStateOf(prefs.getString(KEY_USERNAME, "") ?: "") }
    var savedBio by remember { mutableStateOf(prefs.getString(KEY_BIO, "") ?: "") }
    var name by remember { mutableStateOf(savedName) }
    var username by remember { mutableStateOf(savedUsername) }
    var bio by remember { mutableStateOf(savedBio) }
    var editing by remember { mutableStateOf(false) }
    var syncMsg by remember { mutableStateOf("") }

    var nameChangedAt by remember { mutableStateOf(prefs.getLong(KEY_NAME_TIME, 0L)) }
    var usernameChangedAt by remember { mutableStateOf(prefs.getLong(KEY_USERNAME_TIME, 0L)) }

    LaunchedEffect(uid) {
        if (uid == null) return@LaunchedEffect
        try {
            val snap = db.collection("users").document(uid).get().await()
            if (snap.exists()) {
                savedName = snap.getString("name") ?: ""
                savedUsername = snap.getString("username") ?: ""
                savedBio = snap.getString("bio") ?: ""
                nameChangedAt = snap.getLong("nameChangedAt") ?: 0L
                usernameChangedAt = snap.getLong("usernameChangedAt") ?: 0L
                name = savedName
                username = savedUsername
                bio = savedBio
                prefs.edit()
                    .putString(KEY_NAME, savedName)
                    .putString(KEY_USERNAME, savedUsername)
                    .putString(KEY_BIO, savedBio)
                    .putLong(KEY_NAME_TIME, nameChangedAt)
                    .putLong(KEY_USERNAME_TIME, usernameChangedAt)
                    .apply()
            } else {
                if (savedName.isBlank()) {
                    val dn = user?.displayName.orEmpty()
                    if (dn.isNotBlank()) {
                        savedName = dn
                        name = dn
                    }
                }
                if (savedName.isNotBlank() || savedUsername.isNotBlank()) {
                    db.collection("users").document(uid).set(
                        mapOf(
                            "name" to savedName,
                            "username" to savedUsername,
                            "bio" to savedBio,
                            "nameChangedAt" to nameChangedAt,
                            "usernameChangedAt" to usernameChangedAt,
                        ),
                        SetOptions.merge()
                    ).await()
                }
            }
        } catch (e: Exception) {
            syncMsg = "Offline: showing saved data"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, color = NexoraColors.StarWhite)

        GlassCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
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
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (savedName.isBlank()) "No name set yet" else savedName,
                        style = MaterialTheme.typography.titleLarge,
                        color = NexoraColors.StarWhite,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (savedUsername.isBlank()) "Username not set"
                        else USERNAME_PREFIX + savedUsername,
                        color = NexoraColors.PlasmaCyan,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (savedBio.isBlank()) "No bio yet" else savedBio,
                        color = NexoraColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        name = savedName
                        username = savedUsername
                        bio = savedBio
                        editing = true
                    }) { Text("Edit profile") }
                }
            }
        }

        GlassCard {
            Text("About", style = MaterialTheme.typography.titleSmall, color = NexoraColors.NebulaPink)
            Spacer(Modifier.height(6.dp))
            Text("Nexora Circle", color = NexoraColors.StarWhite)
            Text(
                user?.email ?: "Signed in",
                color = NexoraColors.Muted,
            )
            Text(
                "Profile is saved to your account. Photo stays on this device.",
                color = NexoraColors.Muted,
            )
            if (syncMsg.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(syncMsg, color = NexoraColors.NebulaPink)
            }
        }

        OutlinedButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                photoPath?.let { File(it).delete() }
                prefs.edit().clear().apply()
                (context as? Activity)?.recreate()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Logout") }

        Spacer(Modifier.height(24.dp))
    }
}
