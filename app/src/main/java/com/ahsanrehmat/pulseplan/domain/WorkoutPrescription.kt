package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.Exercise
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseSetDraft

data class WorkoutPrescription(
    val setCount: Int,
    val metricType: ExerciseMetricType,
    val targetReps: Int? = null,
    val targetSeconds: Int? = null,
    val targetDistanceKm: Double? = null,
)

object WorkoutPrescriptionParser {
    private val prescriptionPattern = Regex(
        pattern = """^\s*(\d+)\s*[×xX]\s*(\d+(?:\.\d+)?)\s*(sec|secs|second|seconds|min|mins|minute|minutes|km)?""",
        option = RegexOption.IGNORE_CASE,
    )

    fun parse(exercise: Exercise): WorkoutPrescription = parse(exercise.prescription)

    fun parse(prescription: String): WorkoutPrescription {
        val match = prescriptionPattern.find(prescription)
            ?: return WorkoutPrescription(
                setCount = 1,
                metricType = ExerciseMetricType.REPS,
            )
        val setCount = match.groupValues[1].toIntOrNull()
            ?.coerceIn(1, MAX_SET_COUNT)
            ?: 1
        val target = match.groupValues[2].toDoubleOrNull()
        val unit = match.groupValues[3].lowercase()
        return when {
            unit.startsWith("sec") || unit.startsWith("second") ->
                WorkoutPrescription(
                    setCount = setCount,
                    metricType = ExerciseMetricType.TIME,
                    targetSeconds = target?.toInt()?.coerceAtLeast(1),
                )

            unit.startsWith("min") || unit.startsWith("minute") ->
                WorkoutPrescription(
                    setCount = setCount,
                    metricType = ExerciseMetricType.TIME,
                    targetSeconds = target
                        ?.times(60)
                        ?.toInt()
                        ?.coerceAtLeast(1),
                )

            unit == "km" ->
                WorkoutPrescription(
                    setCount = setCount,
                    metricType = ExerciseMetricType.DISTANCE,
                    targetDistanceKm = target?.takeIf { it > 0.0 },
                )

            else ->
                WorkoutPrescription(
                    setCount = setCount,
                    metricType = ExerciseMetricType.REPS,
                    targetReps = target?.toInt()?.coerceAtLeast(1),
                )
        }
    }

    fun plannedDraft(exercise: Exercise): ExerciseSetDraft {
        val parsed = parse(exercise)
        return ExerciseSetDraft(
            metricType = parsed.metricType,
            reps = parsed.targetReps,
            durationSeconds = parsed.targetSeconds,
            distanceKm = parsed.targetDistanceKm,
        )
    }

    private const val MAX_SET_COUNT = 8
}
