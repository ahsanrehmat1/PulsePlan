package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionCoachTest {
    @Test
    fun `easy weighted result nudges load for a strength goal`() {
        val previous = result(
            metricType = ExerciseMetricType.REPS,
            effort = ExerciseEffort.EASY,
            reps = 8,
            weightKg = 10.0,
        )

        val suggestion = ProgressionCoach.suggest(FitnessGoal.BUILD_STRENGTH, previous)

        assertEquals(8, suggestion?.reps)
        assertEquals(10.5, suggestion?.weightKg)
        assertEquals("8 reps at 10.5 kg", suggestion?.targetLabel)
        assertTrue(suggestion?.explanation.orEmpty().contains("strength goal"))
    }

    @Test
    fun `easy bodyweight result adds only one controlled rep`() {
        val previous = result(
            metricType = ExerciseMetricType.REPS,
            effort = ExerciseEffort.EASY,
            reps = 12,
        )

        val suggestion = ProgressionCoach.suggest(FitnessGoal.GENERAL_FITNESS, previous)

        assertEquals(13, suggestion?.reps)
        assertEquals("13 reps", suggestion?.targetLabel)
        assertEquals("Build gently", suggestion?.title)
    }

    @Test
    fun `good result repeats the same target`() {
        val previous = result(
            metricType = ExerciseMetricType.TIME,
            effort = ExerciseEffort.GOOD,
            durationSeconds = 45,
        )

        val suggestion = ProgressionCoach.suggest(FitnessGoal.IMPROVE_MOBILITY, previous)

        assertEquals(45, suggestion?.durationSeconds)
        assertEquals("45 sec", suggestion?.targetLabel)
        assertEquals("Repeat with control", suggestion?.title)
    }

    @Test
    fun `hard result makes a small bounded reduction`() {
        val reps = result(
            metricType = ExerciseMetricType.REPS,
            effort = ExerciseEffort.HARD,
            reps = 1,
        )
        val distance = result(
            metricType = ExerciseMetricType.DISTANCE,
            effort = ExerciseEffort.HARD,
            distanceKm = 2.0,
        )

        val repsSuggestion = ProgressionCoach.suggest(FitnessGoal.GENERAL_FITNESS, reps)
        val distanceSuggestion = ProgressionCoach.suggest(FitnessGoal.LOSE_WEIGHT, distance)

        assertEquals(1, repsSuggestion?.reps)
        assertEquals(1.9, distanceSuggestion?.distanceKm)
        assertEquals("Ease back", distanceSuggestion?.title)
    }

    @Test
    fun `short easy distance increases by only fifty meters`() {
        val previous = result(
            metricType = ExerciseMetricType.DISTANCE,
            effort = ExerciseEffort.EASY,
            distanceKm = 0.1,
        )

        val suggestion = ProgressionCoach.suggest(FitnessGoal.GENERAL_FITNESS, previous)

        assertEquals(0.15, suggestion?.distanceKm)
        assertEquals("0.15 km", suggestion?.targetLabel)
    }

    @Test
    fun `effort without a measurable result does not invent a target`() {
        val previous = result(
            metricType = ExerciseMetricType.REPS,
            effort = ExerciseEffort.EASY,
        )

        assertNull(ProgressionCoach.suggest(FitnessGoal.BUILD_STRENGTH, previous))
    }

    private fun result(
        metricType: ExerciseMetricType,
        effort: ExerciseEffort,
        reps: Int? = null,
        weightKg: Double? = null,
        durationSeconds: Int? = null,
        distanceKm: Double? = null,
    ) = ExerciseResult(
        exerciseId = "test",
        exerciseName = "Test exercise",
        prescription = "Test target",
        metricType = metricType,
        reps = reps,
        weightKg = weightKg,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        effort = effort,
        loggedAtEpochMillis = 100L,
    )
}
