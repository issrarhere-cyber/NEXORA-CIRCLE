package com.nexora.circle.security

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object AccountSecurity {
    private val auth get() = FirebaseAuth.getInstance()

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun sendVerificationEmail(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await()
        Unit
    }

    suspend fun isEmailVerified(): Boolean {
        val user = auth.currentUser ?: return false
        user.reload().await()
        return user.isEmailVerified
    }

    suspend fun changePassword(current: String, new: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        val email = user.email ?: error("No email on this account")
        user.reauthenticate(EmailAuthProvider.getCredential(email, current)).await()
        user.updatePassword(new).await()
        Unit
    }
}
