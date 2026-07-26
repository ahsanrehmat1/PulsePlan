package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PerformanceTrackerTest {
    @Test
    fun `tracker marks improving results and exposes latest and best`() {
        val firstDate = LocalDate.of(2026, 7, 20)
        val secondDate = LocalDate.of(2026, 7, 22)
        val thirdDate = LocalDate.of(2026, 7, 24)
        val history = mapOf(
            firstDate to mapOf("squat" to repsResult(reps = 8, loggedAt = 100L)),
            secondDate to mapOf("squat" to repsResult(reps = 12, loggedAt = 200L)),
            thirdDate to mapOf("squat" to repsResult(reps = 10, loggedAt = 300L)),
        )

        val snapshot = PerformanceTracker.build(history)
        val summary = snapshot.summaryFor("squat")

        assertEquals(3, snapshot.totalLoggedResults)
        assertEquals(2, snapshot.personalBestMoments)
        assertEquals(1, snapshot.exercisesTracked)
        assertEquals(10, summary?.latest?.result?.reps)
        assertEquals(12, summary?.personalBest?.result?.reps)
        assertEquals(listOf(false, true, true), summary?.history?.map { it.isPersonalBest })
    }

    @Test
    fun `weighted rep result compares weight before reps`() {
        val baseline = repsResult(reps = 12, weightKg = 10.0)
        val heavier = repsResult(reps = 8, weightKg = 12.0)
        val lighter = repsResult(reps = 20, weightKg = 8.0)

        assertTrue(PerformanceTracker.isBetter(heavier, baseline))
        assertFalse(PerformanceTracker.isBetter(lighter, baseline))
    }

    @Test
    fun `first measurable result for each metric is a personal best`() {
        val reps = repsResult(reps = 10)
        val timed = ExerciseResult(
            exerciseId = "squat",
            exerciseName = "Squat",
            prescription = "30 sec",
            metricType = ExerciseMetricType.TIME,
            durationSeconds = 30,
            effort = ExerciseEffort.HARD,
            loggedAtEpochMillis = 200L,
        )

        assertTrue(PerformanceTracker.wouldBePersonalBest(timed, listOf(reps)))
    }

    @Test
    fun `effort only entry is logged but not called a personal best`() {
        val result = repsResult(reps = null)
        val snapshot = PerformanceTracker.build(
            mapOf(LocalDate.of(2026, 7, 25) to mapOf("squat" to result)),
        )

        assertEquals(1, snapshot.totalLoggedResults)
        assertEquals(0, snapshot.personalBestMoments)
        assertFalse(snapshot.recentResults.single().isPersonalBest)
        assertEquals("Effort logged", PerformanceTracker.resultLabel(result))
    }

    @Test
    fun `set results expose breakdown label and workout totals`() {
        val result = repsResult(reps = 12, weightKg = 10.0).copy(
            sets = listOf(
                ExerciseSetResult(
                    setNumber = 1,
                    metricType = ExerciseMetricType.REPS,
                    reps = 12,
                    weightKg = 10.0,
                ),
                ExerciseSetResult(
                    setNumber = 2,
                    metricType = ExerciseMetricType.REPS,
                    reps = 10,
                    weightKg = 10.0,
                ),
            ),
        )

        assertEquals("12 reps at 10 kg · 2 sets", PerformanceTracker.resultLabel(result))
        assertEquals("10 reps at 10 kg", PerformanceTracker.setLabel(result.sets[1]))
        assertEquals(22, PerformanceTracker.totalReps(result))
        assertEquals(220.0, PerformanceTracker.totalVolumeKg(result), 0.001)
    }

    private fun repsResult(
        reps: Int?,
        weightKg: Double? = null,
        loggedAt: Long = 100L,
    ) = ExerciseResult(
        exerciseId = "squat",
        exerciseName = "Squat",
        prescription = "3 x 10",
        metricType = ExerciseMetricType.REPS,
        reps = reps,
        weightKg = weightKg,
        effort = ExerciseEffort.GOOD,
        loggedAtEpochMillis = loggedAt,
    )
}
