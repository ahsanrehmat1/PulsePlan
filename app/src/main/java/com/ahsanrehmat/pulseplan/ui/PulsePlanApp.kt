package com.ahsanrehmat.pulseplan.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.domain.ExerciseGuideCatalog
import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.ExperienceLevel
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.model.MovementPreference
import com.ahsanrehmat.pulseplan.model.ReminderTime
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WeekDayPlan
import com.ahsanrehmat.pulseplan.model.WorkoutPersonality
import com.ahsanrehmat.pulseplan.ui.theme.Canvas
import com.ahsanrehmat.pulseplan.ui.theme.Ink
import com.ahsanrehmat.pulseplan.ui.theme.InkSoft
import com.ahsanrehmat.pulseplan.ui.theme.Line
import com.ahsanrehmat.pulseplan.ui.theme.Muted
import com.ahsanrehmat.pulseplan.ui.theme.PulseGreen
import com.ahsanrehmat.pulseplan.ui.theme.Warm

@Composable
fun PulsePlanApp(viewModel: PulsePlanViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state.phase) {
        AppPhase.LOADING -> LoadingScreen()
        AppPhase.SIGNED_OUT -> AuthScreen(
            state = state,
            onSignIn = viewModel::signIn,
            onRegister = viewModel::register,
            onPasswordReset = viewModel::sendPasswordReset,
            onPreview = viewModel::continueInPreview,
            onClearError = viewModel::clearError,
        )
        AppPhase.ONBOARDING -> OnboardingScreen(
            suggestedName = state.account?.email?.substringBefore("@").orEmpty(),
            onComplete = viewModel::saveProfile,
        )
        AppPhase.DASHBOARD -> {
            val profile = state.profile
            val workout = state.workout
            val workoutSession = state.workoutSession
            val selectedExercise = workout?.exercises?.firstOrNull {
                it.id == state.selectedExerciseId
            }
            if (selectedExercise != null) {
                ExerciseGuideScreen(
                    exercise = selectedExercise,
                    guide = ExerciseGuideCatalog.forExercise(selectedExercise),
                    onBack = viewModel::hideExerciseGuide,
                )
            } else if (workout != null && workoutSession != null) {
                ActiveWorkoutScreen(
                    workout = workout,
                    session = workoutSession,
                    completedExerciseIds = state.completedExerciseIds,
                    onCompleteExercise = viewModel::completeActiveExercise,
                    onSkipExercise = viewModel::skipActiveExercise,
                    onRestTick = viewModel::tickRestTimer,
                    onToggleRestTimer = viewModel::toggleRestTimer,
                    onSkipRest = viewModel::skipRest,
                    onShowExerciseGuide = viewModel::showExerciseGuide,
                    onExit = viewModel::exitActiveWorkout,
                )
            } else if (state.isProgressVisible) {
                ProgressHistoryScreen(
                    progress = state.progress,
                    onBack = viewModel::hideProgress,
                )
            } else if (state.isPlanEditorVisible && profile != null) {
                OnboardingScreen(
                    suggestedName = profile.displayName,
                    initialProfile = profile,
                    onComplete = viewModel::saveProfile,
                    onCancel = viewModel::hidePlanEditor,
                )
            } else {
                DashboardScreen(
                    state = state,
                    onToggleExercise = viewModel::toggleExercise,
                    onSwapExercise = viewModel::swapExercise,
                    onStartWorkout = viewModel::startActiveWorkout,
                    onShowProgress = viewModel::showProgress,
                    onEditPlan = viewModel::showPlanEditor,
                    onShowExerciseGuide = viewModel::showExerciseGuide,
                    onReminderTimeChange = viewModel::updateReminderTime,
                    onSignOut = viewModel::signOut,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = PulseGreen)
    }
}

@Composable
private fun AuthScreen(
    state: PulsePlanUiState,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onPasswordReset: (String) -> Unit,
    onPreview: () -> Unit,
    onClearError: () -> Unit,
) {
    var isCreatingAccount by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        localError = when {
            !state.isFirebaseConfigured ->
                "Firebase accounts are not connected yet. Use preview mode for now."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                "Enter a valid email address."
            password.length < 8 ->
                "Use at least 8 characters for your password."
            else -> null
        }
        if (localError == null) {
            if (isCreatingAccount) onRegister(email, password) else onSignIn(email, password)
        }
    }

    fun requestPasswordReset() {
        localError = when {
            !state.isFirebaseConfigured ->
                "Firebase accounts are not connected yet. Use preview mode for now."
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                "Enter your account email first."
            else -> null
        }
        if (localError == null) {
            onPasswordReset(email)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(PulseGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Ink,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "PULSEPLAN",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        item { Spacer(Modifier.height(22.dp)) }

        item {
            Text(
                text = if (isCreatingAccount) "Build your plan." else "Welcome back.",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "A clear workout for today, shaped around your goals and routine.",
                color = Color(0xFFB8C1B3),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        item {
            AccountConnectionPill(isConnected = state.isFirebaseConfigured)
        }

        item {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                    onClearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = darkFieldColors(),
            )
        }

        item {
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                    onClearError()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                colors = darkFieldColors(),
            )
            if (!isCreatingAccount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = ::requestPasswordReset,
                        enabled = !state.isBusy,
                    ) {
                        Text("Forgot password?", color = PulseGreen)
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = localError != null ||
                    state.errorMessage != null ||
                    state.noticeMessage != null,
            ) {
                val isError = localError != null || state.errorMessage != null
                Text(
                    text = localError
                        ?: state.errorMessage
                        ?: state.noticeMessage.orEmpty(),
                    color = if (isError) Color(0xFFFF9E92) else PulseGreen,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Button(
                onClick = ::submit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !state.isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseGreen,
                    contentColor = Ink,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Ink,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(if (isCreatingAccount) "Create account" else "Sign in")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseGreen),
                border = androidx.compose.foundation.BorderStroke(1.dp, PulseGreen),
            ) {
                Text("Preview the app")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isCreatingAccount) "Already have an account?" else "New here?",
                    color = Color(0xFFB8C1B3),
                )
                TextButton(
                    onClick = {
                        isCreatingAccount = !isCreatingAccount
                        localError = null
                        onClearError()
                    },
                ) {
                    Text(
                        text = if (isCreatingAccount) "Sign in" else "Create account",
                        color = PulseGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = PulseGreen,
    unfocusedLabelColor = Color(0xFFB8C1B3),
    focusedLeadingIconColor = PulseGreen,
    unfocusedLeadingIconColor = Color(0xFFB8C1B3),
    focusedBorderColor = PulseGreen,
    unfocusedBorderColor = Color(0xFF596257),
    cursorColor = PulseGreen,
)

@Composable
private fun AccountConnectionPill(isConnected: Boolean) {
    val background = if (isConnected) PulseGreen.copy(alpha = 0.14f) else Warm.copy(alpha = 0.14f)
    val foreground = if (isConnected) PulseGreen else Warm
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(foreground),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isConnected) "Accounts connected" else "Preview mode - Firebase setup pending",
            color = foreground,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CloudSyncCard(
    syncState: CloudSyncState,
    message: String?,
) {
    val details = when (syncState) {
        CloudSyncState.PREVIEW -> Triple(
            Icons.Default.PhoneAndroid,
            "Preview data stays on this device",
            "Create or sign in to an account later to enable cloud backup.",
        )
        CloudSyncState.SETUP_REQUIRED -> Triple(
            Icons.Default.CloudOff,
            "Cloud backup setup pending",
            "Preview remains available while Firebase is being connected.",
        )
        CloudSyncState.SYNCING -> Triple(
            Icons.Default.CloudSync,
            "Saving your latest changes",
            "Keep the app open for a moment.",
        )
        CloudSyncState.UP_TO_DATE -> Triple(
            Icons.Default.CloudDone,
            "Cloud backup is up to date",
            "Your plan and progress are available when you sign in on another device.",
        )
        CloudSyncState.LOCAL_ONLY -> Triple(
            Icons.Default.CloudOff,
            "Saved safely on this device",
            message ?: "Cloud backup will retry the next time you sign in.",
        )
    }
    val accent = when (syncState) {
        CloudSyncState.UP_TO_DATE -> PulseGreen
        CloudSyncState.SYNCING -> Warm
        CloudSyncState.PREVIEW,
        CloudSyncState.SETUP_REQUIRED,
        CloudSyncState.LOCAL_ONLY,
        -> Muted
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = details.first,
                    contentDescription = null,
                    tint = accent,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.second,
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = details.third,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun OnboardingScreen(
    suggestedName: String,
    initialProfile: UserFitnessProfile? = null,
    onComplete: (UserFitnessProfile) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    val isEditing = initialProfile != null
    BackHandler(enabled = onCancel != null) {
        onCancel?.invoke()
    }
    var name by remember(initialProfile, suggestedName) {
        mutableStateOf(
            initialProfile?.displayName
                ?: suggestedName.replaceFirstChar(Char::uppercase),
        )
    }
    var goal by remember(initialProfile) {
        mutableStateOf(initialProfile?.goal ?: FitnessGoal.GENERAL_FITNESS)
    }
    var experience by remember(initialProfile) {
        mutableStateOf(initialProfile?.experience ?: ExperienceLevel.BEGINNER)
    }
    var personality by remember(initialProfile) {
        mutableStateOf(initialProfile?.personality ?: WorkoutPersonality.GUIDED)
    }
    var equipment by remember(initialProfile) {
        mutableStateOf(initialProfile?.equipment ?: Equipment.BODYWEIGHT)
    }
    var daysPerWeek by remember(initialProfile) {
        mutableIntStateOf(initialProfile?.daysPerWeek ?: 3)
    }
    var sessionMinutes by remember(initialProfile) {
        mutableIntStateOf(initialProfile?.sessionMinutes ?: 30)
    }
    var movementNotes by remember(initialProfile) {
        mutableStateOf(initialProfile?.movementNotes.orEmpty())
    }
    var movementPreferences by remember(initialProfile) {
        mutableStateOf(initialProfile?.movementPreferences.orEmpty())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onCancel != null) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to dashboard",
                            tint = Ink,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) "Update your plan." else "Make it yours.",
                        style = MaterialTheme.typography.displaySmall,
                        color = Ink,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isEditing) {
                            "Adjust what fits your life now, then rebuild today's workout."
                        } else {
                            "A few honest answers will shape your weekly rhythm and today's exercises."
                        },
                        color = Muted,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        item {
            QuestionBlock(
                number = "01",
                title = "What should we call you?",
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Your first name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }

        item {
            QuestionBlock(
                number = "02",
                title = "What's your main goal?",
            ) {
                ChoiceRow(
                    values = FitnessGoal.entries,
                    selected = goal,
                    label = FitnessGoal::label,
                    onSelect = { goal = it },
                )
            }
        }

        item {
            QuestionBlock(
                number = "03",
                title = "Where are you starting?",
            ) {
                ChoiceRow(
                    values = ExperienceLevel.entries,
                    selected = experience,
                    label = ExperienceLevel::label,
                    onSelect = { experience = it },
                )
            }
        }

        item {
            QuestionBlock(
                number = "04",
                title = "What keeps you motivated?",
                helper = personality.description,
            ) {
                ChoiceRow(
                    values = WorkoutPersonality.entries,
                    selected = personality,
                    label = WorkoutPersonality::label,
                    onSelect = { personality = it },
                )
            }
        }

        item {
            QuestionBlock(
                number = "05",
                title = "What equipment is available?",
            ) {
                ChoiceRow(
                    values = Equipment.entries,
                    selected = equipment,
                    label = Equipment::label,
                    onSelect = { equipment = it },
                )
            }
        }

        item {
            QuestionBlock(
                number = "06",
                title = "Your realistic weekly rhythm",
            ) {
                Text("Training days", color = Muted)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(
                    values = listOf(2, 3, 4, 5),
                    selected = daysPerWeek,
                    label = { "$it days" },
                    onSelect = { daysPerWeek = it },
                )
                Spacer(Modifier.height(16.dp))
                Text("Time per session", color = Muted)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(
                    values = listOf(15, 30, 45, 60),
                    selected = sessionMinutes,
                    label = { "$it min" },
                    onSelect = { sessionMinutes = it },
                )
            }
        }

        item {
            QuestionBlock(
                number = "07",
                title = "How should movements be adjusted?",
                helper = "Choose any that help the plan fit you. These are movement preferences, not medical advice.",
            ) {
                MovementPreferenceSelector(
                    selected = movementPreferences,
                    onToggle = { preference ->
                        movementPreferences = if (preference in movementPreferences) {
                            movementPreferences - preference
                        } else {
                            movementPreferences + preference
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Anything else?",
                    color = Ink,
                    style = MaterialTheme.typography.titleSmall,
                )
                OutlinedTextField(
                    value = movementNotes,
                    onValueChange = { movementNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Optional note for your own reference") },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                )
                Text(
                    text = "For pain, injury, pregnancy, or a health condition, ask a qualified professional what is appropriate for you.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item {
            if (isEditing) {
                Surface(
                    color = Warm.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = "Your saved history is not deleted. Schedule or exercise changes can recalculate streaks and totals; only today's completed exercises that still match remain checked.",
                        modifier = Modifier.padding(16.dp),
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = {
                    onComplete(
                        UserFitnessProfile(
                            displayName = name.trim().ifBlank { "Athlete" },
                            goal = goal,
                            experience = experience,
                            personality = personality,
                            equipment = equipment,
                            daysPerWeek = daysPerWeek,
                            sessionMinutes = sessionMinutes,
                            movementNotes = movementNotes.trim(),
                            movementPreferences = movementPreferences,
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Ink,
                    contentColor = PulseGreen,
                ),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text(if (isEditing) "Save & rebuild plan" else "Build my first plan")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun QuestionBlock(
    number: String,
    title: String,
    helper: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = number,
                color = Muted,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
                if (helper != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = helper,
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        content()
    }
}

@Composable
private fun <T> ChoiceRow(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        values.forEach { value ->
            val isSelected = value == selected
            if (isSelected) {
                Button(
                    onClick = { onSelect(value) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseGreen,
                        contentColor = Ink,
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label(value))
                }
            } else {
                OutlinedButton(
                    onClick = { onSelect(value) },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                ) {
                    Text(label(value), color = Ink)
                }
            }
        }
    }
}

@Composable
private fun MovementPreferenceSelector(
    selected: Set<MovementPreference>,
    onToggle: (MovementPreference) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MovementPreference.entries.forEach { preference ->
            val isSelected = preference in selected
            Card(
                onClick = { onToggle(preference) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        PulseGreen.copy(alpha = 0.3f)
                    } else {
                        Color.White
                    },
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) PulseGreen else Line,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggle(preference) },
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = preference.label,
                            color = Ink,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = preference.description,
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    state: PulsePlanUiState,
    onToggleExercise: (String) -> Unit,
    onSwapExercise: (String) -> Unit,
    onStartWorkout: () -> Unit,
    onShowProgress: () -> Unit,
    onEditPlan: () -> Unit,
    onShowExerciseGuide: (String) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onSignOut: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsEnabled = granted
    }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val profile = state.profile ?: return
    val workout = state.workout ?: return

    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PulseGreen),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("PULSEPLAN", fontWeight = FontWeight.Black)
                    }
                },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Canvas,
                    titleContentColor = Ink,
                    actionIconContentColor = Ink,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    text = "Good to see you,",
                    color = Muted,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = profile.displayName,
                    color = Ink,
                    style = MaterialTheme.typography.displaySmall,
                )
            }

            item {
                CloudSyncCard(
                    syncState = state.cloudSyncState,
                    message = state.cloudSyncMessage,
                )
            }

            item {
                ProgressSummary(
                    completed = state.completedExerciseIds.size,
                    total = workout.exercises.size,
                    daysPerWeek = profile.daysPerWeek,
                )
            }

            item {
                ProgressHistoryCard(
                    progress = state.progress,
                    onClick = onShowProgress,
                )
            }

            item {
                PlanPreferencesCard(
                    profile = profile,
                    onClick = onEditPlan,
                )
            }

            item {
                ReminderSettingsCard(
                    reminderTime = state.reminderTime,
                    notificationsEnabled = notificationsEnabled,
                    onEdit = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> onReminderTimeChange(hour, minute) },
                            state.reminderTime.hour,
                            state.reminderTime.minute,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    onOpenNotificationSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                )
            }

            item {
                TodayWorkoutHeader(workout = workout)
            }

            item {
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            state.completedExerciseIds.size >= workout.exercises.size ->
                                "View workout summary"
                            state.completedExerciseIds.isNotEmpty() ->
                                "Continue workout"
                            else -> "Start workout"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            items(workout.exercises.size) { index ->
                val exercise = workout.exercises[index]
                val originalExerciseId = exercise.sourceExerciseId
                ExerciseCard(
                    index = index + 1,
                    name = exercise.name,
                    prescription = exercise.prescription,
                    cue = exercise.coachingCue,
                    restSeconds = exercise.restSeconds,
                    isCompleted = originalExerciseId in state.completedExerciseIds,
                    isSubstituted = originalExerciseId != exercise.id &&
                        exercise.adjustmentReason == null,
                    adjustmentReason = exercise.adjustmentReason,
                    onToggle = { onToggleExercise(exercise.id) },
                    onSwap = { onSwapExercise(exercise.id) },
                    onShowGuide = { onShowExerciseGuide(exercise.id) },
                )
            }

            item {
                AnimatedVisibility(
                    visible = state.completedExerciseIds.size == workout.exercises.size,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PulseGreen),
                        shape = RoundedCornerShape(22.dp),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                text = "Workout complete!",
                                color = Ink,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Strong work. Your progress is saved for today.",
                                color = InkSoft,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "This week",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Ink,
                )
                Spacer(Modifier.height(12.dp))
                WeeklyStrip(days = state.week)
            }

            if (
                profile.movementPreferences.isNotEmpty() ||
                profile.movementNotes.isNotBlank()
            ) {
                item {
                    Surface(
                        color = Warm.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (profile.movementPreferences.isNotEmpty()) {
                                Text(
                                    text = "Movement preferences active",
                                    color = Ink,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = profile.movementPreferences
                                        .sortedBy(MovementPreference::ordinal)
                                        .joinToString { it.label },
                                    color = InkSoft,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Conflicting exercises are adjusted automatically.",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (profile.movementNotes.isNotBlank()) {
                                if (profile.movementPreferences.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text(
                                    text = "Your note: ${profile.movementNotes}",
                                    color = InkSoft,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Exercise at a level appropriate for you. Stop if you feel pain, dizziness, or unusual discomfort.",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanPreferencesCard(
    profile: UserFitnessProfile,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Warm.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Warm.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = Ink,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Plan preferences",
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${profile.goal.label} - ${profile.equipment.label} - " +
                            "${profile.daysPerWeek} days - ${profile.sessionMinutes} min",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (profile.movementPreferences.isNotEmpty()) {
                        Text(
                            text = "${profile.movementPreferences.size} movement " +
                                if (profile.movementPreferences.size == 1) {
                                    "preference"
                                } else {
                                    "preferences"
                                },
                            color = InkSoft,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Edit plan preferences",
                tint = Ink,
            )
        }
    }
}

@Composable
private fun ProgressHistoryCard(
    progress: com.ahsanrehmat.pulseplan.domain.ProgressSnapshot,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(PulseGreen.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Ink,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Progress & history",
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${progress.currentStreak} workout streak · " +
                            "${progress.completedThisWeek} of ${progress.plannedThisWeek} this week",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open progress and history",
                tint = Ink,
            )
        }
    }
}

@Composable
private fun ReminderSettingsCard(
    reminderTime: ReminderTime,
    notificationsEnabled: Boolean,
    onEdit: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(PulseGreen.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Ink,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily reminder",
                            color = Ink,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = reminderTime.label(),
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Change")
                }
            }

            if (!notificationsEnabled) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Warm.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "Notifications are off, so this reminder cannot appear.",
                            color = Ink,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TextButton(
                            onClick = onOpenNotificationSettings,
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("Open notification settings")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressSummary(
    completed: Int,
    total: Int,
    daysPerWeek: Int,
) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    Card(
        colors = CardDefaults.cardColors(containerColor = Ink),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("TODAY'S PROGRESS", color = Color(0xFFADB7A8))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$completed of $total exercises",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PulseGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text("$daysPerWeek days/week", color = PulseGreen)
                }
            }
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape),
                color = PulseGreen,
                trackColor = Color(0xFF30382F),
            )
        }
    }
}

@Composable
private fun TodayWorkoutHeader(workout: DailyWorkout) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "TODAY'S WORKOUT",
                    color = Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = workout.title,
                    color = Ink,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Line, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = Muted,
                )
                Spacer(Modifier.width(5.dp))
                Text("${workout.estimatedMinutes} min", color = Ink)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(workout.focus, color = Muted)
    }
}

@Composable
private fun ExerciseCard(
    index: Int,
    name: String,
    prescription: String,
    cue: String,
    restSeconds: Int,
    isCompleted: Boolean,
    isSubstituted: Boolean,
    adjustmentReason: String?,
    onToggle: () -> Unit,
    onSwap: () -> Unit,
    onShowGuide: () -> Unit,
) {
    val isAutomaticallyAdjusted = adjustmentReason != null
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) PulseGreen.copy(alpha = 0.35f) else Color.White,
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCompleted) PulseGreen else Line,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isCompleted) Ink else Canvas),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = index.toString().padStart(2, '0'),
                    color = if (isCompleted) PulseGreen else Muted,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (isSubstituted) {
                    Text(
                        text = "TODAY'S ALTERNATIVE",
                        color = InkSoft,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (isAutomaticallyAdjusted) {
                    Text(
                        text = "PLAN ADJUSTMENT",
                        color = InkSoft,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = adjustmentReason.orEmpty(),
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = "$prescription - $restSeconds sec rest",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = cue,
                    color = InkSoft,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = onShowGuide,
                        contentPadding = PaddingValues(
                            start = 0.dp,
                            top = 7.dp,
                            end = 8.dp,
                            bottom = 0.dp,
                        ),
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("How to do it")
                    }
                    TextButton(
                        onClick = onSwap,
                        enabled = !isCompleted && !isAutomaticallyAdjusted,
                        contentPadding = PaddingValues(
                            start = 5.dp,
                            top = 7.dp,
                            end = 5.dp,
                            bottom = 0.dp,
                        ),
                    ) {
                        Icon(
                            imageVector = when {
                                isSubstituted -> Icons.Default.Restore
                                isAutomaticallyAdjusted -> Icons.Default.Tune
                                else -> Icons.Default.SwapHoriz
                            },
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            when {
                                isSubstituted -> "Restore"
                                isAutomaticallyAdjusted -> "Adjusted"
                                else -> "Swap"
                            },
                        )
                    }
                }
            }
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun WeeklyStrip(days: List<WeekDayPlan>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        days.forEach { day ->
            val background = when {
                day.isToday -> Ink
                day.isWorkoutDay -> Color.White
                else -> Color(0xFFECEF_E8)
            }
            val foreground = if (day.isToday) Color.White else Ink
            Card(
                colors = CardDefaults.cardColors(containerColor = background),
                shape = RoundedCornerShape(18.dp),
                border = if (day.isToday) null else {
                    androidx.compose.foundation.BorderStroke(1.dp, Line)
                },
            ) {
                Column(
                    modifier = Modifier
                        .width(92.dp)
                        .padding(13.dp),
                ) {
                    Text(
                        text = day.shortDay.uppercase(),
                        color = if (day.isToday) PulseGreen else Muted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(18.dp))
                    Icon(
                        imageVector = if (day.isWorkoutDay) {
                            Icons.Default.FitnessCenter
                        } else {
                            Icons.Default.Notifications
                        },
                        contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = day.focus,
                        color = foreground,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
