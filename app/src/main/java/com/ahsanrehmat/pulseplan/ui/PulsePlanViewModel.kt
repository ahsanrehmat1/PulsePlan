package com.ahsanrehmat.pulseplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahsanrehmat.pulseplan.data.AccountReauthentication
import com.ahsanrehmat.pulseplan.data.AccountProvider
import com.ahsanrehmat.pulseplan.data.AccountUser
import com.ahsanrehmat.pulseplan.data.CloudSyncMerger
import com.ahsanrehmat.pulseplan.data.CloudSyncRepository
import com.ahsanrehmat.pulseplan.data.FirebaseAuthRepository
import com.ahsanrehmat.pulseplan.data.FirebaseCloudSyncRepository
import com.ahsanrehmat.pulseplan.data.UserPreferences
import com.ahsanrehmat.pulseplan.domain.PlanGenerator
import com.ahsanrehmat.pulseplan.domain.PerformanceSnapshot
import com.ahsanrehmat.pulseplan.domain.PerformanceTracker
import com.ahsanrehmat.pulseplan.domain.ProgressSnapshot
import com.ahsanrehmat.pulseplan.domain.ProgressTracker
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionController
import com.ahsanrehmat.pulseplan.domain.WorkoutPrescriptionParser
import com.ahsanrehmat.pulseplan.domain.WorkoutRestKind
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionState
import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.ExerciseSetDraft
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult
import com.ahsanrehmat.pulseplan.model.ReminderTime
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WeekDayPlan
import com.ahsanrehmat.pulseplan.notifications.WorkoutReminder
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate

enum class AppPhase {
    LOADING,
    SIGNED_OUT,
    ONBOARDING,
    DASHBOARD,
}

enum class CloudSyncState {
    SETUP_REQUIRED,
    SYNCING,
    UP_TO_DATE,
    LOCAL_ONLY,
}

data class PulsePlanUiState(
    val phase: AppPhase = AppPhase.LOADING,
    val isFirebaseConfigured: Boolean = false,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val noticeMessage: String? = null,
    val account: AccountUser? = null,
    val profile: UserFitnessProfile? = null,
    val workout: DailyWorkout? = null,
    val week: List<WeekDayPlan> = emptyList(),
    val completedExerciseIds: Set<String> = emptySet(),
    val exerciseSubstitutions: Map<String, String> = emptyMap(),
    val reminderTime: ReminderTime = ReminderTime.DEFAULT,
    val workoutSession: WorkoutSessionState? = null,
    val progress: ProgressSnapshot = ProgressSnapshot.EMPTY,
    val exerciseResultsToday: Map<String, ExerciseResult> = emptyMap(),
    val performance: PerformanceSnapshot = PerformanceSnapshot.EMPTY,
    val lastResultMessage: String? = null,
    val isProgressVisible: Boolean = false,
    val isPerformanceVisible: Boolean = false,
    val isPlanEditorVisible: Boolean = false,
    val isAccountVisible: Boolean = false,
    val selectedExerciseId: String? = null,
    val cloudSyncState: CloudSyncState = CloudSyncState.SETUP_REQUIRED,
    val cloudSyncMessage: String? = null,
)

class PulsePlanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val authRepository = FirebaseAuthRepository(application)
    private val cloudRepository: CloudSyncRepository = FirebaseCloudSyncRepository(application)
    private val preferences = UserPreferences(application)
    private val today = LocalDate.now()
    private var loadedCompletionHistory: Map<LocalDate, Set<String>> = emptyMap()
    private var loadedResultHistory: Map<LocalDate, Map<String, ExerciseResult>> = emptyMap()
    private var sessionSaveJob: Job? = null

    private val _uiState = MutableStateFlow(
        PulsePlanUiState(
            isFirebaseConfigured = authRepository.isConfigured,
            cloudSyncState = if (cloudRepository.isConfigured) {
                CloudSyncState.LOCAL_ONLY
            } else {
                CloudSyncState.SETUP_REQUIRED
            },
        ),
    )
    val uiState: StateFlow<PulsePlanUiState> = _uiState.asStateFlow()

    init {
        val existingAccount = authRepository.currentUser
        if (existingAccount == null) {
            _uiState.update { it.copy(phase = AppPhase.SIGNED_OUT) }
        } else {
            openAccount(existingAccount)
        }
    }

    fun signIn(email: String, password: String) {
        authenticate { authRepository.signIn(email, password) }
    }

    fun register(email: String, password: String) {
        authenticate { authRepository.register(email, password) }
    }

    fun signInWithGoogle(idToken: String) {
        authenticate { authRepository.signInWithGoogle(idToken) }
    }

    fun linkWithGoogle(idToken: String) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    errorMessage = null,
                    noticeMessage = null,
                )
            }
            runCatching { authRepository.linkWithGoogle(idToken) }
                .onSuccess { linkedAccount ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            account = linkedAccount,
                            noticeMessage = "Google sign-in added.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = authActionMessage(
                                error,
                                "Google sign-in could not be added.",
                            ),
                        )
                    }
                }
        }
    }

    fun sendPasswordReset(email: String) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    errorMessage = null,
                    noticeMessage = null,
                )
            }
            runCatching { authRepository.sendPasswordReset(email) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            noticeMessage = "If that email has an account, a reset link is on its way.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = authActionMessage(
                                error,
                                "Reset email could not be sent.",
                            ),
                        )
                    }
                }
        }
    }

    fun saveProfile(profile: UserFitnessProfile) {
        val account = _uiState.value.account ?: return
        viewModelScope.launch {
            preferences.saveProfile(account.id, profile)
            showDashboard(account, profile)
            syncProfile(account)
        }
    }

    fun toggleExercise(exerciseId: String) {
        val state = _uiState.value
        val account = state.account ?: return
        val completionId = state.workout
            ?.exercises
            ?.firstOrNull { it.id == exerciseId }
            ?.sourceExerciseId
            ?: completionIdFor(exerciseId, state.exerciseSubstitutions)
        val nextCompleted = state.completedExerciseIds.toMutableSet().apply {
            if (!add(completionId)) remove(completionId)
        }.toSet()

        loadedCompletionHistory = loadedCompletionHistory + (today to nextCompleted)
        _uiState.update { stateWithCompletion(it, nextCompleted) }
        saveCompletedExercises(account, nextCompleted)
    }

    fun swapExercise(exerciseId: String) {
        val state = _uiState.value
        val account = state.account ?: return
        val profile = state.profile ?: return
        if (state.workoutSession != null) return

        val selectedExercise = state.workout
            ?.exercises
            ?.firstOrNull { it.id == exerciseId }
            ?: return
        if (selectedExercise.adjustmentReason != null) return
        val originalId = selectedExercise.sourceExerciseId
        if (originalId in state.completedExerciseIds) return

        val nextSubstitutions = if (originalId in state.exerciseSubstitutions) {
            state.exerciseSubstitutions - originalId
        } else {
            val replacement = PlanGenerator.substitutionFor(profile, originalId) ?: return
            state.exerciseSubstitutions + (originalId to replacement.id)
        }
        val baseWorkout = PlanGenerator.workoutFor(profile, today)
        val validSubstitutions = PlanGenerator.sanitizeSubstitutions(
            profile = profile,
            workout = baseWorkout,
            selectedSubstitutions = nextSubstitutions,
        )
        val updatedWorkout = PlanGenerator.applySubstitutions(
            profile = profile,
            workout = baseWorkout,
            selectedSubstitutions = validSubstitutions,
        )

        _uiState.update {
            it.copy(
                workout = updatedWorkout,
                exerciseSubstitutions = validSubstitutions,
                selectedExerciseId = null,
            )
        }
        viewModelScope.launch {
            preferences.saveExerciseSubstitutions(
                accountId = account.id,
                date = today,
                substitutions = validSubstitutions,
            )
            syncDay(account, today)
        }
    }

    fun startActiveWorkout() {
        val state = _uiState.value
        val account = state.account ?: return
        val workout = state.workout ?: return
        val displayedCompletedIds = state.completedExerciseIds.mapNotNullTo(mutableSetOf()) {
            completedId -> workout.exercises
                .firstOrNull { exercise -> exercise.sourceExerciseId == completedId }
                ?.id
        }
        _uiState.update {
            it.copy(
                workoutSession = WorkoutSessionController.start(
                    workout = workout,
                    completedExerciseIds = displayedCompletedIds,
                ),
                lastResultMessage = null,
            )
        }
        persistSession(account, _uiState.value.workoutSession)
    }

    fun updateActiveSetDraft(draft: ExerciseSetDraft) {
        val state = _uiState.value
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.updateCurrentSetDraft(session, draft)
        _uiState.update { it.copy(workoutSession = nextSession) }
        persistSession(state.account, nextSession)
    }

    fun updateActiveExerciseFeedback(
        effort: ExerciseEffort,
        notes: String,
    ) {
        val state = _uiState.value
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.updateExerciseFeedback(
            state = session,
            effort = effort,
            notes = notes,
        )
        _uiState.update { it.copy(workoutSession = nextSession) }
        persistSession(state.account, nextSession)
    }

    fun completeActiveSet() {
        val state = _uiState.value
        val account = state.account ?: return
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        val exercise = workout.exercises.getOrNull(session.currentExerciseIndex) ?: return
        val prescription = WorkoutPrescriptionParser.parse(exercise)
        val draft = session.currentSetDraft
            ?: WorkoutPrescriptionParser.plannedDraft(exercise)
        val nextSession = WorkoutSessionController.completeCurrentSet(
            state = session,
            workout = workout,
            draft = draft,
        )
        val exerciseFinished =
            nextSession.completedSets.size >= prescription.setCount &&
                (
                    nextSession.phase == com.ahsanrehmat.pulseplan.domain.WorkoutSessionPhase.COMPLETE ||
                        nextSession.restKind == WorkoutRestKind.BETWEEN_EXERCISES
                    )

        if (!exerciseFinished) {
            _uiState.update {
                it.copy(
                    workoutSession = nextSession,
                    lastResultMessage =
                        "Set ${session.currentSetIndex + 1} of ${prescription.setCount} saved",
                )
            }
            persistSession(account, nextSession)
            return
        }

        val completionId = exercise.sourceExerciseId
        val nextCompleted = state.completedExerciseIds + completionId
        val result = exerciseResultFromSets(
            exerciseId = completionId,
            exerciseName = exercise.name,
            prescription = exercise.prescription,
            sets = nextSession.completedSets,
            effort = session.exerciseEffort,
            notes = session.exerciseNotes,
        )
        val previousResults = loadedResultHistory
            .filterKeys { it < today }
            .values
            .mapNotNull { it[completionId] }
        val isPersonalBest = PerformanceTracker.wouldBePersonalBest(result, previousResults)
        val nextTodayResults = state.exerciseResultsToday + (completionId to result)

        loadedCompletionHistory = loadedCompletionHistory + (today to nextCompleted)
        loadedResultHistory = loadedResultHistory + (today to nextTodayResults)
        _uiState.update {
            stateWithCompletion(it, nextCompleted).copy(
                workoutSession = nextSession,
                exerciseResultsToday = nextTodayResults,
                performance = PerformanceTracker.build(loadedResultHistory),
                lastResultMessage = when {
                    isPersonalBest ->
                        "New personal best: ${PerformanceTracker.resultLabel(result)}"
                    result.hasMeasurableResult() ->
                        "Result saved: ${PerformanceTracker.resultLabel(result)}"
                    else -> "${result.effort.label} effort saved"
                },
            )
        }
        sessionSaveJob?.cancel()
        viewModelScope.launch {
            preferences.saveCompletedExercises(account.id, today, nextCompleted)
            preferences.saveExerciseResults(account.id, today, nextTodayResults)
            preferences.saveWorkoutSession(account.id, today, nextSession)
            syncDay(account, today)
        }
    }

    fun undoLastActiveSet() {
        val state = _uiState.value
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.undoLastSet(session)
        if (nextSession == session) return
        _uiState.update {
            it.copy(
                workoutSession = nextSession,
                lastResultMessage = "Last set reopened for editing",
            )
        }
        persistSession(state.account, nextSession)
    }

    fun tickSetTimer() {
        val state = _uiState.value
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.tickSetTimer(session)
        _uiState.update { it.copy(workoutSession = nextSession) }
        persistSession(state.account, nextSession)
    }

    fun toggleSetTimer() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.toggleSetTimer(session, workout)
        _uiState.update { it.copy(workoutSession = nextSession) }
        persistSession(state.account, nextSession)
    }

    fun resetSetTimer() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.resetSetTimer(session, workout)
        _uiState.update { it.copy(workoutSession = nextSession) }
        persistSession(state.account, nextSession)
    }

    fun skipActiveExercise() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.skipCurrent(session, workout)
        _uiState.update {
            it.copy(
                workoutSession = nextSession,
                lastResultMessage = null,
            )
        }
        persistSession(state.account, nextSession)
    }

    fun tickRestTimer() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.tickRest(session, workout)
        _uiState.update {
            it.copy(
                workoutSession = nextSession,
                lastResultMessage = if (nextSession.phase == session.phase) {
                    it.lastResultMessage
                } else {
                    null
                },
            )
        }
        persistSession(state.account, nextSession)
    }

    fun toggleRestTimer() {
        val state = _uiState.value
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.toggleRestTimer(session)
        _uiState.update {
            it.copy(
                workoutSession = nextSession,
            )
        }
        persistSession(state.account, nextSession)
    }

    fun skipRest() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        val nextSession = WorkoutSessionController.skipRest(session, workout)
        _uiState.update {
            it.copy(
                workoutSession = nextSession,
                lastResultMessage = null,
            )
        }
        persistSession(state.account, nextSession)
    }

    fun exitActiveWorkout() {
        val account = _uiState.value.account
        _uiState.update {
            it.copy(
                workoutSession = null,
                lastResultMessage = null,
            )
        }
        if (account != null) {
            sessionSaveJob?.cancel()
            viewModelScope.launch {
                preferences.clearWorkoutSession(account.id, today)
            }
        }
    }

    fun showProgress() {
        _uiState.update {
            it.copy(
                isProgressVisible = true,
                isPerformanceVisible = false,
                isPlanEditorVisible = false,
                isAccountVisible = false,
                selectedExerciseId = null,
            )
        }
    }

    fun hideProgress() {
        _uiState.update { it.copy(isProgressVisible = false) }
    }

    fun showPerformance() {
        _uiState.update {
            it.copy(
                isPerformanceVisible = true,
                isProgressVisible = false,
                isPlanEditorVisible = false,
                isAccountVisible = false,
                selectedExerciseId = null,
            )
        }
    }

    fun hidePerformance() {
        _uiState.update { it.copy(isPerformanceVisible = false) }
    }

    fun deleteExerciseResult(
        date: LocalDate,
        exerciseId: String,
    ) {
        val state = _uiState.value
        val account = state.account ?: return
        val existing = loadedResultHistory[date].orEmpty()
        if (exerciseId !in existing) return
        val updatedResults = existing - exerciseId
        loadedResultHistory = loadedResultHistory + (date to updatedResults)
        val profile = state.profile
        _uiState.update {
            it.copy(
                exerciseResultsToday = if (date == today) {
                    updatedResults
                } else {
                    it.exerciseResultsToday
                },
                performance = PerformanceTracker.build(loadedResultHistory),
                progress = if (profile == null) {
                    it.progress
                } else {
                    ProgressTracker.build(
                        profile = profile,
                        today = today,
                        completionHistory = loadedCompletionHistory,
                        resultHistory = loadedResultHistory,
                    )
                },
            )
        }
        viewModelScope.launch {
            preferences.saveExerciseResults(account.id, date, updatedResults)
            syncDay(account, date)
        }
    }

    fun showPlanEditor() {
        _uiState.update {
            it.copy(
                isPlanEditorVisible = true,
                isProgressVisible = false,
                isPerformanceVisible = false,
                isAccountVisible = false,
                selectedExerciseId = null,
            )
        }
    }

    fun hidePlanEditor() {
        _uiState.update { it.copy(isPlanEditorVisible = false) }
    }

    fun showAccount() {
        val account = _uiState.value.account ?: return
        _uiState.update {
            it.copy(
                isAccountVisible = true,
                isProgressVisible = false,
                isPerformanceVisible = false,
                isPlanEditorVisible = false,
                selectedExerciseId = null,
                errorMessage = null,
                noticeMessage = null,
            )
        }
        refreshAccountStatus(showNotice = false)
    }

    fun hideAccount() {
        _uiState.update {
            it.copy(
                isAccountVisible = false,
                errorMessage = null,
                noticeMessage = null,
            )
        }
    }

    fun refreshAccountStatus() {
        refreshAccountStatus(showNotice = true)
    }

    fun sendEmailVerification() {
        val account = _uiState.value.account ?: return
        if (account.provider != AccountProvider.EMAIL || account.isEmailVerified) return
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    errorMessage = null,
                    noticeMessage = null,
                )
            }
            runCatching { authRepository.sendEmailVerification() }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            noticeMessage =
                                "Verification email sent. Open the link, then refresh your status.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = authActionMessage(
                                error,
                                "Verification email could not be sent.",
                            ),
                        )
                    }
                }
        }
    }

    fun deleteAccount(reauthentication: AccountReauthentication) {
        val account = _uiState.value.account ?: return
        if (_uiState.value.isBusy) return
        if (
            reauthentication is AccountReauthentication.Password &&
            reauthentication.password.isBlank()
        ) {
            _uiState.update { it.copy(errorMessage = "Enter your account password.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    errorMessage = null,
                    noticeMessage = null,
                )
            }
            runCatching {
                authRepository.reauthenticate(reauthentication)
                check(cloudRepository.isConfigured) {
                    "Connect to the internet and try again."
                }
                withTimeout(ACCOUNT_DELETION_TIMEOUT_MILLIS) {
                    cloudRepository.deleteAccountData(account.id)
                }
                authRepository.deleteCurrentUser()
                preferences.clearAccount(account.id)
                WorkoutReminder.cancel(app)
            }.onSuccess {
                sessionSaveJob?.cancel()
                loadedCompletionHistory = emptyMap()
                loadedResultHistory = emptyMap()
                authRepository.signOut()
                authRepository.clearCredentialState()
                _uiState.value = PulsePlanUiState(
                    phase = AppPhase.SIGNED_OUT,
                    isFirebaseConfigured = authRepository.isConfigured,
                    noticeMessage = "Your account was deleted.",
                    cloudSyncState = if (cloudRepository.isConfigured) {
                        CloudSyncState.LOCAL_ONLY
                    } else {
                        CloudSyncState.SETUP_REQUIRED
                    },
                )
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        errorMessage = accountDeletionMessage(error),
                    )
                }
            }
        }
    }

    fun showExerciseGuide(exerciseId: String) {
        val state = _uiState.value
        if (state.workout?.exercises?.none { it.id == exerciseId } != false) return
        _uiState.update {
            it.copy(
                selectedExerciseId = exerciseId,
                isProgressVisible = false,
                isPerformanceVisible = false,
                isPlanEditorVisible = false,
                isAccountVisible = false,
            )
        }
    }

    fun hideExerciseGuide() {
        _uiState.update { it.copy(selectedExerciseId = null) }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        val account = _uiState.value.account ?: return
        val reminderTime = ReminderTime(hour = hour, minute = minute)
        _uiState.update { it.copy(reminderTime = reminderTime) }
        viewModelScope.launch {
            preferences.saveReminderTime(account.id, reminderTime)
            WorkoutReminder.schedule(
                context = app,
                hour = reminderTime.hour,
                minute = reminderTime.minute,
            )
            syncReminder(account)
        }
    }

    fun signOut() {
        authRepository.signOut()
        WorkoutReminder.cancel(app)
        loadedCompletionHistory = emptyMap()
        loadedResultHistory = emptyMap()
        _uiState.value = PulsePlanUiState(
            phase = AppPhase.SIGNED_OUT,
            isFirebaseConfigured = authRepository.isConfigured,
            cloudSyncState = if (cloudRepository.isConfigured) {
                CloudSyncState.LOCAL_ONLY
            } else {
                CloudSyncState.SETUP_REQUIRED
            },
        )
        viewModelScope.launch {
            authRepository.clearCredentialState()
        }
    }

    private fun refreshAccountStatus(showNotice: Boolean) {
        val account = _uiState.value.account ?: return
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    errorMessage = null,
                    noticeMessage = null,
                )
            }
            runCatching { authRepository.refreshCurrentUser() }
                .onSuccess { refreshedAccount ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            account = refreshedAccount,
                            noticeMessage = if (!showNotice) {
                                null
                            } else if (refreshedAccount.isEmailVerified) {
                                "Your email address is verified."
                            } else {
                                "Email verification is still pending."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = if (showNotice) {
                                authActionMessage(error, "Could not refresh your account.")
                            } else null,
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, noticeMessage = null) }
    }

    private fun saveCompletedExercises(
        account: AccountUser,
        completedExerciseIds: Set<String>,
    ) {
        viewModelScope.launch {
            preferences.saveCompletedExercises(account.id, today, completedExerciseIds)
            syncDay(account, today)
        }
    }

    private fun exerciseResultFromSets(
        exerciseId: String,
        exerciseName: String,
        prescription: String,
        sets: List<ExerciseSetResult>,
        effort: ExerciseEffort,
        notes: String,
    ): ExerciseResult {
        val metricType = sets.lastOrNull()?.metricType ?: ExerciseMetricType.REPS
        return ExerciseResult(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            prescription = prescription,
            metricType = metricType,
            reps = sets.mapNotNull(ExerciseSetResult::reps).maxOrNull(),
            weightKg = sets.mapNotNull(ExerciseSetResult::weightKg).maxOrNull(),
            durationSeconds = sets
                .mapNotNull(ExerciseSetResult::durationSeconds)
                .maxOrNull(),
            distanceKm = sets.mapNotNull(ExerciseSetResult::distanceKm).maxOrNull(),
            effort = effort,
            notes = notes.trim(),
            loggedAtEpochMillis = System.currentTimeMillis(),
            sets = sets,
        )
    }

    private fun persistSession(
        account: AccountUser?,
        session: WorkoutSessionState?,
    ) {
        if (account == null || session == null) return
        sessionSaveJob?.cancel()
        sessionSaveJob = viewModelScope.launch {
            preferences.saveWorkoutSession(account.id, today, session)
        }
    }

    private fun stateWithCompletion(
        state: PulsePlanUiState,
        completedExerciseIds: Set<String>,
    ): PulsePlanUiState {
        val profile = state.profile
        return state.copy(
            completedExerciseIds = completedExerciseIds,
            progress = if (profile == null) {
                state.progress
            } else {
                ProgressTracker.build(
                    profile = profile,
                    today = today,
                    completionHistory = loadedCompletionHistory,
                    resultHistory = loadedResultHistory,
                )
            },
        )
    }

    private fun authenticate(block: suspend () -> AccountUser) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    errorMessage = null,
                    noticeMessage = null,
                )
            }
            runCatching { block() }
                .onSuccess(::openAccount)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = authenticationMessage(error),
                        )
                    }
                }
        }
    }

    private fun openAccount(account: AccountUser) {
        _uiState.update {
            it.copy(
                phase = AppPhase.LOADING,
                isBusy = false,
                account = account,
                errorMessage = null,
                noticeMessage = null,
                cloudSyncState = if (cloudRepository.isConfigured) {
                    CloudSyncState.SYNCING
                } else {
                    CloudSyncState.SETUP_REQUIRED
                },
                cloudSyncMessage = null,
            )
        }
        viewModelScope.launch {
            if (cloudRepository.isConfigured) {
                syncAccountBeforeOpening(account)
            }
            val profile = preferences.profile(account.id).first()
            if (profile == null) {
                _uiState.update { it.copy(phase = AppPhase.ONBOARDING) }
            } else {
                showDashboard(account, profile)
            }
        }
    }

    private suspend fun showDashboard(account: AccountUser, profile: UserFitnessProfile) {
        val baseWorkout = PlanGenerator.workoutFor(profile, today)
        val savedSubstitutions = preferences
            .exerciseSubstitutions(account.id, today)
            .first()
        val substitutions = PlanGenerator.sanitizeSubstitutions(
            profile = profile,
            workout = baseWorkout,
            selectedSubstitutions = savedSubstitutions,
        )
        if (substitutions != savedSubstitutions) {
            preferences.saveExerciseSubstitutions(account.id, today, substitutions)
        }
        val workout = PlanGenerator.applySubstitutions(
            profile = profile,
            workout = baseWorkout,
            selectedSubstitutions = substitutions,
        )
        val historyDates = (0 until HISTORY_LOOKBACK_DAYS).map { offset ->
            today.minusDays(offset.toLong())
        }
        loadedCompletionHistory = preferences
            .completionHistory(account.id, historyDates)
            .first()
        loadedResultHistory = preferences
            .resultHistory(account.id, historyDates)
            .first()
        val savedCompleted = loadedCompletionHistory[today].orEmpty()
        val validExerciseIds = baseWorkout.exercises.map { it.sourceExerciseId }.toSet()
        val completed = savedCompleted.intersect(validExerciseIds)
        if (completed != savedCompleted) {
            preferences.saveCompletedExercises(account.id, today, completed)
            loadedCompletionHistory = loadedCompletionHistory + (today to completed)
        }
        val reminderTime = preferences.reminderTime(account.id).first()
        val restoredSession = WorkoutSessionController.restore(
            workout = workout,
            saved = preferences.workoutSession(account.id, today).first(),
        )
        WorkoutReminder.schedule(
            context = app,
            hour = reminderTime.hour,
            minute = reminderTime.minute,
        )
        _uiState.update {
            it.copy(
                phase = AppPhase.DASHBOARD,
                isBusy = false,
                profile = profile,
                workout = workout,
                week = PlanGenerator.weekFor(profile, today),
                completedExerciseIds = completed,
                exerciseSubstitutions = substitutions,
                reminderTime = reminderTime,
                workoutSession = restoredSession,
                progress = ProgressTracker.build(
                    profile = profile,
                    today = today,
                    completionHistory = loadedCompletionHistory,
                    resultHistory = loadedResultHistory,
                ),
                exerciseResultsToday = loadedResultHistory[today].orEmpty(),
                performance = PerformanceTracker.build(loadedResultHistory),
                lastResultMessage = null,
                isProgressVisible = false,
                isPerformanceVisible = false,
                isPlanEditorVisible = false,
                isAccountVisible = false,
                selectedExerciseId = null,
            )
        }
    }

    private fun completionIdFor(
        displayedExerciseId: String,
        substitutions: Map<String, String>,
    ): String = substitutions.entries
        .firstOrNull { (_, replacementId) -> replacementId == displayedExerciseId }
        ?.key
        ?: displayedExerciseId

    private suspend fun syncAccountBeforeOpening(account: AccountUser) {
        val dates = historyDates()
        runCatching {
            withTimeout(CLOUD_SYNC_TIMEOUT_MILLIS) {
                val local = preferences.cloudSnapshot(account.id, dates)
                val remote = cloudRepository.load(
                    accountId = account.id,
                    earliestDate = dates.last(),
                )
                val merged = CloudSyncMerger.merge(local, remote)
                preferences.applyCloudSnapshot(account.id, merged)
                cloudRepository.saveSnapshot(account.id, merged)
            }
        }.onSuccess {
            _uiState.update {
                it.copy(
                    cloudSyncState = CloudSyncState.UP_TO_DATE,
                    cloudSyncMessage = null,
                )
            }
        }.onFailure {
            showLocalOnlySyncState()
        }
    }

    private suspend fun syncProfile(account: AccountUser) {
        if (!canSync(account)) return
        updateSyncState(CloudSyncState.SYNCING)
        runCatching {
            withTimeout(CLOUD_SYNC_TIMEOUT_MILLIS) {
                val record = requireNotNull(
                    preferences.cloudSnapshot(account.id, emptyList()).profile,
                )
                cloudRepository.saveProfile(account.id, record)
            }
        }.onSuccess {
            updateSyncState(CloudSyncState.UP_TO_DATE)
        }.onFailure {
            showLocalOnlySyncState()
        }
    }

    private suspend fun syncReminder(account: AccountUser) {
        if (!canSync(account)) return
        updateSyncState(CloudSyncState.SYNCING)
        runCatching {
            withTimeout(CLOUD_SYNC_TIMEOUT_MILLIS) {
                val record = requireNotNull(
                    preferences.cloudSnapshot(account.id, emptyList()).reminderTime,
                )
                cloudRepository.saveReminder(account.id, record)
            }
        }.onSuccess {
            updateSyncState(CloudSyncState.UP_TO_DATE)
        }.onFailure {
            showLocalOnlySyncState()
        }
    }

    private suspend fun syncDay(account: AccountUser, date: LocalDate) {
        if (!canSync(account)) return
        updateSyncState(CloudSyncState.SYNCING)
        runCatching {
            withTimeout(CLOUD_SYNC_TIMEOUT_MILLIS) {
                val day = requireNotNull(
                    preferences.cloudSnapshot(account.id, listOf(date)).days[date],
                )
                cloudRepository.saveDay(account.id, date, day)
            }
        }.onSuccess {
            updateSyncState(CloudSyncState.UP_TO_DATE)
        }.onFailure {
            showLocalOnlySyncState()
        }
    }

    private fun canSync(account: AccountUser): Boolean =
        cloudRepository.isConfigured

    private fun updateSyncState(syncState: CloudSyncState) {
        _uiState.update {
            it.copy(
                cloudSyncState = syncState,
                cloudSyncMessage = null,
            )
        }
    }

    private fun showLocalOnlySyncState() {
        _uiState.update {
            it.copy(
                cloudSyncState = CloudSyncState.LOCAL_ONLY,
                cloudSyncMessage = "Backup will retry automatically.",
            )
        }
    }

    private fun authenticationMessage(error: Throwable): String = when (error) {
        is FirebaseAuthUserCollisionException ->
            "That email is already linked to another sign-in method."
        is FirebaseAuthInvalidUserException ->
            "No account was found."
        is FirebaseAuthInvalidCredentialsException ->
            "Your sign-in details are incorrect."
        is FirebaseNetworkException ->
            "Check your internet connection."
        else -> "Sign-in failed. Try again."
    }

    private fun authActionMessage(error: Throwable, fallback: String): String = when (error) {
        is FirebaseNetworkException -> "Check your internet connection."
        else -> fallback
    }

    private fun accountDeletionMessage(error: Throwable): String = when (error) {
        is FirebaseAuthInvalidCredentialsException ->
            "We could not confirm your sign-in. Nothing was deleted."
        is FirebaseAuthRecentLoginRequiredException ->
            "Sign in again, then retry."
        is FirebaseNetworkException ->
            "Check your internet connection. Nothing was deleted."
        else -> "Account deletion failed. Nothing was deleted."
    }

    private fun historyDates(): List<LocalDate> =
        (0 until HISTORY_LOOKBACK_DAYS).map { offset ->
            today.minusDays(offset.toLong())
        }

    private companion object {
        const val HISTORY_LOOKBACK_DAYS = 365
        const val CLOUD_SYNC_TIMEOUT_MILLIS = 15_000L
        const val ACCOUNT_DELETION_TIMEOUT_MILLIS = 45_000L
    }
}
