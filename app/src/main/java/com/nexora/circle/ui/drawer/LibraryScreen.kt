package com.nexora.circle.ui.drawer

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.circle.ui.theme.NexoraColors
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One book or note in My Library. kind = "book" or "note". */
data class LibItem(
    val id: String,
    val kind: String,
    val title: String,
    val author: String,
    val body: String,
    val time: Long,
)

/** Saves the library on this phone (SharedPreferences, as JSON). */
private object LibraryStore {
    private const val PREFS = "nexora_library"
    private const val KEY = "items"

    fun load(context: Context): List<LibItem> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY, null)
        if (raw == null) {
            val seeded = seed()
            save(context, seeded)
            return seeded
        }
        val out = ArrayList<LibItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    LibItem(
                        id = o.optString("id"),
                        kind = o.optString("kind", "book"),
                        title = o.optString("title"),
                        author = o.optString("author"),
                        body = o.optString("body"),
                        time = o.optLong("time"),
                    ),
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return out
    }

    fun save(context: Context, items: List<LibItem>) {
        val arr = JSONArray()
        for (item in items) {
            val o = JSONObject()
            o.put("id", item.id)
            o.put("kind", item.kind)
            o.put("title", item.title)
            o.put("author", item.author)
            o.put("body", item.body)
            o.put("time", item.time)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    private fun seed(): List<LibItem> {
        val now = System.currentTimeMillis()
        return listOf(
            LibItem(
                id = UUID.randomUUID().toString(),
                kind = "book",
                title = "Atomic Habits",
                author = "James Clear",
                time = now - 3000,
                body = "SAMPLE ENTRY. These are short summary notes written for this app, not the text of the book.\n\n" +
                    "Main ideas\n" +
                    "- Small habits compound. Getting a little better every day adds up to a big change over months and years.\n" +
                    "- Focus on systems, not only goals. A goal is the result you want; a system is the daily process that gets you there.\n" +
                    "- Build identity-based habits. Decide who you want to become and cast a small vote for that person with each action.\n" +
                    "- Four steps to build a habit: make it obvious, make it attractive, make it easy, make it satisfying.\n" +
                    "- To break a bad habit, flip the steps: make it invisible, unattractive, difficult and unsatisfying.\n" +
                    "- Habit stacking: attach a new habit to one you already do every day.\n\n" +
                    "Tap Edit to add your own notes and highlights.",
            ),
            LibItem(
                id = UUID.randomUUID().toString(),
                kind = "book",
                title = "Rich Dad Poor Dad",
                author = "Robert Kiyosaki",
                time = now - 2000,
                body = "SAMPLE ENTRY. These are short summary notes written for this app, not the text of the book.\n\n" +
                    "Main ideas\n" +
                    "- Assets put money in your pocket; liabilities take money out. Learn to tell them apart.\n" +
                    "- Financial education matters as much as school education. Learn how money works.\n" +
                    "- Do not only work for money. Make money work for you through investing and building businesses.\n" +
                    "- Income from a job and income from assets are different things; grow the second one.\n" +
                    "- Keep expenses under control as your income grows.\n" +
                    "- Fear and doubt hold many people back. Learn, act, and learn again from mistakes.\n\n" +
                    "Tap Edit to add your own notes and highlights.",
            ),
            LibItem(
                id = UUID.randomUUID().toString(),
                kind = "book",
                title = "Quran Notes",
                author = "My notes",
                time = now - 1000,
                body = "This is your personal space for Quran study notes.\n\n" +
                    "Tap Edit and write the surah or verse you are reading, the translation or tafsir you follow, " +
                    "and what you learned or want to remember. Everything stays on your phone.",
            ),
            LibItem(
                id = UUID.randomUUID().toString(),
                kind = "note",
                title = "Welcome to My Library",
                author = "",
                time = now,
                body = "Use Add Note for quick thoughts, ideas and reminders.\n" +
                    "Use Add Book to save a book or article: paste its text or import a .txt file, then tap Read.\n" +
                    "Use the search box to find anything by title, author or text.",
            ),
        )
    }
}

@Composable
fun MyLibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val library = remember { mutableStateListOf<LibItem>().apply { addAll(LibraryStore.load(context)) } }

    var tab by remember { mutableStateOf(0) } // 0 = Books, 1 = Notes
    var query by remember { mutableStateOf("") }
    var readingId by remember { mutableStateOf("") }
    var editorOpen by remember { mutableStateOf(false) }
    var editorKind by remember { mutableStateOf("book") }
    var editorTarget by remember { mutableStateOf<LibItem?>(null) }
    var deleteTarget by remember { mutableStateOf<LibItem?>(null) }

    fun persist() {
        LibraryStore.save(context, library.toList())
    }

    fun openEditor(kind: String, target: LibItem?) {
        editorKind = kind
        editorTarget = target
        editorOpen = true
    }

    val reading = library.firstOrNull { it.id == readingId }

    if (reading != null) {
        LibraryReader(
            item = reading,
            onBack = { readingId = "" },
            onEdit = { openEditor(reading.kind, reading) },
            onDelete = { deleteTarget = reading },
        )
    } else {
        val q = query.trim()
        val shown = if (q.isBlank()) {
            library.filter { it.kind == (if (tab == 0) "book" else "note") }
        } else {
            library.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.author.contains(q, ignoreCase = true) ||
                    it.body.contains(q, ignoreCase = true)
            }
        }
        val bookCount = library.count { it.kind == "book" }
        val noteCount = library.count { it.kind == "note" }

        DrawerScreenScaffold(title = "My Library", onBack = onBack) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { openEditor("book", null) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Add Book") }
                    OutlinedButton(
                        onClick = { openEditor("note", null) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Add Note") }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("Search books and notes") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                )
                Spacer(Modifier.height(10.dp))
                if (q.isBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NxChip("Books ($bookCount)", tab == 0, { tab = 0 })
                        NxChip("Notes ($noteCount)", tab == 1, { tab = 1 })
                    }
                } else {
                    Text(
                        "${shown.size} result(s) for \"$q\"",
                        color = NexoraColors.Muted.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                    )
                }
            }

            if (shown.isEmpty()) {
                if (q.isBlank()) {
                    NxEmpty(
                        if (tab == 0) "📚" else "📝",
                        if (tab == 0) "No books yet. Tap Add Book to save one." else "No notes yet. Tap Add Note to write one.",
                    )
                } else {
                    NxEmpty("🔎", "Nothing matches your search.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.id }) { item ->
                        LibraryCard(
                            item = item,
                            onRead = { readingId = item.id },
                            onDelete = { deleteTarget = item },
                        )
                    }
                }
            }
        }
    }

    // ---- Add / edit dialog ----
    if (editorOpen) {
        LibraryEditor(
            kind = editorKind,
            initial = editorTarget,
            onDismiss = { editorOpen = false },
            onSave = { title, author, body ->
                val old = editorTarget
                if (old == null) {
                    library.add(
                        0,
                        LibItem(
                            id = UUID.randomUUID().toString(),
                            kind = editorKind,
                            title = title,
                            author = author,
                            body = body,
                            time = System.currentTimeMillis(),
                        ),
                    )
                    tab = if (editorKind == "book") 0 else 1
                } else {
                    val index = library.indexOfFirst { it.id == old.id }
                    if (index >= 0) {
                        library[index] = old.copy(title = title, author = author, body = body)
                    }
                }
                persist()
                editorOpen = false
            },
        )
    }

    // ---- Delete confirmation ----
    val toDelete = deleteTarget
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${toDelete.title}\"?") },
            text = { Text("This removes it from your library. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    library.removeAll { it.id == toDelete.id }
                    persist()
                    if (readingId == toDelete.id) readingId = ""
                    deleteTarget = null
                }) { Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFFF4D5E)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun LibraryCard(item: LibItem, onRead: () -> Unit, onDelete: () -> Unit) {
    NxCard(onClick = onRead) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (item.kind == "book") "📘" else "📝", fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    item.title,
                    color = NexoraColors.StarWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.author.isNotBlank()) {
                    Text(
                        item.author,
                        color = NexoraColors.PlasmaCyan,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            item.body.replace("\n", " "),
            color = NexoraColors.Muted.copy(alpha = 0.75f),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRead) { Text("Read") }
            TextButton(onClick = onDelete) {
                Text("Delete", color = androidx.compose.ui.graphics.Color(0xFFFF4D5E))
            }
        }
    }
}

@Composable
private fun LibraryReader(
    item: LibItem,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var textSize by remember { mutableStateOf(17) }
    DrawerScreenScaffold(
        title = item.title,
        onBack = onBack,
        actions = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = NexoraColors.StarWhite)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = NexoraColors.StarWhite)
            }
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NxChip("A-", false, { if (textSize > 13) textSize -= 2 })
            NxChip("A+", false, { if (textSize < 29) textSize += 2 })
            if (item.author.isNotBlank()) {
                Text(
                    "  by ${item.author}",
                    color = NexoraColors.PlasmaCyan,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                item.body.ifBlank { "This ${item.kind} is empty. Tap the pencil to add text." },
                color = NexoraColors.StarWhite,
                fontSize = textSize.sp,
                lineHeight = (textSize * 1.55f).sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LibraryEditor(
    kind: String,
    initial: LibItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, author: String, body: String) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var author by remember { mutableStateOf(initial?.author.orEmpty()) }
    var body by remember { mutableStateOf(initial?.body.orEmpty()) }
    var error by remember { mutableStateOf("") }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                }.orEmpty()
                if (text.isBlank()) {
                    Toast.makeText(context, "That file is empty", Toast.LENGTH_SHORT).show()
                } else {
                    body = text.take(500_000)
                    if (title.isBlank()) {
                        title = (uri.lastPathSegment ?: "Imported book")
                            .substringAfterLast('/')
                            .removeSuffix(".txt")
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read that file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isBook = kind == "book"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    initial != null -> if (isBook) "Edit book" else "Edit note"
                    isBook -> "Add book"
                    else -> "Add note"
                },
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120); error = "" },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isBook) {
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it.take(80) },
                        label = { Text("Author (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(if (isBook) "Text or your notes" else "Note") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isBook) {
                    OutlinedButton(
                        onClick = { importer.launch(arrayOf("text/plain")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Import a .txt file") }
                }
                if (error.isNotEmpty()) {
                    Text(error, color = androidx.compose.ui.graphics.Color(0xFFFF4D5E), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    error = "Please write a title"
                } else {
                    onSave(title.trim(), author.trim(), body)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
