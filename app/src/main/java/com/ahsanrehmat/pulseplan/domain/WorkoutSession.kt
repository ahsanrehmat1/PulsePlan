package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseSetDraft
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult

enum class WorkoutSessionPhase {
    EXERCISE,
    REST,
    COMPLETE,
}

enum class WorkoutRestKind {
    BETWEEN_SETS,
    BETWEEN_EXERCISES,
}

data class WorkoutSessionState(
    val currentExerciseIndex: Int,
    val phase: WorkoutSessionPhase,
    val restSecondsRemaining: Int = 0,
    val isRestTimerRunning: Boolean = false,
    val currentSetIndex: Int = 0,
    val completedSets: List<ExerciseSetResult> = emptyList(),
    val currentSetDraft: ExerciseSetDraft? = null,
    val exerciseEffort: ExerciseEffort = ExerciseEffort.GOOD,
    val exerciseNotes: String = "",
    val restKind: WorkoutRestKind = WorkoutRestKind.BETWEEN_EXERCISES,
    val setTimerSecondsRemaining: Int = 0,
    val isSetTimerRunning: Boolean = false,
    val sessionStartedAtEpochMillis: Long = 0L,
)

object WorkoutSessionController {
    fun restore(
        workout: DailyWorkout,
        saved: WorkoutSessionState?,
    ): WorkoutSessionState? {
        if (saved == null || workout.exercises.isEmpty()) return null
        if (saved.currentExerciseIndex !in workout.exercises.indices) return null

        val exercise = workout.exercises[saved.currentExerciseIndex]
        val prescription = WorkoutPrescriptionParser.parse(exercise)
        val currentSetIndex = saved.currentSetIndex
            .coerceIn(0, prescription.setCount - 1)
        val completedSets = saved.completedSets
            .filter { it.setNumber in 1..prescription.setCount }
            .distinctBy(ExerciseSetResult::setNumber)
            .sortedBy(ExerciseSetResult::setNumber)
        val plannedDraft = WorkoutPrescriptionParser.plannedDraft(exercise)
        val base = saved.copy(
            currentSetIndex = currentSetIndex,
            completedSets = completedSets,
            currentSetDraft = saved.currentSetDraft ?: plannedDraft,
            exerciseNotes = saved.exerciseNotes.take(MAX_NOTES_LENGTH),
            sessionStartedAtEpochMillis = saved.sessionStartedAtEpochMillis
                .takeIf { it > 0L }
                ?: System.currentTimeMillis(),
        )
        return when (base.phase) {
            WorkoutSessionPhase.EXERCISE -> base.copy(
                restSecondsRemaining = 0,
                isRestTimerRunning = false,
                isSetTimerRunning = base.isSetTimerRunning &&
                    base.setTimerSecondsRemaining > 0,
            )

            WorkoutSessionPhase.COMPLETE -> base.copy(
                restSecondsRemaining = 0,
                isRestTimerRunning = false,
                isSetTimerRunning = false,
            )

            WorkoutSessionPhase.REST -> {
                val validSetRest = base.restKind == WorkoutRestKind.BETWEEN_SETS &&
                    prescription.setCount > 1 &&
                    base.currentSetIndex > 0
                val validExerciseRest =
                    base.restKind == WorkoutRestKind.BETWEEN_EXERCISES &&
                        base.currentExerciseIndex < workout.exercises.lastIndex
                if (!validSetRest && !validExerciseRest) {
                    null
                } else {
                    base.copy(
                        restSecondsRemaining = base.restSecondsRemaining.coerceAtLeast(0),
                        isSetTimerRunning = false,
                    )
                }
            }
        }
    }

    fun start(
        workout: DailyWorkout,
        completedExerciseIds: Set<String>,
        startedAtEpochMillis: Long = System.currentTimeMillis(),
    ): WorkoutSessionState {
        val firstIncompleteIndex = workout.exercises.indexOfFirst {
            it.id !in completedExerciseIds
        }
        return if (firstIncompleteIndex == -1) {
            WorkoutSessionState(
                currentExerciseIndex = workout.exercises.lastIndex.coerceAtLeast(0),
                phase = WorkoutSessionPhase.COMPLETE,
                sessionStartedAtEpochMillis = startedAtEpochMillis,
            )
        } else {
            exerciseState(
                workout = workout,
                exerciseIndex = firstIncompleteIndex,
                startedAtEpochMillis = startedAtEpochMillis,
            )
        }
    }

    fun updateCurrentSetDraft(
        state: WorkoutSessionState,
        draft: ExerciseSetDraft,
    ): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.EXERCISE) return state
        val nextTimerSeconds = when {
            draft.metricType != ExerciseMetricType.TIME -> 0
            state.currentSetDraft?.metricType == ExerciseMetricType.TIME &&
                state.isSetTimerRunning ->
                state.setTimerSecondsRemaining
            else -> draft.durationSeconds ?: 0
        }
        return state.copy(
            currentSetDraft = draft,
            setTimerSecondsRemaining = nextTimerSeconds.coerceAtLeast(0),
            isSetTimerRunning = state.isSetTimerRunning && nextTimerSeconds > 0,
        )
    }

    fun updateExerciseFeedback(
        state: WorkoutSessionState,
        effort: ExerciseEffort,
        notes: String,
    ): WorkoutSessionState = state.copy(
        exerciseEffort = effort,
        exerciseNotes = notes.take(MAX_NOTES_LENGTH),
    )

    fun completeCurrentSet(
        state: WorkoutSessionState,
        workout: DailyWorkout,
        draft: ExerciseSetDraft = state.currentSetDraft
            ?: ExerciseSetDraft(ExerciseMetricType.REPS),
    ): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.EXERCISE) return state
        val exercise = workout.exercises.getOrNull(state.currentExerciseIndex)
            ?: return state.copy(phase = WorkoutSessionPhase.COMPLETE)
        val prescription = WorkoutPrescriptionParser.parse(exercise)
        val setNumber = state.currentSetIndex + 1
        val result = ExerciseSetResult(
            setNumber = setNumber,
            metricType = draft.metricType,
            reps = draft.reps,
            weightKg = draft.weightKg,
            durationSeconds = draft.durationSeconds,
            distanceKm = draft.distanceKm,
        )
        val completedSets = (state.completedSets.filterNot {
            it.setNumber == setNumber
        } + result).sortedBy(ExerciseSetResult::setNumber)

        return when {
            setNumber < prescription.setCount -> {
                val nextDraft = draft
                state.copy(
                    phase = WorkoutSessionPhase.REST,
                    restSecondsRemaining = exercise.restSeconds,
                    isRestTimerRunning = true,
                    currentSetIndex = state.currentSetIndex + 1,
                    completedSets = completedSets,
                    currentSetDraft = nextDraft,
                    restKind = WorkoutRestKind.BETWEEN_SETS,
                    setTimerSecondsRemaining = nextDraft.durationSeconds ?: 0,
                    isSetTimerRunning = false,
                )
            }

            state.currentExerciseIndex >= workout.exercises.lastIndex -> state.copy(
                phase = WorkoutSessionPhase.COMPLETE,
                restSecondsRemaining = 0,
                isRestTimerRunning = false,
                completedSets = completedSets,
                isSetTimerRunning = false,
            )

            else -> state.copy(
                phase = WorkoutSessionPhase.REST,
                restSecondsRemaining = exercise.restSeconds,
                isRestTimerRunning = true,
                completedSets = completedSets,
                restKind = WorkoutRestKind.BETWEEN_EXERCISES,
                isSetTimerRunning = false,
            )
        }
    }

    fun undoLastSet(
        state: WorkoutSessionState,
    ): WorkoutSessionState {
        if (state.completedSets.isEmpty()) return state
        if (
            state.phase == WorkoutSessionPhase.COMPLETE ||
            (
                state.phase == WorkoutSessionPhase.REST &&
                    state.restKind == WorkoutRestKind.BETWEEN_EXERCISES
                )
        ) {
            return state
        }
        val lastSet = state.completedSets.last()
        return state.copy(
            phase = WorkoutSessionPhase.EXERCISE,
            restSecondsRemaining = 0,
            isRestTimerRunning = false,
            currentSetIndex = (lastSet.setNumber - 1).coerceAtLeast(0),
            completedSets = state.completedSets.dropLast(1),
            currentSetDraft = lastSet.asDraft(),
            setTimerSecondsRemaining = lastSet.durationSeconds ?: 0,
            isSetTimerRunning = false,
        )
    }

    fun skipCurrent(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.EXERCISE) return state
        return moveToNextExercise(state, workout)
    }

    fun tickRest(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState {
        if (
            state.phase != WorkoutSessionPhase.REST ||
            !state.isRestTimerRunning
        ) {
            return state
        }
        return if (state.restSecondsRemaining <= 1) {
            finishRest(state, workout)
        } else {
            state.copy(restSecondsRemaining = state.restSecondsRemaining - 1)
        }
    }

    fun toggleRestTimer(state: WorkoutSessionState): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.REST) return state
        return state.copy(isRestTimerRunning = !state.isRestTimerRunning)
    }

    fun skipRest(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.REST) return state
        return finishRest(state, workout)
    }

    fun tickSetTimer(state: WorkoutSessionState): WorkoutSessionState {
        if (
            state.phase != WorkoutSessionPhase.EXERCISE ||
            !state.isSetTimerRunning
        ) {
            return state
        }
        val startingSeconds = state.currentSetDraft?.durationSeconds
            ?: state.setTimerSecondsRemaining
        return if (state.setTimerSecondsRemaining <= 1) {
            state.copy(
                setTimerSecondsRemaining = 0,
                isSetTimerRunning = false,
                currentSetDraft = (state.currentSetDraft
                    ?: ExerciseSetDraft(ExerciseMetricType.TIME)).copy(
                    metricType = ExerciseMetricType.TIME,
                    durationSeconds = startingSeconds.coerceAtLeast(1),
                ),
            )
        } else {
            state.copy(setTimerSecondsRemaining = state.setTimerSecondsRemaining - 1)
        }
    }

    fun toggleSetTimer(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.EXERCISE) return state
        val exercise = workout.exercises.getOrNull(state.currentExerciseIndex) ?: return state
        val plannedSeconds = WorkoutPrescriptionParser.parse(exercise).targetSeconds ?: 0
        val timerSeconds = state.setTimerSecondsRemaining
            .takeIf { it > 0 }
            ?: state.currentSetDraft?.durationSeconds
            ?: plannedSeconds
        if (timerSeconds <= 0) return state
        return state.copy(
            setTimerSecondsRemaining = timerSeconds,
            isSetTimerRunning = !state.isSetTimerRunning,
        )
    }

    fun resetSetTimer(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.EXERCISE) return state
        val exercise = workout.exercises.getOrNull(state.currentExerciseIndex) ?: return state
        val seconds = state.currentSetDraft?.durationSeconds
            ?: WorkoutPrescriptionParser.parse(exercise).targetSeconds
            ?: 0
        return state.copy(
            setTimerSecondsRemaining = seconds.coerceAtLeast(0),
            isSetTimerRunning = false,
        )
    }

    private fun finishRest(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState = when (state.restKind) {
        WorkoutRestKind.BETWEEN_SETS -> state.copy(
            phase = WorkoutSessionPhase.EXERCISE,
            restSecondsRemaining = 0,
            isRestTimerRunning = false,
        )

        WorkoutRestKind.BETWEEN_EXERCISES -> moveToNextExercise(state, workout)
    }

    private fun moveToNextExercise(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState {
        val nextIndex = state.currentExerciseIndex + 1
        return if (nextIndex > workout.exercises.lastIndex) {
            state.copy(
                phase = WorkoutSessionPhase.COMPLETE,
                restSecondsRemaining = 0,
                isRestTimerRunning = false,
                isSetTimerRunning = false,
            )
        } else {
            exerciseState(
                workout = workout,
                exerciseIndex = nextIndex,
                startedAtEpochMillis = state.sessionStartedAtEpochMillis,
            )
        }
    }

    private fun exerciseState(
        workout: DailyWorkout,
        exerciseIndex: Int,
        startedAtEpochMillis: Long,
    ): WorkoutSessionState {
        val exercise = workout.exercises[exerciseIndex]
        val draft = WorkoutPrescriptionParser.plannedDraft(exercise)
        return WorkoutSessionState(
            currentExerciseIndex = exerciseIndex,
            phase = WorkoutSessionPhase.EXERCISE,
            currentSetDraft = draft,
            setTimerSecondsRemaining = draft.durationSeconds ?: 0,
            sessionStartedAtEpochMillis = startedAtEpochMillis,
        )
    }

    private const val MAX_NOTES_LENGTH = 160
}
