package com.ahsanrehmat.pulseplan.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AccountUser(
    val id: String,
    val email: String,
)

interface AuthRepository {
    val isConfigured: Boolean
    val currentUser: AccountUser?

    suspend fun signIn(email: String, password: String): AccountUser
    suspend fun register(email: String, password: String): AccountUser
    suspend fun sendPasswordReset(email: String)
    fun signOut()
}

class FirebaseAuthRepository(context: Context) : AuthRepository {
    private val auth: FirebaseAuth? = FirebaseApp.getApps(context)
        .firstOrNull()
        ?.let { FirebaseAuth.getInstance(it) }

    override val isConfigured: Boolean = auth != null

    override val currentUser: AccountUser?
        get() = auth?.currentUser?.toAccountUser()

    override suspend fun signIn(email: String, password: String): AccountUser {
        val configuredAuth = requireNotNull(auth) {
            "Firebase is not configured yet. Add its values to local.properties."
        }
        val result = suspendCancellableCoroutine { continuation ->
            configuredAuth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    when {
                        !continuation.isActive -> Unit
                        task.isSuccessful -> continuation.resume(
                            requireNotNull(task.result.user).toAccountUser(),
                        )
                        else -> continuation.resumeWithException(
                            task.exception ?: IllegalStateException("Sign-in failed."),
                        )
                    }
                }
        }
        return result
    }

    override suspend fun register(email: String, password: String): AccountUser {
        val configuredAuth = requireNotNull(auth) {
            "Firebase is not configured yet. Add its values to local.properties."
        }
        val result = suspendCancellableCoroutine { continuation ->
            configuredAuth.createUserWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener { task ->
                    when {
                        !continuation.isActive -> Unit
                        task.isSuccessful -> continuation.resume(
                            requireNotNull(task.result.user).toAccountUser(),
                        )
                        else -> continuation.resumeWithException(
                            task.exception ?: IllegalStateException("Account creation failed."),
                        )
                    }
                }
        }
        return result
    }

    override suspend fun sendPasswordReset(email: String) {
        val configuredAuth = requireNotNull(auth) {
            "Firebase is not configured yet. Add its values to local.properties."
        }
        suspendCancellableCoroutine { continuation ->
            configuredAuth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener { task ->
                    when {
                        !continuation.isActive -> Unit
                        task.isSuccessful -> continuation.resume(Unit)
                        else -> continuation.resumeWithException(
                            task.exception ?: IllegalStateException(
                                "Password reset could not be sent.",
                            ),
                        )
                    }
                }
        }
    }

    override fun signOut() {
        auth?.signOut()
    }

    private fun FirebaseUser.toAccountUser() = AccountUser(
        id = uid,
        email = email.orEmpty(),
    )
}
