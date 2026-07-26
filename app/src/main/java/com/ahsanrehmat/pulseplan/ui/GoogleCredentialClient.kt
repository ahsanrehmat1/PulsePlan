package com.ahsanrehmat.pulseplan.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.SecureRandom

internal sealed interface GoogleCredentialOutcome {
    data class Success(val idToken: String) : GoogleCredentialOutcome
    data object Cancelled : GoogleCredentialOutcome
    data class Error(val message: String) : GoogleCredentialOutcome
}

internal suspend fun requestGoogleIdToken(
    context: Context,
    serverClientId: String,
): GoogleCredentialOutcome {
    if (serverClientId.isBlank()) {
        return GoogleCredentialOutcome.Error("Google sign-in is unavailable.")
    }
    val activity = context.findActivity()
        ?: return GoogleCredentialOutcome.Error("Google sign-in could not open.")
    val option = GetSignInWithGoogleOption.Builder(serverClientId)
        .setNonce(secureNonce())
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()

    return try {
        val credential = CredentialManager.create(activity)
            .getCredential(context = activity, request = request)
            .credential
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleCredentialOutcome.Success(
                GoogleIdTokenCredential.createFrom(credential.data).idToken,
            )
        } else {
            GoogleCredentialOutcome.Error("Google sign-in could not be completed.")
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleCredentialOutcome.Cancelled
    } catch (_: NoCredentialException) {
        GoogleCredentialOutcome.Error("No Google account is available.")
    } catch (_: GoogleIdTokenParsingException) {
        GoogleCredentialOutcome.Error("Google sign-in returned an invalid response.")
    } catch (error: GetCredentialException) {
        GoogleCredentialOutcome.Error(
            error.message?.takeIf(String::isNotBlank)
                ?: "Google sign-in could not be completed.",
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun secureNonce(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(
        bytes,
        Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING,
    )
}
