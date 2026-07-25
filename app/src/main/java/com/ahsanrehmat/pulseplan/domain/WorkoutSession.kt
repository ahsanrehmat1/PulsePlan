package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.DailyWorkout

enum class WorkoutSessionPhase {
    EXERCISE,
    REST,
    COMPLETE,
}

data class WorkoutSessionState(
    val currentExerciseIndex: Int,
    val phase: WorkoutSessionPhase,
    val restSecondsRemaining: Int = 0,
    val isRestTimerRunning: Boolean = false,
)

object WorkoutSessionController {
    fun start(
        workout: DailyWorkout,
        completedExerciseIds: Set<String>,
    ): WorkoutSessionState {
        val firstIncompleteIndex = workout.exercises.indexOfFirst {
            it.id !in completedExerciseIds
        }
        return if (firstIncompleteIndex == -1) {
            WorkoutSessionState(
                currentExerciseIndex = workout.exercises.lastIndex.coerceAtLeast(0),
                phase = WorkoutSessionPhase.COMPLETE,
            )
        } else {
            WorkoutSessionState(
                currentExerciseIndex = firstIncompleteIndex,
                phase = WorkoutSessionPhase.EXERCISE,
            )
        }
    }

    fun completeCurrent(
        state: WorkoutSessionState,
        workout: DailyWorkout,
    ): WorkoutSessionState {
        if (state.phase != WorkoutSessionPhase.EXERCISE) return state
        val exercise = workout.exercises.getOrNull(state.currentExerciseIndex)
            ?: return state.copy(phase = WorkoutSessionPhase.COMPLETE)
        if (state.currentExerciseIndex >= workout.exercises.lastIndex) {
            return state.copy(
                phase = WorkoutSessionPhase.COMPLETE,
                restSecondsRemaining = 0,
                isRestTimerRunning = false,
            )
        }
        return state.copy(
            phase = WorkoutSessionPhase.REST,
            restSecondsRemaining = exercise.restSeconds,
            isRestTimerRunning = true,
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
            moveToNextExercise(state, workout)
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
        return moveToNextExercise(state, workout)
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
            )
        } else {
            WorkoutSessionState(
                currentExerciseIndex = nextIndex,
                phase = WorkoutSessionPhase.EXERCISE,
            )
        }
    }
}
