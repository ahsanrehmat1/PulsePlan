package com.ahsanrehmat.pulseplan.domain

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
)

data class ProgressSnapshot(
    val history: List<WorkoutHistoryDay>,
    val currentStreak: Int,
    val completedThisWeek: Int,
    val plannedThisWeek: Int,
    val completedInHistory: Int,
) {
    companion object {
        val EMPTY = ProgressSnapshot(
            history = emptyList(),
            currentStreak = 0,
            completedThisWeek = 0,
            plannedThisWeek = 0,
            completedInHistory = 0,
        )
    }
}

object ProgressTracker {
    fun build(
        profile: UserFitnessProfile,
        today: LocalDate,
        completionHistory: Map<LocalDate, Set<String>>,
        historyDays: Int = 28,
        streakLookbackDays: Int = 365,
    ): ProgressSnapshot {
        require(historyDays > 0) { "History must include at least one day." }
        require(streakLookbackDays >= historyDays) {
            "Streak lookback must include the visible history."
        }

        val visibleDays = (0 until historyDays).map { offset ->
            historyDay(
                profile = profile,
                date = today.minusDays(offset.toLong()),
                today = today,
                completionHistory = completionHistory,
            )
        }
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekDays = (0 until 7).map { monday.plusDays(it.toLong()) }

        return ProgressSnapshot(
            history = visibleDays,
            currentStreak = calculateCurrentStreak(
                profile = profile,
                today = today,
                completionHistory = completionHistory,
                streakLookbackDays = streakLookbackDays,
            ),
            completedThisWeek = weekDays.count { date ->
                date <= today && historyDay(profile, date, today, completionHistory).isComplete
            },
            plannedThisWeek = weekDays.count { date ->
                PlanGenerator.isWorkoutDay(profile, date)
            },
            completedInHistory = visibleDays.count(WorkoutHistoryDay::isComplete),
        )
    }

    private fun calculateCurrentStreak(
        profile: UserFitnessProfile,
        today: LocalDate,
        completionHistory: Map<LocalDate, Set<String>>,
        streakLookbackDays: Int,
    ): Int {
        var streak = 0
        var date = today
        var inspectedDays = 0

        // Today's scheduled workout is still in progress, so it cannot break an
        // existing streak until the next calendar day.
        if (
            PlanGenerator.isWorkoutDay(profile, today) &&
            !historyDay(profile, today, today, completionHistory).isComplete
        ) {
            date = date.minusDays(1)
            inspectedDays++
        }

        while (inspectedDays < streakLookbackDays) {
            if (PlanGenerator.isWorkoutDay(profile, date)) {
                if (historyDay(profile, date, today, completionHistory).isComplete) {
                    streak++
                } else {
                    break
                }
            }
            date = date.minusDays(1)
            inspectedDays++
        }
        return streak
    }

    private fun historyDay(
        profile: UserFitnessProfile,
        date: LocalDate,
        today: LocalDate,
        completionHistory: Map<LocalDate, Set<String>>,
    ): WorkoutHistoryDay {
        val workout = PlanGenerator.workoutFor(profile, date)
        val completedIds = completionHistory[date].orEmpty()
        val completedCount = workout.exercises.count {
            it.sourceExerciseId in completedIds
        }
        val isWorkoutDay = PlanGenerator.isWorkoutDay(profile, date)

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
        )
    }
}
