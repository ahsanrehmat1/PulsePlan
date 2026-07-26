package com.ahsanrehmat.pulseplan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ahsanrehmat.pulseplan.data.AccountProvider
import com.ahsanrehmat.pulseplan.data.AccountReauthentication
import com.ahsanrehmat.pulseplan.data.AccountUser
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreenDark
import com.ahsanrehmat.pulseplan.ui.theme.Warm
import kotlinx.coroutines.launch

private val Danger = Color(0xFFB3261E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountScreen(
    account: AccountUser,
    isBusy: Boolean,
    errorMessage: String?,
    noticeMessage: String?,
    onBack: () -> Unit,
    onClearMessage: () -> Unit,
    onRefreshVerification: () -> Unit,
    onSendVerification: () -> Unit,
    onPasswordReset: () -> Unit,
    onLinkGoogle: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: (AccountReauthentication) -> Unit,
    googleWebClientId: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var isGooglePromptOpen by remember { mutableStateOf(false) }
    var localDeleteError by remember { mutableStateOf<String?>(null) }
    var localGoogleError by remember { mutableStateOf<String?>(null) }

    if (showDeleteConfirmation) {
        DeleteAccountDialog(
            provider = account.provider,
            isBusy = isBusy || isGooglePromptOpen,
            errorMessage = localDeleteError ?: errorMessage,
            onDismiss = {
                if (!isBusy && !isGooglePromptOpen) {
                    showDeleteConfirmation = false
                    localDeleteError = null
                    onClearMessage()
                }
            },
            onConfirm = { password ->
                localDeleteError = null
                if (account.provider == AccountProvider.GOOGLE) {
                    isGooglePromptOpen = true
                    scope.launch {
                        when (
                            val result = requestGoogleIdToken(
                                context = context,
                                serverClientId = googleWebClientId,
                            )
                        ) {
                            is GoogleCredentialOutcome.Success ->
                                onDeleteAccount(
                                    AccountReauthentication.Google(result.idToken),
                                )
                            GoogleCredentialOutcome.Cancelled -> Unit
                            is GoogleCredentialOutcome.Error ->
                                localDeleteError = result.message
                        }
                        isGooglePromptOpen = false
                    }
                } else {
                    onDeleteAccount(
                        AccountReauthentication.Password(password.orEmpty()),
                    )
                }
            },
        )
    }

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Account & privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Canvas,
                    titleContentColor = Ink,
                    navigationIconContentColor = Ink,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AccountIdentityCard(
                    account = account,
                    isBusy = isBusy,
                    onRefreshVerification = onRefreshVerification,
                    onSendVerification = onSendVerification,
                )
            }

            if (localGoogleError != null || errorMessage != null || noticeMessage != null) {
                item {
                    AccountMessage(
                        message = localGoogleError ?: errorMessage ?: noticeMessage.orEmpty(),
                        isError = localGoogleError != null || errorMessage != null,
                    )
                }
            }

            if (account.provider == AccountProvider.EMAIL) {
                item {
                    AccountSecurityCard(
                        isBusy = isBusy || isGooglePromptOpen,
                        onPasswordReset = onPasswordReset,
                        onLinkGoogle = {
                            localGoogleError = null
                            isGooglePromptOpen = true
                            scope.launch {
                                when (
                                    val result = requestGoogleIdToken(
                                        context = context,
                                        serverClientId = googleWebClientId,
                                    )
                                ) {
                                    is GoogleCredentialOutcome.Success ->
                                        onLinkGoogle(result.idToken)
                                    GoogleCredentialOutcome.Cancelled -> Unit
                                    is GoogleCredentialOutcome.Error ->
                                        localGoogleError = result.message
                                }
                                isGooglePromptOpen = false
                            }
                        },
                    )
                }
            }

            item {
                PrivacyInformationCard()
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Sign out")
                }
            }

            item {
                DeleteAccountCard(
                    enabled = !isBusy,
                    onDelete = {
                        onClearMessage()
                        showDeleteConfirmation = true
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountIdentityCard(
    account: AccountUser,
    isBusy: Boolean,
    onRefreshVerification: () -> Unit,
    onSendVerification: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(PulseGreen.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Ink)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (account.provider == AccountProvider.GOOGLE) {
                            "Google account"
                        } else {
                            "Email account"
                        },
                        color = Muted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = account.email,
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            if (account.provider == AccountProvider.EMAIL) {
                VerificationStatus(isVerified = account.isEmailVerified)
                if (!account.isEmailVerified) {
                    Button(
                        onClick = onSendVerification,
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PulseGreen,
                            contentColor = Ink,
                        ),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Ink,
                            )
                        } else {
                            Icon(Icons.Default.Email, contentDescription = null)
                        }
                        Spacer(Modifier.size(8.dp))
                        Text("Send verification email")
                    }
                }
                OutlinedButton(
                    onClick = onRefreshVerification,
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Refresh status")
                }
            }
        }
    }
}

@Composable
private fun VerificationStatus(isVerified: Boolean) {
    val background = if (isVerified) PulseGreen.copy(alpha = 0.24f) else Warm.copy(alpha = 0.3f)
    val text = if (isVerified) "Email verified" else "Verification pending"
    Surface(
        color = background,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (isVerified) PulseGreenDark else Warm),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                imageVector = if (isVerified) Icons.Default.Check else Icons.Default.Info,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(18.dp),
            )
            Text(text = text, color = Ink, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun AccountSecurityCard(
    isBusy: Boolean,
    onPasswordReset: () -> Unit,
    onLinkGoogle: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Ink)
                Text(
                    text = "Account security",
                    color = Ink,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                text = "Use your password or Google.",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onLinkGoogle,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseGreen,
                    contentColor = Ink,
                ),
            ) {
                Text("Add Google sign-in")
            }
            OutlinedButton(
                onClick = onPasswordReset,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Send password reset email")
            }
        }
    }
}

@Composable
private fun PrivacyInformationCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Privacy & data use",
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
            )
            PrivacyParagraph(
                title = "Saved data",
                body = "Your plan, progress, results, and settings sync across your devices.",
            )
            PrivacyParagraph(
                title = "Privacy",
                body = "PulsePlan has no ads and does not sell your data.",
            )
            PrivacyParagraph(
                title = "Your choice",
                body = "You can sign out or delete your account at any time.",
            )
        }
    }
}

@Composable
private fun PrivacyParagraph(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            color = Ink,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(text = body, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccountMessage(message: String, isError: Boolean) {
    Surface(
        color = if (isError) Danger.copy(alpha = 0.12f) else PulseGreen.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isError) Danger else PulseGreenDark),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = if (isError) Danger else Ink,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DeleteAccountCard(
    enabled: Boolean,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Danger.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Danger.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Danger zone",
                color = Danger,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Deletes your account and all saved data. This cannot be undone.",
                color = Ink,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger),
                border = BorderStroke(1.dp, Danger),
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Delete account and data")
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    provider: AccountProvider,
    isBusy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit,
) {
    var confirmation by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canDelete = confirmation == DELETE_CONFIRMATION &&
        (provider == AccountProvider.GOOGLE || password.isNotBlank()) &&
        !isBusy

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your account and workout history will be deleted permanently.")
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Type DELETE") },
                    singleLine = true,
                    enabled = !isBusy,
                    colors = confirmationFieldColors(),
                )
                if (provider == AccountProvider.EMAIL) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !isBusy,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = confirmationFieldColors(),
                    )
                } else {
                    Text(
                        text = "You will confirm with Google.",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Danger,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password.takeIf(String::isNotBlank)) },
                enabled = canDelete,
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Danger,
                    )
                } else {
                    Text("Delete account", color = if (canDelete) Danger else Muted)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel")
            }
        },
        containerColor = Color.White,
        titleContentColor = Ink,
        textContentColor = Ink,
    )
}

@Composable
private fun confirmationFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedLabelColor = Danger,
    unfocusedLabelColor = Muted,
    focusedBorderColor = Danger,
    unfocusedBorderColor = Line,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = Danger,
)

private const val DELETE_CONFIRMATION = "DELETE"
