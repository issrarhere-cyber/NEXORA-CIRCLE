package com.nexora.circle.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(onBack: () -> Unit, onUserClick: (String) -> Unit) {
    val db = Firebase.firestore
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(listOf<DocumentSnapshot>()) }

    LaunchedEffect(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) { results = emptyList(); return@LaunchedEffect }
        delay(400)
        db.collection("users")
            .whereGreaterThanOrEqualTo("handleLower", q)
            .whereLessThan("handleLower", q + "\uf8ff")
            .limit(20)
            .get()
            .addOnSuccessListener { results = it.documents }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("Back") }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search Nexora...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn {
            items(results) { doc ->
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { onUserClick(doc.id) }
                        .padding(vertical = 12.dp)
                ) {
                    Text(doc.getString("name") ?: "")
                    Text(doc.getString("handle") ?: "")
                }
            }
        }
    }
}
