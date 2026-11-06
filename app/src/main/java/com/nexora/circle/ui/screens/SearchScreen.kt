package com.nexora.circle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.nexora.circle.ui.theme.NexoraColors
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(onBack: () -> Unit, onUserClick: (String) -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<DocumentSnapshot>()) }
    var msg by remember { mutableStateOf("") }

    LaunchedEffect(query) {
        val q = query.trim().lowercase().removePrefix("x.")
        if (q.isEmpty()) {
            results = emptyList()
            msg = ""
            return@LaunchedEffect
        }
        msg = "Searching..."
        delay(400)
        db.collection("users")
            .whereGreaterThanOrEqualTo("usernameLower", q)
            .whereLessThan("usernameLower", q + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener {
                results = it.documents
                msg = if (it.isEmpty) "No users found" else ""
            }
            .addOnFailureListener { msg = "Search error: ${it.localizedMessage}" }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("Back", color = NexoraColors.PlasmaCyan) }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search username...") },
            singleLine = true,
            textStyle = TextStyle(color = NexoraColors.StarWhite),
            modifier = Modifier.fillMaxWidth(),
        )
        if (msg.isNotEmpty()) Text(msg, color = NexoraColors.Muted, modifier = Modifier.padding(8.dp))
        LazyColumn {
            items(results, key = { it.id }) { doc ->
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { onUserClick(doc.id) }
                        .padding(vertical = 12.dp)
                ) {
                    Text(doc.getString("name") ?: "", color = NexoraColors.StarWhite)
                    Text("X." + (doc.getString("username") ?: ""), color = NexoraColors.PlasmaCyan)
                }
            }
        }
    }
}
