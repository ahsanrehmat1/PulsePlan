package com.ahsanrehmat.pulseplan.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class AccountProvider {
    EMAIL,
    GOOGLE,
}

data class AccountUser(
    val id: String,
    val email: String,
    val isEmailVerified: Boolean = false,
    val provider: AccountProvider = AccountProvider.EMAIL,
)

sealed interface AccountReauthentication {
    data class Password(val password: String) : AccountReauthentication
    data class Google(val idToken: String) : AccountReauthentication
}

interface AuthRepository {
    val isConfigured: Boolean
    val currentUser: AccountUser?

    suspend fun signIn(email: String, password: String): AccountUser
    suspend fun register(email: String, password: String): AccountUser
    suspend fun signInWithGoogle(idToken: String): AccountUser
    suspend fun linkWithGoogle(idToken: String): AccountUser
    suspend fun sendPasswordReset(email: String)
    suspend fun refreshCurrentUser(): AccountUser
    suspend fun sendEmailVerification()
    suspend fun reauthenticate(reauthentication: AccountReauthentication)
    suspend fun deleteCurrentUser()
    suspend fun clearCredentialState()
    fun signOut()
}

class FirebaseAuthRepository(context: Context) : AuthRepository {
    private val auth: FirebaseAuth? = FirebaseApp.getApps(context)
        .firstOrNull()
        ?.let { FirebaseAuth.getInstance(it) }
    private val credentialManager = CredentialManager.create(context)

    override val isConfigured: Boolean = auth != null

    override val currentUser: AccountUser?
        get() = auth?.currentUser?.toAccountUser()

    override suspend fun signIn(email: String, password: String): AccountUser {
        val configuredAuth = requireNotNull(auth) {
            "Sign-in is temporarily unavailable."
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
            "Sign-in is temporarily unavailable."
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

    override suspend fun signInWithGoogle(idToken: String): AccountUser {
        val configuredAuth = requireNotNull(auth) {
            "Sign-in is temporarily unavailable."
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return suspendCancellableCoroutine { continuation ->
            configuredAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    when {
                        !continuation.isActive -> Unit
                        task.isSuccessful -> continuation.resume(
                            requireNotNull(task.result.user).toAccountUser(),
                        )
                        else -> continuation.resumeWithException(
                            task.exception ?: IllegalStateException(
                                "Google sign-in failed.",
                            ),
                        )
                    }
                }
        }
    }

    override suspend fun linkWithGoogle(idToken: String): AccountUser {
        val user = requireNotNull(auth?.currentUser) {
            "Sign in again before adding Google."
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return suspendCancellableCoroutine { continuation ->
            user.linkWithCredential(credential)
                .addOnCompleteListener { task ->
                    when {
                        !continuation.isActive -> Unit
                        task.isSuccessful -> continuation.resume(
                            requireNotNull(task.result.user).toAccountUser(),
                        )
                        else -> continuation.resumeWithException(
                            task.exception ?: IllegalStateException(
                                "Google sign-in could not be added.",
                            ),
                        )
                    }
                }
        }
    }

    override suspend fun sendPasswordReset(email: String) {
        val configuredAuth = requireNotNull(auth) {
            "Sign-in is temporarily unavailable."
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

    override suspend fun refreshCurrentUser(): AccountUser {
        val configuredAuth = requireNotNull(auth) {
            "Sign-in is temporarily unavailable."
        }
        val user = requireNotNull(configuredAuth.currentUser) {
            "Sign in again to refresh your account."
        }
        user.reload().awaitResult("Account status could not be refreshed.")
        return requireNotNull(configuredAuth.currentUser).toAccountUser()
    }

    override suspend fun sendEmailVerification() {
        val user = requireNotNull(auth?.currentUser) {
            "Sign in again to verify your email."
        }
        user.sendEmailVerification()
            .awaitResult("Verification email could not be sent.")
    }

    override suspend fun reauthenticate(reauthentication: AccountReauthentication) {
        val user = requireNotNull(auth?.currentUser) {
            "Sign in again before deleting your account."
        }
        val credential = when (reauthentication) {
            is AccountReauthentication.Password -> {
                val email = requireNotNull(user.email) {
                    "This account does not have an email address."
                }
                EmailAuthProvider.getCredential(email, reauthentication.password)
            }
            is AccountReauthentication.Google ->
                GoogleAuthProvider.getCredential(reauthentication.idToken, null)
        }
        user.reauthenticate(credential)
            .awaitResult("Your sign-in could not be confirmed.")
    }

    override suspend fun deleteCurrentUser() {
        val user = requireNotNull(auth?.currentUser) {
            "Sign in again before deleting your account."
        }
        user.delete().awaitResult("Your account could not be deleted.")
    }

    override suspend fun clearCredentialState() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    override fun signOut() {
        auth?.signOut()
    }

    private fun FirebaseUser.toAccountUser() = AccountUser(
        id = uid,
        email = email.orEmpty(),
        isEmailVerified = isEmailVerified,
        provider = if (
            providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
        ) {
            AccountProvider.GOOGLE
        } else {
            AccountProvider.EMAIL
        },
    )

    private suspend fun <T> Task<T>.awaitResult(fallbackMessage: String): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                when {
                    !continuation.isActive -> Unit
                    task.isSuccessful -> continuation.resume(task.result)
                    else -> continuation.resumeWithException(
                        task.exception ?: IllegalStateException(fallbackMessage),
                    )
                }
            }
        }
}
