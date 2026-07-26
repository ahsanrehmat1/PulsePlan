package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class DatedExerciseResult(
    val date: LocalDate,
    val result: ExerciseResult,
    val isPersonalBest: Boolean,
)

data class ExercisePerformanceSummary(
    val exerciseId: String,
    val exerciseName: String,
    val latest: DatedExerciseResult,
    val personalBest: DatedExerciseResult?,
    val history: List<DatedExerciseResult>,
)

data class PerformanceSnapshot(
    val totalLoggedResults: Int,
    val personalBestMoments: Int,
    val exercisesTracked: Int,
    val exercises: List<ExercisePerformanceSummary>,
    val recentResults: List<DatedExerciseResult>,
) {
    fun summaryFor(exerciseId: String): ExercisePerformanceSummary? =
        exercises.firstOrNull { it.exerciseId == exerciseId }

    companion object {
        val EMPTY = PerformanceSnapshot(
            totalLoggedResults = 0,
            personalBestMoments = 0,
            exercisesTracked = 0,
            exercises = emptyList(),
            recentResults = emptyList(),
        )
    }
}

object PerformanceTracker {
    fun build(
        resultHistory: Map<LocalDate, Map<String, ExerciseResult>>,
    ): PerformanceSnapshot {
        val chronological = resultHistory
            .flatMap { (date, results) ->
                results.values.map { result -> date to result }
            }
            .sortedWith(
                compareBy<Pair<LocalDate, ExerciseResult>> { it.first }
                    .thenBy { it.second.loggedAtEpochMillis },
            )

        val marked = mutableListOf<DatedExerciseResult>()
        val bestByExerciseMetric =
            mutableMapOf<Pair<String, ExerciseMetricType>, ExerciseResult>()
        chronological.forEach { (date, result) ->
            val key = result.exerciseId to result.metricType
            val previousBest = bestByExerciseMetric[key]
            val isBest = result.hasMeasurableResult() &&
                (previousBest == null || isBetter(result, previousBest))
            if (isBest) {
                bestByExerciseMetric[key] = result
            }
            marked += DatedExerciseResult(
                date = date,
                result = result,
                isPersonalBest = isBest,
            )
        }

        val summaries = marked
            .groupBy { it.result.exerciseId }
            .map { (exerciseId, entries) ->
                val newestFirst = entries.sortedWith(
                    compareByDescending<DatedExerciseResult> { it.date }
                        .thenByDescending { it.result.loggedAtEpochMillis },
                )
                ExercisePerformanceSummary(
                    exerciseId = exerciseId,
                    exerciseName = newestFirst.first().result.exerciseName,
                    latest = newestFirst.first(),
                    personalBest = newestFirst.firstOrNull { candidate ->
                        candidate.result == bestByExerciseMetric[
                            exerciseId to newestFirst.first().result.metricType
                        ]
                    },
                    history = newestFirst,
                )
            }
            .sortedByDescending { it.latest.result.loggedAtEpochMillis }

        val recent = marked.sortedWith(
            compareByDescending<DatedExerciseResult> { it.date }
                .thenByDescending { it.result.loggedAtEpochMillis },
        )
        return PerformanceSnapshot(
            totalLoggedResults = marked.size,
            personalBestMoments = marked.count(DatedExerciseResult::isPersonalBest),
            exercisesTracked = summaries.size,
            exercises = summaries,
            recentResults = recent.take(12),
        )
    }

    fun wouldBePersonalBest(
        candidate: ExerciseResult,
        existing: List<ExerciseResult>,
    ): Boolean {
        if (!candidate.hasMeasurableResult()) return false
        val comparable = existing.filter {
            it.exerciseId == candidate.exerciseId &&
                it.metricType == candidate.metricType &&
                it.hasMeasurableResult()
        }
        val best = comparable.maxWithOrNull { first, second ->
            compareResults(first, second)
        }
        return best == null || isBetter(candidate, best)
    }

    fun isBetter(candidate: ExerciseResult, baseline: ExerciseResult): Boolean {
        if (candidate.metricType != baseline.metricType) return false
        return compareResults(candidate, baseline) > 0
    }

    fun resultLabel(result: ExerciseResult): String {
        val measurement = when (result.metricType) {
        ExerciseMetricType.REPS -> buildList {
            result.reps?.let { add("$it reps") }
            result.weightKg?.let { add("${formatDecimal(it)} kg") }
        }.ifEmpty { listOf("Effort logged") }.joinToString(" at ")

        ExerciseMetricType.TIME -> result.durationSeconds
            ?.let(::formatDuration)
            ?: "Effort logged"

        ExerciseMetricType.DISTANCE -> buildList {
            result.distanceKm?.let { add("${formatDecimal(it)} km") }
            result.durationSeconds?.let { add(formatDuration(it)) }
        }.ifEmpty { listOf("Effort logged") }.joinToString(" in ")
        }
        return if (result.sets.size > 1) {
            "$measurement · ${result.sets.size} sets"
        } else {
            measurement
        }
    }

    fun setLabel(set: ExerciseSetResult): String = when (set.metricType) {
        ExerciseMetricType.REPS -> buildList {
            set.reps?.let { add("$it reps") }
            set.weightKg?.let { add("${formatDecimal(it)} kg") }
        }.ifEmpty { listOf("Completed") }.joinToString(" at ")

        ExerciseMetricType.TIME -> set.durationSeconds
            ?.let(::formatDuration)
            ?: "Completed"

        ExerciseMetricType.DISTANCE -> buildList {
            set.distanceKm?.let { add("${formatDecimal(it)} km") }
            set.durationSeconds?.let { add(formatDuration(it)) }
        }.ifEmpty { listOf("Completed") }.joinToString(" in ")
    }

    fun totalReps(result: ExerciseResult): Int =
        result.sets.sumOf { it.reps ?: 0 }.takeIf { it > 0 }
            ?: result.reps
            ?: 0

    fun totalVolumeKg(result: ExerciseResult): Double =
        result.sets.sumOf { set ->
            (set.reps ?: 0) * (set.weightKg ?: 0.0)
        }

    fun totalTimedSeconds(result: ExerciseResult): Int =
        result.sets.sumOf { it.durationSeconds ?: 0 }.takeIf { it > 0 }
            ?: result.durationSeconds
            ?: 0

    fun totalDistanceKm(result: ExerciseResult): Double =
        result.sets.sumOf { it.distanceKm ?: 0.0 }.takeIf { it > 0.0 }
            ?: result.distanceKm
            ?: 0.0

    fun shortDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d"))

    fun chartValue(result: ExerciseResult): Float = when (result.metricType) {
        ExerciseMetricType.REPS -> (result.weightKg ?: result.reps?.toDouble() ?: 0.0).toFloat()
        ExerciseMetricType.TIME -> (result.durationSeconds ?: 0).toFloat()
        ExerciseMetricType.DISTANCE -> (result.distanceKm ?: 0.0).toFloat()
    }

    private fun compareResults(first: ExerciseResult, second: ExerciseResult): Int =
        when (first.metricType) {
            ExerciseMetricType.REPS -> {
                val firstWeight = first.weightKg ?: 0.0
                val secondWeight = second.weightKg ?: 0.0
                if (firstWeight.compareTo(secondWeight) != 0) {
                    firstWeight.compareTo(secondWeight)
                } else {
                    (first.reps ?: 0).compareTo(second.reps ?: 0)
                }
            }

            ExerciseMetricType.TIME ->
                (first.durationSeconds ?: 0).compareTo(second.durationSeconds ?: 0)

            ExerciseMetricType.DISTANCE -> {
                val distanceComparison =
                    (first.distanceKm ?: 0.0).compareTo(second.distanceKm ?: 0.0)
                if (distanceComparison != 0) {
                    distanceComparison
                } else {
                    (first.durationSeconds ?: 0).compareTo(second.durationSeconds ?: 0)
                }
            }
        }

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
}
