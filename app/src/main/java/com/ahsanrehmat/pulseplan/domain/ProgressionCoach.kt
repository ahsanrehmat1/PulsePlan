package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

data class ProgressionSuggestion(
    val metricType: ExerciseMetricType,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val title: String,
    val targetLabel: String,
    val explanation: String,
)

object ProgressionCoach {
    fun suggest(
        goal: FitnessGoal,
        previous: ExerciseResult?,
    ): ProgressionSuggestion? {
        if (previous == null || !previous.hasMeasurableResult()) return null
        val target = when (previous.effort) {
            ExerciseEffort.EASY -> progressTarget(goal, previous)
            ExerciseEffort.GOOD -> repeatTarget(previous)
            ExerciseEffort.HARD -> reduceTarget(previous)
        }
        return target.copy(targetLabel = targetLabel(target))
    }

    private fun progressTarget(
        goal: FitnessGoal,
        previous: ExerciseResult,
    ): ProgressionSuggestion = when (previous.metricType) {
        ExerciseMetricType.REPS -> {
            val shouldProgressWeight =
                previous.weightKg != null && goal == FitnessGoal.BUILD_STRENGTH
            val nextWeight = if (shouldProgressWeight) {
                val currentWeight = previous.weightKg
                val increase = (currentWeight * 0.05)
                    .coerceIn(0.1, 2.5)
                if (currentWeight < 5.0) {
                    roundToTenth(currentWeight + increase)
                } else {
                    roundToHalf(currentWeight + increase)
                }
            } else {
                previous.weightKg
            }
            val nextReps = if (shouldProgressWeight) {
                previous.reps
            } else {
                previous.reps?.plus(1)
            }
            baseSuggestion(
                previous = previous,
                reps = nextReps,
                weightKg = nextWeight,
                title = "Build gently",
                explanation = if (shouldProgressWeight) {
                    "Your last result felt easy. A small load increase supports your " +
                        "strength goal without changing two targets at once."
                } else {
                    "Your last result felt easy. One extra controlled rep is a small, " +
                        "clear next step for ${goal.label.lowercase()}."
                },
            )
        }

        ExerciseMetricType.TIME -> baseSuggestion(
            previous = previous,
            durationSeconds = previous.durationSeconds?.plus(5),
            title = "Add a little time",
            explanation = "Your last result felt easy. Five extra seconds keeps the " +
                "progression small and easy to understand.",
        )

        ExerciseMetricType.DISTANCE -> {
            val currentDistance = previous.distanceKm ?: 0.0
            val increase = max(0.05, currentDistance * 0.05)
            baseSuggestion(
                previous = previous,
                distanceKm = roundToHundredth(currentDistance + increase),
                title = "Go a little farther",
                explanation = "Your last result felt easy. The next distance is only a " +
                    "small step up, and no faster time is required.",
            )
        }
    }

    private fun repeatTarget(previous: ExerciseResult): ProgressionSuggestion =
        baseSuggestion(
            previous = previous,
            reps = previous.reps,
            weightKg = previous.weightKg,
            durationSeconds = previous.durationSeconds,
            distanceKm = previous.distanceKm,
            title = "Repeat with control",
            explanation = "Your last effort felt right. Repeating the same target helps " +
                "confirm consistency before increasing it.",
        )

    private fun reduceTarget(previous: ExerciseResult): ProgressionSuggestion =
        when (previous.metricType) {
            ExerciseMetricType.REPS -> {
                if (previous.weightKg != null) {
                    baseSuggestion(
                        previous = previous,
                        reps = previous.reps,
                        weightKg = if (previous.weightKg < 5.0) {
                            roundToTenth(
                                (previous.weightKg * 0.95).coerceAtLeast(0.1),
                            )
                        } else {
                            roundDownToHalf(
                                (previous.weightKg * 0.95).coerceAtLeast(0.5),
                            )
                        },
                        title = "Ease back",
                        explanation = "Your last effort felt hard. A small load reduction " +
                            "keeps the target realistic while preserving the same movement.",
                    )
                } else {
                    baseSuggestion(
                        previous = previous,
                        reps = previous.reps?.minus(1)?.coerceAtLeast(1),
                        title = "Ease back",
                        explanation = "Your last effort felt hard. One fewer rep is a small " +
                            "adjustment, not a setback.",
                    )
                }
            }

            ExerciseMetricType.TIME -> baseSuggestion(
                previous = previous,
                durationSeconds = previous.durationSeconds
                    ?.minus(5)
                    ?.coerceAtLeast(5),
                title = "Ease back",
                explanation = "Your last effort felt hard. Five fewer seconds makes the next " +
                    "target more manageable.",
            )

            ExerciseMetricType.DISTANCE -> {
                val currentDistance = previous.distanceKm ?: 0.1
                baseSuggestion(
                    previous = previous,
                    distanceKm = roundToHundredth(
                        (currentDistance * 0.95).coerceAtLeast(0.05),
                    ),
                    title = "Ease back",
                    explanation = "Your last effort felt hard. A slightly shorter distance " +
                        "keeps the next target achievable.",
                )
            }
        }

    private fun baseSuggestion(
        previous: ExerciseResult,
        reps: Int? = null,
        weightKg: Double? = null,
        durationSeconds: Int? = null,
        distanceKm: Double? = null,
        title: String,
        explanation: String,
    ) = ProgressionSuggestion(
        metricType = previous.metricType,
        reps = reps,
        weightKg = weightKg,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
        title = title,
        targetLabel = "",
        explanation = explanation,
    )

    private fun targetLabel(target: ProgressionSuggestion): String = when (target.metricType) {
        ExerciseMetricType.REPS -> buildList {
            target.reps?.let { add("$it reps") }
            target.weightKg?.let { add("${formatDecimal(it)} kg") }
        }.joinToString(" at ")

        ExerciseMetricType.TIME -> target.durationSeconds
            ?.let(::formatDuration)
            .orEmpty()

        ExerciseMetricType.DISTANCE -> target.distanceKm
            ?.let { "${formatDistance(it)} km" }
            .orEmpty()
    }

    private fun roundToHalf(value: Double): Double = ceil(value * 2.0) / 2.0

    private fun roundDownToHalf(value: Double): Double = floor(value * 2.0) / 2.0

    private fun roundToTenth(value: Double): Double =
        (value * 10.0).roundToInt() / 10.0

    private fun roundToHundredth(value: Double): Double =
        (value * 100.0).roundToInt() / 100.0

    private fun formatDuration(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return when {
            minutes == 0 -> "$seconds sec"
            seconds == 0 -> "$minutes min"
            else -> "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    }

    private fun formatDecimal(value: Double): String =
        if (value == value.roundToInt().toDouble()) {
            value.roundToInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.1f", value)
        }

    private fun formatDistance(value: Double): String =
        String.format(java.util.Locale.US, "%.2f", value)
            .trimEnd('0')
            .trimEnd('.')
}
