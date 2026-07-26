package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPrescriptionTest {
    @Test
    fun `repetition prescription returns set and rep targets`() {
        val parsed = WorkoutPrescriptionParser.parse("3 × 10 each")

        assertEquals(3, parsed.setCount)
        assertEquals(ExerciseMetricType.REPS, parsed.metricType)
        assertEquals(10, parsed.targetReps)
    }

    @Test
    fun `seconds prescription creates a timed target`() {
        val parsed = WorkoutPrescriptionParser.parse("2 × 45 sec")

        assertEquals(2, parsed.setCount)
        assertEquals(ExerciseMetricType.TIME, parsed.metricType)
        assertEquals(45, parsed.targetSeconds)
    }

    @Test
    fun `minute prescription converts decimal minutes to seconds`() {
        val parsed = WorkoutPrescriptionParser.parse("4 x 1.5 min")

        assertEquals(4, parsed.setCount)
        assertEquals(90, parsed.targetSeconds)
    }

    @Test
    fun `unknown prescription falls back safely to one rep set`() {
        val parsed = WorkoutPrescriptionParser.parse("Move for comfort")

        assertEquals(1, parsed.setCount)
        assertEquals(ExerciseMetricType.REPS, parsed.metricType)
        assertEquals(null, parsed.targetReps)
    }
}
