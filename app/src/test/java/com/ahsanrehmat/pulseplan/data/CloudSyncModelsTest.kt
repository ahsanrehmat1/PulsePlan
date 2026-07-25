package com.ahsanrehmat.pulseplan.data

import com.ahsanrehmat.pulseplan.model.Equipment
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
