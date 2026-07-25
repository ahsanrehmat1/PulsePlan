package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkoutSessionTest {
    private val workout = DailyWorkout(
        date = LocalDate.of(2026, 7, 25),
        title = "Test workout",
        focus = "Test focus",
        estimatedMinutes = 20,
        exercises = listOf(
            exercise(id = "one", restSeconds = 30),
            exercise(id = "two", restSeconds = 45),
            exercise(id = "three", restSeconds = 60),
        ),
    )

    @Test
    fun `session starts at first unfinished exercise`() {
        val state = WorkoutSessionController.start(
            workout = workout,
            completedExerciseIds = setOf("one"),
        )

        assertEquals(1, state.currentExerciseIndex)
        assertEquals(WorkoutSessionPhase.EXERCISE, state.phase)
    }

    @Test
    fun `session starts complete when every exercise is already done`() {
        val state = WorkoutSessionController.start(
            workout = workout,
            completedExerciseIds = setOf("one", "two", "three"),
        )

        assertEquals(WorkoutSessionPhase.COMPLETE, state.phase)
    }

    @Test
    fun `completing an exercise starts its rest timer`() {
        val started = WorkoutSessionController.start(workout, emptySet())

        val resting = WorkoutSessionController.completeCurrent(started, workout)

        assertEquals(WorkoutSessionPhase.REST, resting.phase)
        assertEquals(30, resting.restSecondsRemaining)
        assertTrue(resting.isRestTimerRunning)
    }

    @Test
    fun `paused rest timer does not count down`() {
        val resting = WorkoutSessionController.completeCurrent(
            WorkoutSessionController.start(workout, emptySet()),
            workout,
        )
        val paused = WorkoutSessionController.toggleRestTimer(resting)

        val afterTick = WorkoutSessionController.tickRest(paused, workout)

        assertFalse(afterTick.isRestTimerRunning)
        assertEquals(30, afterTick.restSecondsRemaining)
    }

    @Test
    fun `rest timer advances to next exercise at zero`() {
        val resting = WorkoutSessionState(
            currentExerciseIndex = 0,
            phase = WorkoutSessionPhase.REST,
            restSecondsRemaining = 1,
            isRestTimerRunning = true,
        )

        val next = WorkoutSessionController.tickRest(resting, workout)

        assertEquals(1, next.currentExerciseIndex)
        assertEquals(WorkoutSessionPhase.EXERCISE, next.phase)
        assertEquals(0, next.restSecondsRemaining)
    }

    @Test
    fun `completing final exercise finishes session`() {
        val finalExercise = WorkoutSessionState(
            currentExerciseIndex = workout.exercises.lastIndex,
            phase = WorkoutSessionPhase.EXERCISE,
        )

        val complete = WorkoutSessionController.completeCurrent(finalExercise, workout)

        assertEquals(WorkoutSessionPhase.COMPLETE, complete.phase)
        assertFalse(complete.isRestTimerRunning)
    }

    private fun exercise(id: String, restSeconds: Int) = Exercise(
        id = id,
        name = "Exercise $id",
        prescription = "3 x 10",
        restSeconds = restSeconds,
        coachingCue = "Move with control.",
    )
}
