package com.nexora.circle.ui.drawer

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.screens.ProfileSubScreen
import com.nexora.circle.ui.theme.NexoraColors

private val DangerRed = Color(0xFFFF4D5E)

// -------------------------------------------------------------------------------------
// Settings: Privacy, Notifications, Account, Help
// -------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var page by remember { mutableStateOf("") }

    when (page) {
        "privacy" -> ProfileSubScreen(label = "Privacy & Security", onBack = { page = "" })
        "notifications" -> ProfileSubScreen(label = "Notifications", onBack = { page = "" })
        "account" -> AccountScreen(onBack = { page = "" })
        "help" -> HelpCentreScreen(onBack = { page = "" })
        else -> DrawerScreenScaffold(title = "Settings", onBack = onBack) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsRow("🔒", "Privacy", "Private account, online status, password") { page = "privacy" }
                SettingsRow("🔔", "Notifications", "Likes, comments, followers, messages") { page = "notifications" }
                SettingsRow("👤", "Account", "Your email, password and log out") { page = "account" }
                SettingsRow("❓", "Help", "FAQ and contact support") { page = "help" }
            }
        }
    }
}

@Composable
private fun SettingsRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    NxCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    title,
                    color = NexoraColors.StarWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(subtitle, color = NexoraColors.Muted.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            Text("›", color = NexoraColors.Muted, fontSize = 26.sp)
        }
    }
}

@Composable
private fun AccountScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val email = user?.email.orEmpty()
    var showLogout by remember { mutableStateOf(false) }

    DrawerScreenScaffold(title = "Account", onBack = onBack) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NxCard {
                Text("Email", color = NexoraColors.Muted.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(
                    email.ifBlank { "No email on this account" },
                    color = NexoraColors.StarWhite,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text("Anonymous Mode", color = NexoraColors.Muted.copy(alpha = 0.7f), fontSize = 12.sp)
                Text(
                    if (AnonymousMode.enabled) "ON" else "OFF",
                    color = NexoraColors.StarWhite,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "To change your name, username, bio or photo, use Edit Profile in your profile.",
                    color = NexoraColors.Muted.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
            Button(
                onClick = {
                    if (email.isBlank()) {
                        Toast.makeText(context, "No email found on this account", Toast.LENGTH_SHORT).show()
                    } else {
                        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener { t ->
                                val msg = if (t.isSuccessful) "Reset link sent to $email" else "Could not send the email"
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Change password (email link)") }
            OutlinedButton(
                onClick = { showLogout = true },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Log out", color = DangerRed) }
        }
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
                    context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE).edit().clear().apply()
                    (context as? Activity)?.recreate()
                }) { Text("Logout", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) { Text("Cancel") }
            },
        )
    }
}

// -------------------------------------------------------------------------------------
// Help Centre: FAQ + Contact Support
// -------------------------------------------------------------------------------------

private data class Faq(val q: String, val a: String)

private val FAQS = listOf(
    Faq(
        "How do I change my name, username or photo?",
        "Open your profile (Profile in the menu, or the Me tab) and tap Edit. Name and username can be changed once every 7 days.",
    ),
    Faq(
        "What is Anonymous Mode?",
        "When it is ON, your name and photo are replaced by \"Anonymous User\" and a grey avatar in posts, comments and Nexus. Turn it off in the menu to go back to normal.",
    ),
    Faq(
        "How do I follow someone?",
        "Search for the person in Orbit, open their profile and use the follow or Signal buttons.",
    ),
    Faq(
        "How do I message someone?",
        "Open their profile and tap Beam, or open the Comms tab and pick a chat.",
    ),
    Faq(
        "How does NEXORA NEXUS work?",
        "Nexus rooms are live rooms with text chat and speaker control. Admins create a room. Everyone can join. Listeners tap Raise hand, and the admin decides who may speak, mute or remove.",
    ),
    Faq(
        "How do I change the colours, dark mode or text size?",
        "Open Customize Theme in the menu. Changes apply to the whole app straight away and are saved on your phone.",
    ),
    Faq(
        "I forgot my password.",
        "Open Settings > Account > Change password. A reset link is sent to your email.",
    ),
    Faq(
        "How do I log out?",
        "Open Settings > Account > Log out.",
    ),
)

@Composable
fun HelpCentreScreen(onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) } // 0 = FAQ, 1 = Contact

    DrawerScreenScaffold(title = "Help Centre", onBack = onBack) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NxChip("FAQ", tab == 0, { tab = 0 })
            NxChip("Contact Support", tab == 1, { tab = 1 })
        }
        if (tab == 0) {
            FaqList()
        } else {
            ContactSupport()
        }
    }
}

@Composable
private fun ColumnFaqItem(faq: Faq) {
    var open by remember { mutableStateOf(false) }
    NxCard(onClick = { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                faq.q,
                color = NexoraColors.StarWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(if (open) "−" else "+", color = NexoraColors.PlasmaCyan, fontSize = 24.sp)
        }
        if (open) {
            Spacer(Modifier.height(8.dp))
            Text(faq.a, color = NexoraColors.Muted.copy(alpha = 0.85f), fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.FaqList() {
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FAQS.forEach { ColumnFaqItem(it) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ContactSupport() {
    val context = LocalContext.current
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Tell us what went wrong or what you need. Your message goes straight to the Nexora Circle team.",
            color = NexoraColors.Muted.copy(alpha = 0.75f),
            fontSize = 14.sp,
        )
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it.take(100) },
            label = { Text("Subject") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = message,
            onValueChange = { message = it.take(2000) },
            label = { Text("Your message") },
            minLines = 5,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth(),
        )
        if (status.isNotEmpty()) {
            Text(status, color = if (failed) DangerRed else NexoraColors.PlasmaCyan, fontSize = 14.sp)
        }
        Button(
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    failed = true
                    status = "Please sign in first."
                } else if (subject.isBlank() || message.isBlank()) {
                    failed = true
                    status = "Please fill in the subject and the message."
                } else {
                    busy = true
                    failed = false
                    status = ""
                    val profile = context.getSharedPreferences("nexora_profile", Context.MODE_PRIVATE)
                    FirebaseFirestore.getInstance().collection("supportTickets").add(
                        hashMapOf<String, Any>(
                            "uid" to user.uid,
                            "email" to user.email.orEmpty(),
                            "name" to profile.getString("name", "").orEmpty(),
                            "subject" to subject.trim(),
                            "message" to message.trim(),
                            "time" to System.currentTimeMillis(),
                            "status" to "open",
                        ),
                    ).addOnSuccessListener {
                        busy = false
                        failed = false
                        status = "Sent. Thank you, we will look at it."
                        subject = ""
                        message = ""
                    }.addOnFailureListener { e ->
                        busy = false
                        failed = true
                        status = "Could not send: ${e.localizedMessage}. If this says permission denied, add the supportTickets rule to Firestore (see NEXUS_AND_ANON_RULES.txt)."
                    }
                }
            },
        ) { Text(if (busy) "Sending..." else "Send message") }
        Spacer(Modifier.height(12.dp))
    }
}
