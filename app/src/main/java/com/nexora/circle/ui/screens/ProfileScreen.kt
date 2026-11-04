package com.nexora.circle.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.nexora.circle.ui.components.GlassCard
import com.nexora.circle.ui.theme.NexoraColors
import java.io.File

private const val PREFS_NAME = "nexora_profile"
private const val KEY_NAME = "name"
private const val KEY_BIO = "bio"
private const val KEY_PHOTO = "photo_path"

@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var savedName by remember { mutableStateOf(prefs.getString(KEY_NAME, "") ?: "") }
    var savedBio by remember { mutableStateOf(prefs.getString(KEY_BIO, "") ?: "") }
    var name by remember { mutableStateOf(savedName) }
    var bio by remember { mutableStateOf(savedBio) }
    var editing by remember { mutableStateOf(false) }

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
                        onValueChange = { name = it.take(40) },
                        label = { Text("Display name") },
                        singleLine = true,
                        textStyle = TextStyle(color = NexoraColors.StarWhite),
                        modifier = Modifier.fillMaxWidth(),
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
                        Button(onClick = {
                            savedName = name.trim()
                            savedBio = bio.trim()
                            prefs.edit()
                                .putString(KEY_NAME, savedName)
                                .putString(KEY_BIO, savedBio)
                                .apply()
                            focus.clearFocus()
                            editing = false
                        }) { Text("Save") }
                        OutlinedButton(onClick = {
                            name = savedName
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (savedBio.isBlank()) "No bio yet" else savedBio,
                        color = NexoraColors.Muted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = {
                        name = savedName
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
            Text("This profile is stored on this device only.", color = NexoraColors.Muted)
        }
    }
}
