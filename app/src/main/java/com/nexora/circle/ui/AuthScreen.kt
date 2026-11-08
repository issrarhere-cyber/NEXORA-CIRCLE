package com.nexora.circle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.nexora.circle.security.AccountSecurity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val Cyan = Color(0xFF00F0FF)
private val Bg = Color(0xFF0B0B1E)

// Set to false if you want users to log in without verifying their email.
private const val REQUIRE_EMAIL_VERIFICATION = true

@Composable
fun AuthScreen(onSignedIn: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Cyan,
        unfocusedBorderColor = Color(0xFF4A3F8F),
        focusedLabelColor = Cyan,
        unfocusedLabelColor = Color(0xFF9A93D0),
        cursorColor = Cyan
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))
        Text(
            "Nexora Circle",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (isLogin) "Welcome back" else "Create your account",
            color = Cyan,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(28.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (min 6)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        if (isLogin) {
            TextButton(
                onClick = {
                    error = ""
                    info = ""
                    if (email.isBlank()) {
                        error = "Enter your email first, then tap Forgot password."
                        return@TextButton
                    }
                    loading = true
                    scope.launch {
                        // Same message either way, so nobody can check which emails have accounts.
                        AccountSecurity.sendPasswordReset(email)
                        info = "If an account exists for ${email.trim()}, a password reset link has been sent."
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot password?", color = Cyan, fontSize = 14.sp)
            }
        }

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = Color(0xFFFF6B8A), fontSize = 14.sp)
        }
        if (info.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(info, color = Cyan, fontSize = 14.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                error = ""
                info = ""
                if (email.isBlank() || password.length < 6 || (!isLogin && name.isBlank())) {
                    error = "Please fill in all fields (password must be at least 6 characters)."
                    return@Button
                }
                loading = true
                scope.launch {
                    try {
                        val auth = FirebaseAuth.getInstance()
                        if (isLogin) {
                            auth.signInWithEmailAndPassword(email.trim(), password).await()
                            val user = auth.currentUser
                            if (REQUIRE_EMAIL_VERIFICATION && user != null && !user.isEmailVerified) {
                                runCatching { user.sendEmailVerification().await() }
                                auth.signOut()
                                error = "Your email is not verified yet. We sent a new verification link to ${email.trim()}. Verify it, then log in."
                                return@launch
                            }
                            onSignedIn()
                        } else {
                            val result = auth
                                .createUserWithEmailAndPassword(email.trim(), password)
                                .await()
                            result.user?.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(name.trim())
                                    .build()
                            )?.await()
                            if (REQUIRE_EMAIL_VERIFICATION) {
                                runCatching { result.user?.sendEmailVerification()?.await() }
                                auth.signOut()
                                isLogin = true
                                password = ""
                                info = "Account created! We sent a verification link to ${email.trim()}. Verify it, then log in."
                            } else {
                                onSignedIn()
                            }
                        }
                    } catch (e: Exception) {
                        error = e.localizedMessage ?: "Something went wrong. Please try again."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Cyan,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                if (loading) "Please wait..." else if (isLogin) "Login" else "Sign Up",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { isLogin = !isLogin; error = ""; info = "" }) {
            Text(
                if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login",
                color = Color(0xFF9A93D0)
            )
        }
    }
}
