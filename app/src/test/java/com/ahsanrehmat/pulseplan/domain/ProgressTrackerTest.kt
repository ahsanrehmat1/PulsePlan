package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.ExperienceLevel
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.model.MovementPreference
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WorkoutPersonality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgressTrackerTest {
    private val profile = UserFitnessProfile(
        displayName = "Ahsan",
        goal = FitnessGoal.BUILD_STRENGTH,
        experience = ExperienceLevel.BEGINNER,
        personality = WorkoutPersonality.STEADY,
        equipment = Equipment.DUMBBELLS,
        daysPerWeek = 3,
        sessionMinutes = 30,
    )

    @Test
    fun `unfinished workout today does not erase an existing streak`() {
        val today = LocalDate.of(2026, 7, 24) // Friday
        val history = mapOf(
            LocalDate.of(2026, 7, 20) to completedWorkout(LocalDate.of(2026, 7, 20)),
            LocalDate.of(2026, 7, 22) to completedWorkout(LocalDate.of(2026, 7, 22)),
        )

        val progress = ProgressTracker.build(profile, today, history)

        assertEquals(2, progress.currentStreak)
        assertEquals(2, progress.completedThisWeek)
        assertEquals(3, progress.plannedThisWeek)
    }

    @Test
    fun `recovery days are ignored between completed workouts`() {
        val today = LocalDate.of(2026, 7, 25) // Saturday
        val history = mapOf(
            LocalDate.of(2026, 7, 20) to completedWorkout(LocalDate.of(2026, 7, 20)),
            LocalDate.of(2026, 7, 22) to completedWorkout(LocalDate.of(2026, 7, 22)),
            LocalDate.of(2026, 7, 24) to completedWorkout(LocalDate.of(2026, 7, 24)),
        )

        val progress = ProgressTracker.build(profile, today, history)

        assertEquals(3, progress.currentStreak)
        assertFalse(progress.history.first().isWorkoutDay)
    }

    @Test
    fun `missed scheduled workout breaks the streak`() {
        val today = LocalDate.of(2026, 7, 25) // Saturday
        val history = mapOf(
            LocalDate.of(2026, 7, 20) to completedWorkout(LocalDate.of(2026, 7, 20)),
            LocalDate.of(2026, 7, 24) to completedWorkout(LocalDate.of(2026, 7, 24)),
        )

        val progress = ProgressTracker.build(profile, today, history)

        assertEquals(1, progress.currentStreak)
    }

    @Test
    fun `history marks full and partial workouts correctly`() {
        val today = LocalDate.of(2026, 7, 24)
        val completeDate = LocalDate.of(2026, 7, 22)
        val partialIds = PlanGenerator.workoutFor(profile, today).exercises
            .take(2)
            .map { it.id }
            .toSet()
        val history = mapOf(
            completeDate to completedWorkout(completeDate),
            today to partialIds,
        )

        val progress = ProgressTracker.build(profile, today, history)
        val todayEntry = progress.history.first()
        val completedEntry = progress.history.first { it.date == completeDate }

        assertEquals(2, todayEntry.completedExercises)
        assertFalse(todayEntry.isComplete)
        assertTrue(completedEntry.isComplete)
        assertEquals(1, progress.completedInHistory)
    }

    @Test
    fun `automatic adjustments keep original progress identity in history`() {
        val today = LocalDate.of(2026, 7, 20)
        val adjustedProfile = profile.copy(
            equipment = Equipment.BODYWEIGHT,
            sessionMinutes = 60,
            movementPreferences = setOf(MovementPreference.NO_FLOOR_EXERCISES),
        )
        val workout = PlanGenerator.workoutFor(adjustedProfile, today)
        val completedSourceIds = workout.exercises
            .take(3)
            .map { it.sourceExerciseId }
            .toSet()

        val progress = ProgressTracker.build(
            profile = adjustedProfile,
            today = today,
            completionHistory = mapOf(today to completedSourceIds),
        )

        assertEquals(3, progress.history.first().completedExercises)
        assertEquals(6, progress.history.first().totalExercises)
    }

    private fun completedWorkout(date: LocalDate): Set<String> =
        PlanGenerator.workoutFor(profile, date)
            .exercises
            .map { it.sourceExerciseId }
            .toSet()
}
