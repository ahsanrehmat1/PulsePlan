package com.ahsanrehmat.pulseplan.domain

import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.ExperienceLevel
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WorkoutPersonality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExerciseGuideCatalogTest {
    @Test
    fun `every generated exercise has a dedicated guide`() {
        val startDate = LocalDate.of(2026, 7, 20)
        val exerciseIds = buildSet {
            FitnessGoal.entries.forEach { goal ->
                Equipment.entries.forEach { equipment ->
                    val profile = UserFitnessProfile(
                        displayName = "Guide test",
                        goal = goal,
                        experience = ExperienceLevel.INTERMEDIATE,
                        personality = WorkoutPersonality.VARIETY,
                        equipment = equipment,
                        daysPerWeek = 5,
                        sessionMinutes = 60,
                    )
                    repeat(7) { offset ->
                        addAll(
                            PlanGenerator.workoutFor(
                                profile = profile,
                                date = startDate.plusDays(offset.toLong()),
                            ).exercises.map { it.id },
                        )
                    }
                }
            }
        }

        val missingGuides = exerciseIds.filterNot(ExerciseGuideCatalog::hasGuide)

        assertEquals(30, exerciseIds.size)
        assertTrue("Missing guides: $missingGuides", missingGuides.isEmpty())
    }

    @Test
    fun `every generated exercise has a dedicated real video search`() {
        val startDate = LocalDate.of(2026, 7, 20)
        val exerciseIds = buildSet {
            FitnessGoal.entries.forEach { goal ->
                Equipment.entries.forEach { equipment ->
                    val profile = UserFitnessProfile(
                        displayName = "Video guide test",
                        goal = goal,
                        experience = ExperienceLevel.INTERMEDIATE,
                        personality = WorkoutPersonality.VARIETY,
                        equipment = equipment,
                        daysPerWeek = 5,
                        sessionMinutes = 60,
                    )
                    repeat(7) { offset ->
                        addAll(
                            PlanGenerator.workoutFor(
                                profile = profile,
                                date = startDate.plusDays(offset.toLong()),
                            ).exercises.map { it.id },
                        )
                    }
                }
            }
        }

        val missingVideoGuides = exerciseIds.filterNot(ExerciseVideoGuideCatalog::hasVideoGuide)

        assertEquals(30, exerciseIds.size)
        assertTrue("Missing video guides: $missingVideoGuides", missingVideoGuides.isEmpty())
    }

    @Test
    fun `every approved substitution has written and video guidance`() {
        val substitutionIds = PlanGenerator.substitutionExerciseIds()
        val missingGuides = substitutionIds.filterNot(ExerciseGuideCatalog::hasGuide)
        val missingVideoGuides = substitutionIds.filterNot(
            ExerciseVideoGuideCatalog::hasVideoGuide,
        )

        assertEquals(42, substitutionIds.size)
        assertTrue("Missing substitution guides: $missingGuides", missingGuides.isEmpty())
        assertTrue(
            "Missing substitution video guides: $missingVideoGuides",
            missingVideoGuides.isEmpty(),
        )
    }

    @Test
    fun `guide contains actionable steps and safety-focused mistake`() {
        val exercise = PlanGenerator.workoutFor(
            profile = UserFitnessProfile(
                displayName = "Guide test",
                goal = FitnessGoal.BUILD_STRENGTH,
                experience = ExperienceLevel.BEGINNER,
                personality = WorkoutPersonality.GUIDED,
                equipment = Equipment.BODYWEIGHT,
                daysPerWeek = 3,
                sessionMinutes = 30,
            ),
            date = LocalDate.of(2026, 7, 20),
        ).exercises.first()

        val guide = ExerciseGuideCatalog.forExercise(exercise)

        assertEquals(3, guide.steps.size)
        assertTrue(guide.targetArea.isNotBlank())
        assertTrue(guide.commonMistake.isNotBlank())
    }
}
