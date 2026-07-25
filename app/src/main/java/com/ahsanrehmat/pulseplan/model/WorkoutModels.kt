package com.ahsanrehmat.pulseplan.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class FitnessGoal(val label: String) {
    GENERAL_FITNESS("Feel fitter"),
    BUILD_STRENGTH("Build strength"),
    LOSE_WEIGHT("Lose weight"),
    IMPROVE_MOBILITY("Move better"),
}

enum class ExperienceLevel(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
}

enum class WorkoutPersonality(val label: String, val description: String) {
    STEADY("Steady", "A predictable routine that builds consistency"),
    VARIETY("Variety", "Fresh exercise combinations to keep things interesting"),
    CHALLENGE("Challenge", "Clear targets and progressive difficulty"),
    GUIDED("Guided", "Simple instructions and an encouraging pace"),
}

enum class Equipment(val label: String) {
    BODYWEIGHT("Bodyweight"),
    DUMBBELLS("Dumbbells"),
    FULL_GYM("Full gym"),
}

enum class MovementPreference(
    val label: String,
    val description: String,
) {
    GENTLE_TRANSITIONS(
        "Gentle transitions",
        "Avoid quick changes between standing and lowered positions",
    ),
    NO_FLOOR_EXERCISES(
        "No floor exercises",
        "Keep the session standing, seated, or supported",
    ),
    LIMIT_OVERHEAD(
        "Limit overhead movements",
        "Keep reaching and resistance below an overhead range",
    ),
    LIMIT_WRIST_LOADING(
        "Limit wrist loading",
        "Avoid supporting body weight through the hands",
    ),
    NO_KNEELING(
        "No kneeling",
        "Avoid positions that place a knee on the floor",
    ),
    EXTRA_BALANCE_SUPPORT(
        "Extra balance support",
        "Prefer a wall, chair, or rail for standing movements",
    ),
}

data class UserFitnessProfile(
    val displayName: String,
    val goal: FitnessGoal,
    val experience: ExperienceLevel,
    val personality: WorkoutPersonality,
    val equipment: Equipment,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
    val movementNotes: String = "",
    val movementPreferences: Set<MovementPreference> = emptySet(),
)

data class Exercise(
    val id: String,
    val name: String,
    val prescription: String,
    val restSeconds: Int,
    val coachingCue: String,
    val sourceExerciseId: String = id,
    val adjustmentReason: String? = null,
)

data class DailyWorkout(
    val date: LocalDate,
    val title: String,
    val focus: String,
    val estimatedMinutes: Int,
    val exercises: List<Exercise>,
)

data class WeekDayPlan(
    val shortDay: String,
    val focus: String,
    val isWorkoutDay: Boolean,
    val isToday: Boolean,
)

data class ReminderTime(
    val hour: Int,
    val minute: Int,
) {
    init {
        require(hour in 0..23) { "Reminder hour must be between 0 and 23." }
        require(minute in 0..59) { "Reminder minute must be between 0 and 59." }
    }

    fun label(): String = LocalTime.of(hour, minute).format(DISPLAY_FORMAT)

    companion object {
        val DEFAULT = ReminderTime(hour = 7, minute = 0)

        private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("h:mm a")
    }
}
