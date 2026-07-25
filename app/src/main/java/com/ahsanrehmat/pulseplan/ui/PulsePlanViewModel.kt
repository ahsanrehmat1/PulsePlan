package com.ahsanrehmat.pulseplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ahsanrehmat.pulseplan.data.AccountUser
import com.ahsanrehmat.pulseplan.data.CloudSyncMerger
import com.ahsanrehmat.pulseplan.data.CloudSyncRepository
import com.ahsanrehmat.pulseplan.data.FirebaseAuthRepository
import com.ahsanrehmat.pulseplan.data.FirebaseCloudSyncRepository
import com.ahsanrehmat.pulseplan.data.UserPreferences
import com.ahsanrehmat.pulseplan.domain.PlanGenerator
import com.ahsanrehmat.pulseplan.domain.ProgressSnapshot
import com.ahsanrehmat.pulseplan.domain.ProgressTracker
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionController
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionState
import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.ReminderTime
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WeekDayPlan
import com.ahsanrehmat.pulseplan.notifications.WorkoutReminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    PREVIEW,
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
    val isProgressVisible: Boolean = false,
    val isPlanEditorVisible: Boolean = false,
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
                            errorMessage = error.message
                                ?: "Password reset could not be sent.",
                        )
                    }
                }
        }
    }

    fun continueInPreview() {
        openAccount(AccountUser(id = PREVIEW_USER_ID, email = "preview@pulseplan.app"))
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
            )
        }
    }

    fun completeActiveExercise() {
        val state = _uiState.value
        val account = state.account ?: return
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        val exercise = workout.exercises.getOrNull(session.currentExerciseIndex) ?: return
        val completionId = exercise.sourceExerciseId
        val nextCompleted = state.completedExerciseIds + completionId
        val nextSession = WorkoutSessionController.completeCurrent(session, workout)

        loadedCompletionHistory = loadedCompletionHistory + (today to nextCompleted)
        _uiState.update {
            stateWithCompletion(it, nextCompleted).copy(workoutSession = nextSession)
        }
        saveCompletedExercises(account, nextCompleted)
    }

    fun skipActiveExercise() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        _uiState.update {
            it.copy(
                workoutSession = WorkoutSessionController.skipCurrent(session, workout),
            )
        }
    }

    fun tickRestTimer() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        _uiState.update {
            it.copy(
                workoutSession = WorkoutSessionController.tickRest(session, workout),
            )
        }
    }

    fun toggleRestTimer() {
        val session = _uiState.value.workoutSession ?: return
        _uiState.update {
            it.copy(
                workoutSession = WorkoutSessionController.toggleRestTimer(session),
            )
        }
    }

    fun skipRest() {
        val state = _uiState.value
        val workout = state.workout ?: return
        val session = state.workoutSession ?: return
        _uiState.update {
            it.copy(
                workoutSession = WorkoutSessionController.skipRest(session, workout),
            )
        }
    }

    fun exitActiveWorkout() {
        _uiState.update { it.copy(workoutSession = null) }
    }

    fun showProgress() {
        _uiState.update {
            it.copy(
                isProgressVisible = true,
                isPlanEditorVisible = false,
                selectedExerciseId = null,
            )
        }
    }

    fun hideProgress() {
        _uiState.update { it.copy(isProgressVisible = false) }
    }

    fun showPlanEditor() {
        _uiState.update {
            it.copy(
                isPlanEditorVisible = true,
                isProgressVisible = false,
                selectedExerciseId = null,
            )
        }
    }

    fun hidePlanEditor() {
        _uiState.update { it.copy(isPlanEditorVisible = false) }
    }

    fun showExerciseGuide(exerciseId: String) {
        val state = _uiState.value
        if (state.workout?.exercises?.none { it.id == exerciseId } != false) return
        _uiState.update {
            it.copy(
                selectedExerciseId = exerciseId,
                isProgressVisible = false,
                isPlanEditorVisible = false,
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
        if (_uiState.value.account?.id != PREVIEW_USER_ID) {
            authRepository.signOut()
        }
        loadedCompletionHistory = emptyMap()
        _uiState.value = PulsePlanUiState(
            phase = AppPhase.SIGNED_OUT,
            isFirebaseConfigured = authRepository.isConfigured,
            cloudSyncState = if (cloudRepository.isConfigured) {
                CloudSyncState.LOCAL_ONLY
            } else {
                CloudSyncState.SETUP_REQUIRED
            },
        )
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
                ProgressTracker.build(profile, today, loadedCompletionHistory)
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
                            errorMessage = error.message ?: "Something went wrong. Please try again.",
                        )
                    }
                }
        }
    }

    private fun openAccount(account: AccountUser) {
        val isPreview = account.id == PREVIEW_USER_ID
        _uiState.update {
            it.copy(
                phase = AppPhase.LOADING,
                isBusy = false,
                account = account,
                errorMessage = null,
                noticeMessage = null,
                cloudSyncState = when {
                    isPreview -> CloudSyncState.PREVIEW
                    !cloudRepository.isConfigured -> CloudSyncState.SETUP_REQUIRED
                    else -> CloudSyncState.SYNCING
                },
                cloudSyncMessage = null,
            )
        }
        viewModelScope.launch {
            if (!isPreview && cloudRepository.isConfigured) {
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
        val savedCompleted = loadedCompletionHistory[today].orEmpty()
        val validExerciseIds = baseWorkout.exercises.map { it.sourceExerciseId }.toSet()
        val completed = savedCompleted.intersect(validExerciseIds)
        if (completed != savedCompleted) {
            preferences.saveCompletedExercises(account.id, today, completed)
            loadedCompletionHistory = loadedCompletionHistory + (today to completed)
        }
        val reminderTime = preferences.reminderTime(account.id).first()
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
                workoutSession = null,
                progress = ProgressTracker.build(profile, today, loadedCompletionHistory),
                isProgressVisible = false,
                isPlanEditorVisible = false,
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
        account.id != PREVIEW_USER_ID && cloudRepository.isConfigured

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
                cloudSyncMessage =
                    "Your changes are safe on this device. Cloud backup will retry next sign-in.",
            )
        }
    }

    private fun historyDates(): List<LocalDate> =
        (0 until HISTORY_LOOKBACK_DAYS).map { offset ->
            today.minusDays(offset.toLong())
        }

    private companion object {
        const val PREVIEW_USER_ID = "pulse-plan-preview-user"
        const val HISTORY_LOOKBACK_DAYS = 365
        const val CLOUD_SYNC_TIMEOUT_MILLIS = 15_000L
    }
}
