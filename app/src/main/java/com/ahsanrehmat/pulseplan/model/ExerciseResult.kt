package com.ahsanrehmat.pulseplan.model

enum class ExerciseMetricType(val label: String) {
    REPS("Reps"),
    TIME("Time"),
    DISTANCE("Distance"),
}

enum class ExerciseEffort(val label: String) {
    EASY("Easy"),
    GOOD("Good"),
    HARD("Hard"),
}

data class ExerciseResultDraft(
    val metricType: ExerciseMetricType,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val effort: ExerciseEffort = ExerciseEffort.GOOD,
    val notes: String = "",
    val sets: List<ExerciseSetResult> = emptyList(),
)

data class ExerciseSetDraft(
    val metricType: ExerciseMetricType,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
) {
    fun hasMeasurableResult(): Boolean = when (metricType) {
        ExerciseMetricType.REPS -> reps != null || weightKg != null
        ExerciseMetricType.TIME -> durationSeconds != null
        ExerciseMetricType.DISTANCE -> distanceKm != null
    }
}

data class ExerciseSetResult(
    val setNumber: Int,
    val metricType: ExerciseMetricType,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
) {
    fun asDraft(): ExerciseSetDraft = ExerciseSetDraft(
        metricType = metricType,
        reps = reps,
        weightKg = weightKg,
        durationSeconds = durationSeconds,
        distanceKm = distanceKm,
    )

    fun hasMeasurableResult(): Boolean = asDraft().hasMeasurableResult()
}

data class ExerciseResult(
    val exerciseId: String,
    val exerciseName: String,
    val prescription: String,
    val metricType: ExerciseMetricType,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceKm: Double? = null,
    val effort: ExerciseEffort = ExerciseEffort.GOOD,
    val notes: String = "",
    val loggedAtEpochMillis: Long,
    val sets: List<ExerciseSetResult> = emptyList(),
) {
    fun hasMeasurableResult(): Boolean = when (metricType) {
        ExerciseMetricType.REPS -> reps != null || weightKg != null
        ExerciseMetricType.TIME -> durationSeconds != null
        ExerciseMetricType.DISTANCE -> distanceKm != null
    }
}
