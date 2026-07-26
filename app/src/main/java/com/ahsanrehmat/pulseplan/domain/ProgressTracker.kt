package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import java.time.LocalDate

data class WorkoutHistoryDay(
    val date: LocalDate,
    val title: String,
    val isWorkoutDay: Boolean,
    val completedExercises: Int,
    val totalExercises: Int,
    val isComplete: Boolean,
    val isToday: Boolean,
    val exercises: List<WorkoutHistoryExercise>,
)

data class WorkoutHistoryExercise(
    val id: String,
    val name: String,
    val prescription: String,
    val isCompleted: Boolean,
    val resultSummary: String? = null,
)

data class WeeklyProgress(
    val weekStart: LocalDate,
    val completedWorkouts: Int,
    val plannedWorkouts: Int,
)

data class ProgressMilestone(
    val title: String,
    val description: String,
    val progressLabel: String,
    val progressFraction: Float,
    val isUnlocked: Boolean,
)

data class ProgressSnapshot(
    val history: List<WorkoutHistoryDay>,
    val calendarHistory: List<WorkoutHistoryDay>,
    val weeklyProgress: List<WeeklyProgress>,
    val milestones: List<ProgressMilestone>,
    val currentStreak: Int,
    val bestStreak: Int,
    val completedThisWeek: Int,
    val plannedThisWeek: Int,
    val completedInHistory: Int,
    val totalCompletedWorkouts: Int,
    val totalCompletedExercises: Int,
    val completionRate: Int,
) {
    companion object {
        val EMPTY = ProgressSnapshot(
            history = emptyList(),
            calendarHistory = emptyList(),
            weeklyProgress = emptyList(),
            milestones = emptyList(),
            currentStreak = 0,
            bestStreak = 0,
            completedThisWeek = 0,
            plannedThisWeek = 0,
            completedInHistory = 0,
            totalCompletedWorkouts = 0,
            totalCompletedExercises = 0,
            completionRate = 0,
        )
    }
}

object ProgressTracker {
    fun build(
        profile: UserFitnessProfile,
        today: LocalDate,
        completionHistory: Map<LocalDate, Set<String>>,
        resultHistory: Map<LocalDate, Map<String, ExerciseResult>> = emptyMap(),
        historyDays: Int = 28,
        streakLookbackDays: Int = 365,
    ): ProgressSnapshot {
        require(historyDays > 0) { "History must include at least one day." }
        require(streakLookbackDays >= historyDays) {
            "Streak lookback must include the visible history."
        }

        val calendarHistory = (0 until streakLookbackDays).map { offset ->
            historyDay(
                profile = profile,
                date = today.minusDays(offset.toLong()),
                today = today,
                completionHistory = completionHistory,
                resultHistory = resultHistory,
            )
        }
        val historyByDate = calendarHistory.associateBy(WorkoutHistoryDay::date)
        val visibleDays = calendarHistory.take(historyDays)
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekDays = (0 until 7).map { monday.plusDays(it.toLong()) }
        val completedThisWeek = weekDays.count { date ->
            date <= today && historyByDate[date]?.isComplete == true
        }
        val totalCompletedWorkouts = calendarHistory.count(WorkoutHistoryDay::isComplete)
        val totalCompletedExercises = calendarHistory.sumOf(
            WorkoutHistoryDay::completedExercises,
        )
        val dueVisibleWorkouts = visibleDays.filter { day ->
            day.isWorkoutDay && (day.date < today || day.isComplete)
        }
        val bestStreak = calculateBestStreak(calendarHistory)

        return ProgressSnapshot(
            history = visibleDays,
            calendarHistory = calendarHistory,
            weeklyProgress = weeklyProgress(
                profile = profile,
                today = today,
                historyByDate = historyByDate,
            ),
            milestones = milestones(
                completedWorkouts = totalCompletedWorkouts,
                completedExercises = totalCompletedExercises,
                bestStreak = bestStreak,
            ),
            currentStreak = calculateCurrentStreak(
                today = today,
                historyByDate = historyByDate,
            ),
            bestStreak = bestStreak,
            completedThisWeek = completedThisWeek,
            plannedThisWeek = weekDays.count { date ->
                PlanGenerator.isWorkoutDay(profile, date)
            },
            completedInHistory = visibleDays.count(WorkoutHistoryDay::isComplete),
            totalCompletedWorkouts = totalCompletedWorkouts,
            totalCompletedExercises = totalCompletedExercises,
            completionRate = if (dueVisibleWorkouts.isEmpty()) {
                0
            } else {
                visibleDays.count(WorkoutHistoryDay::isComplete) *
                    100 / dueVisibleWorkouts.size
            },
        )
    }

    private fun calculateCurrentStreak(
        today: LocalDate,
        historyByDate: Map<LocalDate, WorkoutHistoryDay>,
    ): Int {
        var streak = 0
        var date = today

        // Today's scheduled workout is still in progress, so it cannot break an
        // existing streak until the next calendar day.
        val todayEntry = historyByDate[today]
        if (todayEntry?.isWorkoutDay == true && !todayEntry.isComplete) {
            date = date.minusDays(1)
        }

        while (date in historyByDate) {
            val day = historyByDate.getValue(date)
            if (day.isWorkoutDay) {
                if (day.isComplete) {
                    streak++
                } else {
                    break
                }
            }
            date = date.minusDays(1)
        }
        return streak
    }

    private fun calculateBestStreak(
        calendarHistory: List<WorkoutHistoryDay>,
    ): Int {
        var best = 0
        var current = 0
        calendarHistory.asReversed().forEach { day ->
            if (day.isWorkoutDay) {
                if (day.isComplete) {
                    current++
                    best = maxOf(best, current)
                } else {
                    current = 0
                }
            }
        }
        return best
    }

    private fun weeklyProgress(
        profile: UserFitnessProfile,
        today: LocalDate,
        historyByDate: Map<LocalDate, WorkoutHistoryDay>,
        numberOfWeeks: Int = 6,
    ): List<WeeklyProgress> {
        val currentMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        return (numberOfWeeks - 1 downTo 0).map { weeksAgo ->
            val weekStart = currentMonday.minusWeeks(weeksAgo.toLong())
            val dates = (0 until 7).map { weekStart.plusDays(it.toLong()) }
            WeeklyProgress(
                weekStart = weekStart,
                completedWorkouts = dates.count { date ->
                    date <= today && historyByDate[date]?.isComplete == true
                },
                plannedWorkouts = dates.count { date ->
                    PlanGenerator.isWorkoutDay(profile, date)
                },
            )
        }
    }

    private fun milestones(
        completedWorkouts: Int,
        completedExercises: Int,
        bestStreak: Int,
    ): List<ProgressMilestone> = listOf(
        ProgressMilestone(
            title = "First workout",
            description = "Complete one scheduled workout",
            progressLabel = "${completedWorkouts.coerceAtMost(1)}/1",
            progressFraction = completedWorkouts.coerceAtMost(1).toFloat(),
            isUnlocked = completedWorkouts >= 1,
        ),
        ProgressMilestone(
            title = "Consistency builder",
            description = "Complete 3 scheduled workouts in a row",
            progressLabel = "${bestStreak.coerceAtMost(3)}/3",
            progressFraction = bestStreak.coerceAtMost(3) / 3f,
            isUnlocked = bestStreak >= 3,
        ),
        ProgressMilestone(
            title = "Ten workouts",
            description = "Complete 10 scheduled workouts",
            progressLabel = "${completedWorkouts.coerceAtMost(10)}/10",
            progressFraction = completedWorkouts.coerceAtMost(10) / 10f,
            isUnlocked = completedWorkouts >= 10,
        ),
        ProgressMilestone(
            title = "Fifty exercises",
            description = "Complete 50 individual exercises",
            progressLabel = "${completedExercises.coerceAtMost(50)}/50",
            progressFraction = completedExercises.coerceAtMost(50) / 50f,
            isUnlocked = completedExercises >= 50,
        ),
    )

    private fun historyDay(
        profile: UserFitnessProfile,
        date: LocalDate,
        today: LocalDate,
        completionHistory: Map<LocalDate, Set<String>>,
        resultHistory: Map<LocalDate, Map<String, ExerciseResult>>,
    ): WorkoutHistoryDay {
        val workout = PlanGenerator.workoutFor(profile, date)
        val completedIds = completionHistory[date].orEmpty()
        val completedCount = workout.exercises.count {
            it.sourceExerciseId in completedIds
        }
        val isWorkoutDay = PlanGenerator.isWorkoutDay(profile, date)
        val results = resultHistory[date].orEmpty()

        return WorkoutHistoryDay(
            date = date,
            title = workout.title,
            isWorkoutDay = isWorkoutDay,
            completedExercises = completedCount,
            totalExercises = workout.exercises.size,
            isComplete = isWorkoutDay &&
                workout.exercises.isNotEmpty() &&
                completedCount == workout.exercises.size,
            isToday = date == today,
            exercises = workout.exercises.map { exercise ->
                WorkoutHistoryExercise(
                    id = exercise.sourceExerciseId,
                    name = exercise.name,
                    prescription = exercise.prescription,
                    isCompleted = exercise.sourceExerciseId in completedIds,
                    resultSummary = results[exercise.sourceExerciseId]
                        ?.let(PerformanceTracker::resultLabel),
                )
            },
        )
    }
}
