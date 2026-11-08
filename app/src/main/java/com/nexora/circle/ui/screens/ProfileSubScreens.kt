package com.nexora.circle.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.nexora.circle.ui.theme.NexoraColors

@Composable
fun ProfileSubScreen(label: String, onBack: () -> Unit) {
    val l = label.lowercase()
    when {
        l.contains("achiev") -> PfScaffold(label, onBack) {
            PfEmpty("🏆", "No achievements yet. Keep using Nexora Circle and your badges will show up here.")
        }
        l.contains("game") -> PfScaffold(label, onBack) {
            PfEmpty("🎮", "No game stats yet. Play games in Arcade and your scores will appear here.")
        }
        l.contains("saved") -> PfScaffold(label, onBack) {
            PfEmpty("🔖", "Nothing saved yet. Posts you save will appear here.")
        }
        l.contains("activity") -> PfScaffold(label, onBack) {
            PfEmpty("⚡", "No activity yet. Your likes and comments history will appear here.")
        }
        l.contains("personal") -> PfScaffold(label, onBack) { PfPersonalInfo() }
        l.contains("privacy") -> PfScaffold(label, onBack) { PfPrivacy() }
        l.contains("notif") -> PfScaffold(label, onBack) {
            PfCard {
                PfToggle("n_likes", "Likes")
                PfToggle("n_comments", "Comments")
                PfToggle("n_follow", "New followers")
                PfToggle("n_messages", "Messages")
            }
        }
        l.contains("appear") -> PfScaffold(label, onBack) { PfAppearance() }
        l.contains("help") -> PfScaffold(label, onBack) { PfHelp() }
        l.contains("about") -> PfScaffold(label, onBack) {
            PfCard {
                Text("Nexora Circle", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Version 1.0", color = NexoraColors.Muted)
                Spacer(Modifier.height(12.dp))
                Text(
                    "A space-themed social app: Orbit, Comms, Arcade and Nexora-X in one place.",
                    color = Color.White,
                )
            }
        }
        else -> PfScaffold(label, onBack) {
            PfEmpty("🚧", "This screen is not connected yet.")
        }
    }
}

@Composable
private fun PfScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back", color = NexoraColors.PlasmaCyan) }
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
    }
}

@Composable
private fun PfCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(NexoraColors.Panel, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun PfEmpty(emoji: String, text: String) {
    PfCard {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(text, color = NexoraColors.Muted)
    }
}

@Composable
private fun PfToggle(key: String, label: String, default: Boolean = true) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("nexora_prefs", 0) }
    var on by remember { mutableStateOf(prefs.getBoolean(key, default)) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = {
            on = it
            prefs.edit().putBoolean(key, it).apply()
        })
    }
}

@Composable
private fun PfInfoRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, color = NexoraColors.Muted, fontSize = 12.sp)
        Text(value.ifEmpty { "-" }, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun PfPersonalInfo() {
    val user = FirebaseAuth.getInstance().currentUser
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val uid = user?.uid ?: return@LaunchedEffect
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { d ->
                name = d.getString("name").orEmpty()
                username = d.getString("username").orEmpty()
                bio = d.getString("bio").orEmpty()
            }
    }
    PfCard {
        PfInfoRow("Name", name)
        PfInfoRow("Username", username)
        PfInfoRow("Bio", bio)
        PfInfoRow("Email", user?.email.orEmpty())
    }
    Spacer(Modifier.height(8.dp))
    Text("To change your name, username or bio, use Edit Profile.", color = NexoraColors.Muted)
}

@Composable
private fun PfPrivacy() {
    val ctx = LocalContext.current
    val email = FirebaseAuth.getInstance().currentUser?.email
    PfCard {
        PfToggle("p_private", "Private account", default = false)
        PfToggle("p_online", "Show online status")
    }
    Spacer(Modifier.height(16.dp))
    Button(onClick = {
        if (email.isNullOrEmpty()) {
            Toast.makeText(ctx, "No email found", Toast.LENGTH_SHORT).show()
        } else {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { t ->
                val msg = if (t.isSuccessful) "Reset link sent to $email" else "Could not send the email"
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
            }
        }
    }) { Text("Change password (email link)") }
}

@Composable
private fun PfAppearance() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("nexora_prefs", 0) }
    var mode by remember { mutableStateOf(prefs.getString("theme_mode", "dark") ?: "dark") }
    val options = listOf("dark" to "Dark", "light" to "Light", "system" to "System default")
    PfCard {
        options.forEach { (key, text) ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == key, onClick = {
                    mode = key
                    prefs.edit().putString("theme_mode", key).apply()
                })
                Text(text, color = Color.White)
            }
        }
    }
}

@Composable
private fun PfHelp() {
    val faqs = listOf(
        "How do I change my profile?" to "Open Edit Profile in the Me tab and update your name, username or bio.",
        "Forgot your password?" to "Go to Privacy & Security and request a reset link by email.",
        "How do I message someone?" to "Open their profile in Orbit and tap Beam, or use the Comms tab.",
    )
    faqs.forEach { (q, a) ->
        PfCard {
            Text(q, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(a, color = NexoraColors.Muted)
        }
        Spacer(Modifier.height(10.dp))
    }
    Text("Need more help? Contact the developer.", color = NexoraColors.Muted)
}
