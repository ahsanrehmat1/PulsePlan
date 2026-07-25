package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.ExperienceLevel
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.model.MovementPreference
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WorkoutPersonality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanGeneratorTest {
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
    fun `workout matches selected equipment and duration`() {
        val workout = PlanGenerator.workoutFor(profile, LocalDate.of(2026, 7, 24))

        assertEquals(5, workout.exercises.size)
        assertTrue(workout.exercises.first().name.contains("Goblet"))
        assertEquals(30, workout.estimatedMinutes)
    }

    @Test
    fun `week contains requested number of workout days`() {
        val week = PlanGenerator.weekFor(profile, LocalDate.of(2026, 7, 24))

        assertEquals(7, week.size)
        assertEquals(3, week.count { it.isWorkoutDay })
        assertEquals(1, week.count { it.isToday })
    }

    @Test
    fun `non training day becomes an active recovery session`() {
        val workout = PlanGenerator.workoutFor(profile, LocalDate.of(2026, 7, 25))

        assertEquals("Recovery reset", workout.title)
        assertTrue(workout.exercises.first().name.contains("Cat-cow"))
    }

    @Test
    fun `updated preferences regenerate the workout and weekly rhythm`() {
        val updatedProfile = profile.copy(
            equipment = Equipment.BODYWEIGHT,
            daysPerWeek = 2,
            sessionMinutes = 15,
        )
        val date = LocalDate.of(2026, 7, 20)

        val workout = PlanGenerator.workoutFor(updatedProfile, date)
        val week = PlanGenerator.weekFor(updatedProfile, date)

        assertEquals(4, workout.exercises.size)
        assertTrue(workout.exercises.first().name.contains("Bodyweight"))
        assertEquals(2, week.count { it.isWorkoutDay })
    }

    @Test
    fun `approved substitution replaces one slot and keeps beginner pacing`() {
        val date = LocalDate.of(2026, 7, 24)
        val workout = PlanGenerator.workoutFor(profile, date)
        val replacement = PlanGenerator.substitutionFor(profile, "db_goblet_squat")

        requireNotNull(replacement)
        val swapped = PlanGenerator.applySubstitutions(
            profile = profile,
            workout = workout,
            selectedSubstitutions = mapOf("db_goblet_squat" to replacement.id),
        )

        assertEquals(workout.exercises.size, swapped.exercises.size)
        assertEquals("db_split_squat", swapped.exercises.first().id)
        assertTrue(swapped.exercises.first().prescription.startsWith("2 ×"))
        assertEquals(60, swapped.exercises.first().restSeconds)
    }

    @Test
    fun `invalid or stale substitutions are ignored`() {
        val workout = PlanGenerator.workoutFor(profile, LocalDate.of(2026, 7, 24))

        val sanitized = PlanGenerator.sanitizeSubstitutions(
            profile = profile,
            workout = workout,
            selectedSubstitutions = mapOf(
                "db_goblet_squat" to "wrong_replacement",
                "not_in_workout" to "db_split_squat",
            ),
        )

        assertTrue(sanitized.isEmpty())
    }

    @Test
    fun `each movement preference filters every generated plan`() {
        val startDate = LocalDate.of(2026, 7, 20)

        MovementPreference.entries.forEach { preference ->
            FitnessGoal.entries.forEach { goal ->
                Equipment.entries.forEach { equipment ->
                    val adjustedProfile = profile.copy(
                        goal = goal,
                        equipment = equipment,
                        personality = WorkoutPersonality.VARIETY,
                        daysPerWeek = 5,
                        sessionMinutes = 60,
                        movementPreferences = setOf(preference),
                    )
                    repeat(7) { offset ->
                        val workout = PlanGenerator.workoutFor(
                            profile = adjustedProfile,
                            date = startDate.plusDays(offset.toLong()),
                        )
                        assertTrue(
                            "$preference left an incompatible exercise in ${workout.date}",
                            workout.exercises.all {
                                PlanGenerator.isCompatible(it.id, setOf(preference))
                            },
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `combined movement preferences preserve workout slots and source identity`() {
        val date = LocalDate.of(2026, 7, 20)
        val adjustedProfile = profile.copy(
            equipment = Equipment.BODYWEIGHT,
            sessionMinutes = 60,
            movementPreferences = MovementPreference.entries.toSet(),
        )

        val workout = PlanGenerator.workoutFor(adjustedProfile, date)

        assertEquals(6, workout.exercises.size)
        assertEquals(6, workout.exercises.map { it.sourceExerciseId }.toSet().size)
        assertTrue(
            workout.exercises.all {
                PlanGenerator.isCompatible(it.id, adjustedProfile.movementPreferences)
            },
        )
    }

    @Test
    fun `no floor preference explains replacements without changing completion ids`() {
        val adjustedProfile = profile.copy(
            equipment = Equipment.BODYWEIGHT,
            sessionMinutes = 60,
            movementPreferences = setOf(MovementPreference.NO_FLOOR_EXERCISES),
        )

        val workout = PlanGenerator.workoutFor(
            adjustedProfile,
            LocalDate.of(2026, 7, 20),
        )
        val replaced = workout.exercises.filter { it.adjustmentReason != null }

        assertEquals(
            setOf("glute_bridge", "dead_bug", "plank"),
            replaced.map { it.sourceExerciseId }.toSet(),
        )
        assertTrue(replaced.all { it.id != it.sourceExerciseId })
        assertTrue(replaced.all { it.adjustmentReason?.contains("No floor") == true })
    }

    @Test
    fun `manual substitute falls back to a compatible movement`() {
        val adjustedProfile = profile.copy(
            equipment = Equipment.BODYWEIGHT,
            movementPreferences = setOf(MovementPreference.NO_KNEELING),
        )

        val replacement = PlanGenerator.substitutionFor(adjustedProfile, "dead_bug")

        requireNotNull(replacement)
        assertEquals("standing_cross_crawl", replacement.id)
        assertEquals("dead_bug", replacement.sourceExerciseId)
        assertTrue(
            PlanGenerator.isCompatible(
                replacement.id,
                adjustedProfile.movementPreferences,
            ),
        )
    }

    @Test
    fun `manual substitutions cannot override an automatic adjustment`() {
        val adjustedProfile = profile.copy(
            equipment = Equipment.BODYWEIGHT,
            sessionMinutes = 60,
            movementPreferences = setOf(MovementPreference.NO_FLOOR_EXERCISES),
        )
        val workout = PlanGenerator.workoutFor(
            adjustedProfile,
            LocalDate.of(2026, 7, 20),
        )

        val sanitized = PlanGenerator.sanitizeSubstitutions(
            profile = adjustedProfile,
            workout = workout,
            selectedSubstitutions = mapOf("dead_bug" to "standing_cross_crawl"),
        )

        assertTrue(sanitized.isEmpty())
    }
}
