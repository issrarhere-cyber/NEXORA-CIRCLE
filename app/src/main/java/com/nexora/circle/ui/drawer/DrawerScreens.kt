package com.nexora.circle.ui.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Change this to your real support email. */
private const val SUPPORT_EMAIL = "support@nexora.app"
private const val NOTE_SEP = "\u0001"

/**
 * Global anonymous-mode flag (saved on device).
 * Call AnonymousState.load(context) once at app start, then read AnonymousState.enabled anywhere.
 */
object AnonymousState {
    var enabled by mutableStateOf(false)
        private set
    private var loaded = false

    fun load(context: Context) {
        if (!loaded) {
            enabled = DrawerStore.prefs(context).getBoolean("anonymous", false)
            loaded = true
        }
    }

    fun set(context: Context, value: Boolean) {
        enabled = value
        loaded = true
        DrawerStore.prefs(context).edit().putBoolean("anonymous", value).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerDestinationScreen(
    destination: DrawerDestination,
    onBack: () -> Unit,
    onNavigate: (DrawerDestination) -> Unit = {},
    /** Plug your existing ThemeState picker UI here. */
    themeContent: @Composable () -> Unit = { Text("Theme options will be connected here.") }
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(destination.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (destination) {
                DrawerDestination.Nexus -> NexusContent(onNavigate)
                DrawerDestination.Library -> LibraryContent()
                DrawerDestination.LearnSkills -> LearnSkillsContent()
                DrawerDestination.Theme -> themeContent()
                DrawerDestination.Anonymous -> AnonymousContent()
                DrawerDestination.Settings -> SettingsContent()
                DrawerDestination.Help -> HelpContent()
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

// ---------------------------------------------------------------- NEXUS
@Composable
private fun NexusContent(onNavigate: (DrawerDestination) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { DrawerStore.prefs(ctx) }
    val lessonsDone = prefs.getStringSet("skills_done", emptySet())?.size ?: 0
    val notesCount = (prefs.getString("notes", "") ?: "").split(NOTE_SEP).count { it.isNotBlank() }
    AnonymousState.load(ctx)

    Text("Your hub", style = MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Lessons done", lessonsDone.toString(), Modifier.weight(1f))
        StatCard("Saved notes", notesCount.toString(), Modifier.weight(1f))
        StatCard("Anonymous", if (AnonymousState.enabled) "ON" else "OFF", Modifier.weight(1f))
    }
    Text("Quick access", style = MaterialTheme.typography.titleMedium)
    listOf(
        DrawerDestination.Library,
        DrawerDestination.LearnSkills,
        DrawerDestination.Theme,
        DrawerDestination.Anonymous
    ).forEach { d ->
        OutlinedButton(onClick = { onNavigate(d) }, modifier = Modifier.fillMaxWidth()) {
            Text(d.label)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// -------------------------------------------------------------- LIBRARY
@Composable
private fun LibraryContent() {
    val ctx = LocalContext.current
    val prefs = remember { DrawerStore.prefs(ctx) }
    var notes by remember {
        mutableStateOf((prefs.getString("notes", "") ?: "").split(NOTE_SEP).filter { it.isNotBlank() })
    }
    var draft by remember { mutableStateOf("") }

    fun save(list: List<String>) {
        notes = list
        prefs.edit().putString("notes", list.joinToString(NOTE_SEP)).apply()
    }

    Text("Keep your notes and saved things here.", style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        label = { Text("Write a new note") },
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = {
            val t = draft.trim().replace(NOTE_SEP, " ")
            if (t.isNotEmpty()) {
                save(listOf(t) + notes)
                draft = ""
            }
        },
        enabled = draft.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text("Save") }

    if (notes.isEmpty()) {
        Text("No notes yet.", style = MaterialTheme.typography.bodySmall)
    }
    notes.forEachIndexed { i, n ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { save(notes.filterIndexed { idx, _ -> idx != i }) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

// --------------------------------------------------------- LEARN SKILLS
private data class Skill(val title: String, val desc: String, val lessons: List<Pair<String, String>>)

private val skills = listOf(
    Skill(
        "Communication", "Better chatting and speaking",
        listOf(
            "Listen first" to "Hear the whole message before you reply.",
            "Be short and clear" to "Keep one idea per message.",
            "Mind your tone" to "Tone can read differently in text, so choose words and emoji carefully."
        )
    ),
    Skill(
        "Tech basics", "Getting started with digital skills",
        listOf(
            "Strong passwords" to "Use long passwords, a different one for each app.",
            "Online safety" to "Never share unknown links or OTP codes.",
            "Learn new apps" to "Try one new tool for 15 minutes a day."
        )
    ),
    Skill(
        "Creativity", "From idea to finished work",
        listOf(
            "Write ideas down" to "Don't keep them in your head, note them right away.",
            "Learn by studying" to "When you like something, figure out why it works.",
            "Make a little daily" to "Even something small, create something every day."
        )
    ),
    Skill(
        "Productivity", "Time and focus",
        listOf(
            "Top 3 tasks" to "Pick the 3 most important tasks of the day first.",
            "25 minute focus" to "Work 25 minutes, then rest 5.",
            "Keep your phone away" to "Turn off notifications during focus time."
        )
    )
)

@Composable
private fun LearnSkillsContent() {
    val ctx = LocalContext.current
    val prefs = remember { DrawerStore.prefs(ctx) }
    var done by remember {
        mutableStateOf<Set<String>>(prefs.getStringSet("skills_done", emptySet()) ?: emptySet())
    }
    var open by remember { mutableStateOf<String?>(null) }

    fun toggle(id: String) {
        val n = if (id in done) done - id else done + id
        done = n
        prefs.edit().putStringSet("skills_done", HashSet(n)).apply()
    }

    skills.forEach { skill ->
        val ids = skill.lessons.indices.map { "${skill.title}:$it" }
        val count = ids.count { it in done }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Column(Modifier.clickable { open = if (open == skill.title) null else skill.title }) {
                    Text(skill.title, style = MaterialTheme.typography.titleMedium)
                    Text(skill.desc, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { count.toFloat() / skill.lessons.size },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("$count/${skill.lessons.size} complete", style = MaterialTheme.typography.labelSmall)
                }
                if (open == skill.title) {
                    Spacer(Modifier.height(8.dp))
                    skill.lessons.forEachIndexed { i, (t, tip) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = ids[i] in done, onCheckedChange = { toggle(ids[i]) })
                            Column {
                                Text(t, style = MaterialTheme.typography.bodyMedium)
                                Text(tip, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ ANONYMOUS
@Composable
private fun AnonymousContent() {
    val ctx = LocalContext.current
    AnonymousState.load(ctx)
    SwitchRow(
        "Anonymous Mode",
        if (AnonymousState.enabled) "ON: your identity is hidden" else "OFF: your normal profile is visible",
        AnonymousState.enabled
    ) { AnonymousState.set(ctx, it) }
    InfoCard(
        "How it works",
        "When ON, your name and photo should not be shown to others in the app. " +
            "The flag is available as AnonymousState.enabled and must be checked in posts, comments and chat."
    )
}

// ------------------------------------------------------------- SETTINGS
@Composable
private fun SettingsContent() {
    val ctx = LocalContext.current
    val (notif, setNotif) = rememberBoolPref("set_notifications", true)
    val (sound, setSound) = rememberBoolPref("set_sounds", true)
    val (vibe, setVibe) = rememberBoolPref("set_vibration", true)
    val version = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "-" } catch (e: Exception) { "-" }
    }

    SwitchRow("Notifications", "Alerts for new messages and updates", notif, setNotif)
    SwitchRow("Sounds", "Message tone and app sounds", sound, setSound)
    SwitchRow("Vibration", "Vibrate on taps and alerts", vibe, setVibe)
    InfoCard("Version", "NEXORA Circle $version")
}

// ----------------------------------------------------------------- HELP
private val faqs = listOf(
    "How do I edit my profile?" to "Open the Me tab and tap Edit Profile.",
    "What is Anonymous Mode?" to "Turn it on from the drawer to hide your identity.",
    "How do I change the theme?" to "Open Theme from the drawer and pick your mode, accent and font.",
    "How do I clear or delete a chat?" to "Use the top-right menu in a chat to clear it, or long-press a message to delete it."
)

@Composable
private fun HelpContent() {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf<Int?>(null) }

    Text("Frequently asked questions", style = MaterialTheme.typography.titleMedium)
    faqs.forEachIndexed { i, (q, a) ->
        Card(Modifier.fillMaxWidth().clickable { open = if (open == i) null else i }) {
            Column(Modifier.padding(16.dp)) {
                Text(q, style = MaterialTheme.typography.titleSmall)
                if (open == i) {
                    Spacer(Modifier.height(4.dp))
                    Text(a, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "NEXORA Circle support")
            }
            try {
                ctx.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(ctx, "No email app found", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Contact support") }
}
