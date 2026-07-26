package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.DailyWorkout
import com.ahsanrehmat.pulseplan.model.Exercise
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseSetDraft
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun `session starts at first unfinished exercise with its planned first set`() {
        val state = WorkoutSessionController.start(
            workout = workout,
            completedExerciseIds = setOf("one"),
            startedAtEpochMillis = 123L,
        )

        assertEquals(1, state.currentExerciseIndex)
        assertEquals(0, state.currentSetIndex)
        assertEquals(10, state.currentSetDraft?.reps)
        assertEquals(WorkoutSessionPhase.EXERCISE, state.phase)
        assertEquals(123L, state.sessionStartedAtEpochMillis)
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
    fun `completing first set starts between-set rest and keeps its result`() {
        val started = WorkoutSessionController.start(workout, emptySet())

        val resting = WorkoutSessionController.completeCurrentSet(
            state = started,
            workout = workout,
            draft = reps(12),
        )

        assertEquals(WorkoutSessionPhase.REST, resting.phase)
        assertEquals(WorkoutRestKind.BETWEEN_SETS, resting.restKind)
        assertEquals(30, resting.restSecondsRemaining)
        assertTrue(resting.isRestTimerRunning)
        assertEquals(1, resting.currentSetIndex)
        assertEquals(12, resting.completedSets.single().reps)
    }

    @Test
    fun `paused rest timer does not count down`() {
        val resting = WorkoutSessionController.completeCurrentSet(
            WorkoutSessionController.start(workout, emptySet()),
            workout,
            reps(10),
        )
        val paused = WorkoutSessionController.toggleRestTimer(resting)

        val afterTick = WorkoutSessionController.tickRest(paused, workout)

        assertFalse(afterTick.isRestTimerRunning)
        assertEquals(30, afterTick.restSecondsRemaining)
    }

    @Test
    fun `between-set rest returns to same exercise and next set`() {
        val resting = WorkoutSessionController.completeCurrentSet(
            WorkoutSessionController.start(workout, emptySet()),
            workout,
            reps(10),
        ).copy(restSecondsRemaining = 1)

        val next = WorkoutSessionController.tickRest(resting, workout)

        assertEquals(0, next.currentExerciseIndex)
        assertEquals(1, next.currentSetIndex)
        assertEquals(WorkoutSessionPhase.EXERCISE, next.phase)
        assertEquals(10, next.currentSetDraft?.reps)
    }

    @Test
    fun `final set of an exercise starts between-exercise rest`() {
        val finalSet = WorkoutSessionState(
            currentExerciseIndex = 0,
            phase = WorkoutSessionPhase.EXERCISE,
            currentSetIndex = 2,
            completedSets = listOf(set(1, 10), set(2, 10)),
            currentSetDraft = reps(10),
        )

        val resting = WorkoutSessionController.completeCurrentSet(
            finalSet,
            workout,
            reps(11),
        )

        assertEquals(WorkoutSessionPhase.REST, resting.phase)
        assertEquals(WorkoutRestKind.BETWEEN_EXERCISES, resting.restKind)
        assertEquals(3, resting.completedSets.size)
        assertEquals(30, resting.restSecondsRemaining)
    }

    @Test
    fun `finishing rest after final set advances and clears set data`() {
        val exerciseRest = WorkoutSessionState(
            currentExerciseIndex = 0,
            phase = WorkoutSessionPhase.REST,
            restSecondsRemaining = 1,
            isRestTimerRunning = true,
            currentSetIndex = 2,
            completedSets = listOf(set(1, 10), set(2, 10), set(3, 10)),
            restKind = WorkoutRestKind.BETWEEN_EXERCISES,
            sessionStartedAtEpochMillis = 100L,
        )

        val next = WorkoutSessionController.tickRest(exerciseRest, workout)

        assertEquals(1, next.currentExerciseIndex)
        assertEquals(0, next.currentSetIndex)
        assertEquals(emptyList<ExerciseSetResult>(), next.completedSets)
        assertEquals(100L, next.sessionStartedAtEpochMillis)
    }

    @Test
    fun `completing final set of final exercise finishes session`() {
        val finalSet = WorkoutSessionState(
            currentExerciseIndex = workout.exercises.lastIndex,
            phase = WorkoutSessionPhase.EXERCISE,
            currentSetIndex = 2,
            completedSets = listOf(set(1, 10), set(2, 10)),
            currentSetDraft = reps(10),
        )

        val complete = WorkoutSessionController.completeCurrentSet(
            finalSet,
            workout,
            reps(10),
        )

        assertEquals(WorkoutSessionPhase.COMPLETE, complete.phase)
        assertFalse(complete.isRestTimerRunning)
        assertEquals(3, complete.completedSets.size)
    }

    @Test
    fun `undo reopens last set for editing`() {
        val resting = WorkoutSessionController.completeCurrentSet(
            WorkoutSessionController.start(workout, emptySet()),
            workout,
            reps(12),
        )

        val reopened = WorkoutSessionController.undoLastSet(resting)

        assertEquals(WorkoutSessionPhase.EXERCISE, reopened.phase)
        assertEquals(0, reopened.currentSetIndex)
        assertEquals(12, reopened.currentSetDraft?.reps)
        assertTrue(reopened.completedSets.isEmpty())
    }

    @Test
    fun `timed set countdown pauses at zero and keeps target duration`() {
        val timedWorkout = workout.copy(
            exercises = listOf(
                exercise(
                    id = "plank",
                    restSeconds = 30,
                    prescription = "2 × 2 sec",
                ),
            ),
        )
        val started = WorkoutSessionController.start(timedWorkout, emptySet())
        val running = WorkoutSessionController.toggleSetTimer(started, timedWorkout)

        val oneSecond = WorkoutSessionController.tickSetTimer(running)
        val finished = WorkoutSessionController.tickSetTimer(oneSecond)

        assertEquals(0, finished.setTimerSecondsRemaining)
        assertFalse(finished.isSetTimerRunning)
        assertEquals(2, finished.currentSetDraft?.durationSeconds)
    }

    @Test
    fun `valid interrupted set session restores drafts results and timer`() {
        val saved = WorkoutSessionState(
            currentExerciseIndex = 1,
            phase = WorkoutSessionPhase.REST,
            restSecondsRemaining = 27,
            isRestTimerRunning = true,
            currentSetIndex = 1,
            completedSets = listOf(set(1, 12)),
            currentSetDraft = reps(11),
            restKind = WorkoutRestKind.BETWEEN_SETS,
            sessionStartedAtEpochMillis = 500L,
        )

        val restored = WorkoutSessionController.restore(workout, saved)

        assertNotNull(restored)
        assertEquals(saved, restored)
    }

    @Test
    fun `invalid interrupted session is discarded safely`() {
        val saved = WorkoutSessionState(
            currentExerciseIndex = 99,
            phase = WorkoutSessionPhase.EXERCISE,
        )

        val restored = WorkoutSessionController.restore(workout, saved)

        assertEquals(null, restored)
    }

    private fun reps(value: Int) = ExerciseSetDraft(
        metricType = ExerciseMetricType.REPS,
        reps = value,
    )

    private fun set(number: Int, value: Int) = ExerciseSetResult(
        setNumber = number,
        metricType = ExerciseMetricType.REPS,
        reps = value,
    )

    private fun exercise(
        id: String,
        restSeconds: Int,
        prescription: String = "3 × 10",
    ) = Exercise(
        id = id,
        name = "Exercise $id",
        prescription = prescription,
        restSeconds = restSeconds,
        coachingCue = "Move with control.",
    )
}
