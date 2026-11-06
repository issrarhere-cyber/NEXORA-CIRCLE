package com.nexora.circle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.nexora.circle.ui.AuthScreen
import com.nexora.circle.ui.NexoraApp
import com.nexora.circle.ui.theme.NexoraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NexoraTheme {
                var signedIn by remember {
                    mutableStateOf(FirebaseAuth.getInstance().currentUser != null)
                }
                if (signedIn) {
                    NexoraApp()
                } else {
                    AuthScreen(onSignedIn = { signedIn = true })
                }
            }
        }
    }
}
