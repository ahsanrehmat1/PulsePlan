package com.ahsanrehmat.pulseplan.data

import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult
import com.ahsanrehmat.pulseplan.model.ExperienceLevel
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.model.MovementPreference
import com.ahsanrehmat.pulseplan.model.ReminderTime
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WorkoutPersonality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CloudSyncModelsTest {
    @Test
    fun `newest timestamp wins independently for profile and reminder`() {
        val local = CloudAccountSnapshot(
            profile = SyncRecord(profile("Local"), updatedAtEpochMillis = 200L),
            reminderTime = SyncRecord(ReminderTime(7, 30), updatedAtEpochMillis = 100L),
        )
        val remote = CloudAccountSnapshot(
            profile = SyncRecord(profile("Remote"), updatedAtEpochMillis = 100L),
            reminderTime = SyncRecord(ReminderTime(18, 0), updatedAtEpochMillis = 300L),
        )

        val merged = CloudSyncMerger.merge(local, remote)

        assertEquals("Local", merged.profile?.value?.displayName)
        assertEquals(ReminderTime(18, 0), merged.reminderTime?.value)
    }

    @Test
    fun `newer empty completion set preserves an intentional uncheck`() {
        val date = LocalDate.of(2026, 7, 25)
        val local = CloudAccountSnapshot(
            days = mapOf(
                date to CloudDaySnapshot(
                    completedExercises = SyncRecord(emptySet(), 500L),
                ),
            ),
        )
        val remote = CloudAccountSnapshot(
            days = mapOf(
                date to CloudDaySnapshot(
                    completedExercises = SyncRecord(setOf("squat"), 400L),
                ),
            ),
        )

        val merged = CloudSyncMerger.merge(local, remote)

        assertEquals(emptySet<String>(), merged.days[date]?.completedExercises?.value)
    }

    @Test
    fun `remote records populate a new device with no local data`() {
        val date = LocalDate.of(2026, 7, 24)
        val remote = CloudAccountSnapshot(
            profile = SyncRecord(profile("Ahsan"), 100L),
            days = mapOf(
                date to CloudDaySnapshot(
                    completedExercises = SyncRecord(setOf("plank"), 120L),
                ),
            ),
        )

        val merged = CloudSyncMerger.merge(CloudAccountSnapshot(), remote)

        assertEquals(remote, merged)
    }

    @Test
    fun `account and day maps round trip without losing workout data`() {
        val date = LocalDate.of(2026, 7, 25)
        val account = CloudAccountSnapshot(
            profile = SyncRecord(profile("Ahsan"), 123L),
            reminderTime = SyncRecord(ReminderTime(6, 45), 456L),
        )
        val day = CloudDaySnapshot(
            completedExercises = SyncRecord(setOf("squat", "plank"), 789L),
            substitutions = SyncRecord(mapOf("squat" to "chair-squat"), 790L),
            exerciseResults = SyncRecord(
                mapOf(
                    "squat" to ExerciseResult(
                        exerciseId = "squat",
                        exerciseName = "Bodyweight squat",
                        prescription = "2 x 10",
                        metricType = ExerciseMetricType.REPS,
                        reps = 12,
                        weightKg = 5.5,
                        effort = ExerciseEffort.GOOD,
                        notes = "Controlled form",
                        loggedAtEpochMillis = 791L,
                        sets = listOf(
                            ExerciseSetResult(
                                setNumber = 1,
                                metricType = ExerciseMetricType.REPS,
                                reps = 12,
                                weightKg = 5.5,
                            ),
                            ExerciseSetResult(
                                setNumber = 2,
                                metricType = ExerciseMetricType.REPS,
                                reps = 11,
                                weightKg = 5.5,
                            ),
                        ),
                    ),
                ),
                792L,
            ),
        )

        val decodedAccount = CloudSyncCodec.accountFromMap(
            CloudSyncCodec.accountToMap(account),
        )
        val decodedDay = CloudSyncCodec.dayFromMap(
            CloudSyncCodec.dayToMap(date, day),
        )

        assertEquals(account, decodedAccount)
        assertEquals(date to day, decodedDay)
    }

    @Test
    fun `newer exercise results win without overwriting newer completions`() {
        val date = LocalDate.of(2026, 7, 25)
        val localResult = ExerciseResult(
            exerciseId = "squat",
            exerciseName = "Squat",
            prescription = "3 x 10",
            metricType = ExerciseMetricType.REPS,
            reps = 10,
            loggedAtEpochMillis = 100L,
        )
        val remoteResult = localResult.copy(reps = 12, loggedAtEpochMillis = 200L)
        val local = CloudAccountSnapshot(
            days = mapOf(
                date to CloudDaySnapshot(
                    completedExercises = SyncRecord(setOf("squat"), 500L),
                    exerciseResults = SyncRecord(mapOf("squat" to localResult), 100L),
                ),
            ),
        )
        val remote = CloudAccountSnapshot(
            days = mapOf(
                date to CloudDaySnapshot(
                    completedExercises = SyncRecord(emptySet(), 400L),
                    exerciseResults = SyncRecord(mapOf("squat" to remoteResult), 200L),
                ),
            ),
        )

        val merged = CloudSyncMerger.merge(local, remote)

        assertEquals(setOf("squat"), merged.days[date]?.completedExercises?.value)
        assertEquals(12, merged.days[date]?.exerciseResults?.value?.get("squat")?.reps)
    }

    @Test
    fun `empty result map round trips so a deletion can sync`() {
        val date = LocalDate.of(2026, 7, 25)
        val day = CloudDaySnapshot(
            exerciseResults = SyncRecord(emptyMap(), 900L),
        )

        val decoded = CloudSyncCodec.dayFromMap(
            CloudSyncCodec.dayToMap(date, day),
        )

        assertEquals(date to day, decoded)
    }

    @Test
    fun `malformed cloud records are ignored instead of crashing`() {
        val account = CloudSyncCodec.accountFromMap(
            mapOf(
                "profile" to mapOf("displayName" to "Incomplete"),
                "reminder" to mapOf("hour" to 99, "minute" to 0),
            ),
        )
        val day = CloudSyncCodec.dayFromMap(
            mapOf("date" to "not-a-date"),
        )

        assertNull(account.profile)
        assertNull(account.reminderTime)
        assertNull(day)
    }

    private fun profile(name: String) = UserFitnessProfile(
        displayName = name,
        goal = FitnessGoal.GENERAL_FITNESS,
        experience = ExperienceLevel.BEGINNER,
        personality = WorkoutPersonality.GUIDED,
        equipment = Equipment.BODYWEIGHT,
        daysPerWeek = 3,
        sessionMinutes = 25,
        movementNotes = "",
        movementPreferences = setOf(
            MovementPreference.NO_FLOOR_EXERCISES,
            MovementPreference.LIMIT_WRIST_LOADING,
        ),
    )
}
