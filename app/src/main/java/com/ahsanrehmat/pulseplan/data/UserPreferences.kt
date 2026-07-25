package com.ahsanrehmat.pulseplan.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.ExperienceLevel
import com.ahsanrehmat.pulseplan.model.FitnessGoal
import com.ahsanrehmat.pulseplan.model.MovementPreference
import com.ahsanrehmat.pulseplan.model.ReminderTime
import com.ahsanrehmat.pulseplan.model.UserFitnessProfile
import com.ahsanrehmat.pulseplan.model.WorkoutPersonality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "pulse_plan_preferences")

class UserPreferences(private val context: Context) {
    fun profile(accountId: String): Flow<UserFitnessProfile?> {
        val key = stringPreferencesKey("profile_${safeId(accountId)}")
        return context.dataStore.data.map { preferences ->
            preferences[key]?.let(::decodeProfile)
        }
    }

    suspend fun saveProfile(
        accountId: String,
        profile: UserFitnessProfile,
        updatedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val key = stringPreferencesKey("profile_${safeId(accountId)}")
        val updatedKey = longPreferencesKey("profile_updated_${safeId(accountId)}")
        context.dataStore.edit { preferences ->
            preferences[key] = encodeProfile(profile)
            preferences[updatedKey] = updatedAtEpochMillis
        }
    }

    fun completedExercises(accountId: String, date: LocalDate): Flow<Set<String>> {
        val key = stringPreferencesKey("completed_${safeId(accountId)}_$date")
        return context.dataStore.data.map { preferences ->
            decodeCompletedExercises(preferences[key])
        }
    }

    fun completionHistory(
        accountId: String,
        dates: List<LocalDate>,
    ): Flow<Map<LocalDate, Set<String>>> {
        val safeAccountId = safeId(accountId)
        val keys = dates.associateWith { date ->
            stringPreferencesKey("completed_${safeAccountId}_$date")
        }
        return context.dataStore.data.map { preferences ->
            keys.mapValues { (_, key) ->
                decodeCompletedExercises(preferences[key])
            }
        }
    }

    suspend fun saveCompletedExercises(
        accountId: String,
        date: LocalDate,
        exerciseIds: Set<String>,
        updatedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val key = stringPreferencesKey("completed_${safeId(accountId)}_$date")
        val updatedKey = longPreferencesKey("completed_updated_${safeId(accountId)}_$date")
        context.dataStore.edit { preferences ->
            preferences[key] = exerciseIds.sorted().joinToString(",")
            preferences[updatedKey] = updatedAtEpochMillis
        }
    }

    fun exerciseSubstitutions(
        accountId: String,
        date: LocalDate,
    ): Flow<Map<String, String>> {
        val key = stringPreferencesKey("substitutions_${safeId(accountId)}_$date")
        return context.dataStore.data.map { preferences ->
            decodeExerciseSubstitutions(preferences[key])
        }
    }

    suspend fun saveExerciseSubstitutions(
        accountId: String,
        date: LocalDate,
        substitutions: Map<String, String>,
        updatedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val key = stringPreferencesKey("substitutions_${safeId(accountId)}_$date")
        val updatedKey = longPreferencesKey(
            "substitutions_updated_${safeId(accountId)}_$date",
        )
        context.dataStore.edit { preferences ->
            preferences[key] = substitutions
                .toSortedMap()
                .entries
                .joinToString(",") { (originalId, replacementId) ->
                    "$originalId:$replacementId"
                }
            preferences[updatedKey] = updatedAtEpochMillis
        }
    }

    fun reminderTime(accountId: String): Flow<ReminderTime> {
        val safeAccountId = safeId(accountId)
        val hourKey = intPreferencesKey("reminder_hour_$safeAccountId")
        val minuteKey = intPreferencesKey("reminder_minute_$safeAccountId")
        return context.dataStore.data.map { preferences ->
            runCatching {
                ReminderTime(
                    hour = preferences[hourKey] ?: ReminderTime.DEFAULT.hour,
                    minute = preferences[minuteKey] ?: ReminderTime.DEFAULT.minute,
                )
            }.getOrDefault(ReminderTime.DEFAULT)
        }
    }

    suspend fun saveReminderTime(
        accountId: String,
        reminderTime: ReminderTime,
        updatedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val safeAccountId = safeId(accountId)
        val hourKey = intPreferencesKey("reminder_hour_$safeAccountId")
        val minuteKey = intPreferencesKey("reminder_minute_$safeAccountId")
        val updatedKey = longPreferencesKey("reminder_updated_$safeAccountId")
        context.dataStore.edit { preferences ->
            preferences[hourKey] = reminderTime.hour
            preferences[minuteKey] = reminderTime.minute
            preferences[updatedKey] = updatedAtEpochMillis
        }
    }

    suspend fun cloudSnapshot(
        accountId: String,
        dates: List<LocalDate>,
    ): CloudAccountSnapshot {
        val safeAccountId = safeId(accountId)
        val preferences = context.dataStore.data.first()
        val profileKey = stringPreferencesKey("profile_$safeAccountId")
        val profileUpdatedKey = longPreferencesKey("profile_updated_$safeAccountId")
        val reminderHourKey = intPreferencesKey("reminder_hour_$safeAccountId")
        val reminderMinuteKey = intPreferencesKey("reminder_minute_$safeAccountId")
        val reminderUpdatedKey = longPreferencesKey("reminder_updated_$safeAccountId")

        val profile = preferences[profileKey]
            ?.let(::decodeProfile)
            ?.let { value ->
                SyncRecord(
                    value = value,
                    updatedAtEpochMillis = preferences[profileUpdatedKey] ?: 0L,
                )
            }
        val reminder = if (
            preferences[reminderHourKey] != null ||
            preferences[reminderMinuteKey] != null
        ) {
            runCatching {
                ReminderTime(
                    hour = preferences[reminderHourKey] ?: ReminderTime.DEFAULT.hour,
                    minute = preferences[reminderMinuteKey] ?: ReminderTime.DEFAULT.minute,
                )
            }.getOrNull()?.let { value ->
                SyncRecord(
                    value = value,
                    updatedAtEpochMillis = preferences[reminderUpdatedKey] ?: 0L,
                )
            }
        } else {
            null
        }

        val days = dates.mapNotNull { date ->
            val completedKey = stringPreferencesKey("completed_${safeAccountId}_$date")
            val completedUpdatedKey = longPreferencesKey(
                "completed_updated_${safeAccountId}_$date",
            )
            val substitutionsKey = stringPreferencesKey(
                "substitutions_${safeAccountId}_$date",
            )
            val substitutionsUpdatedKey = longPreferencesKey(
                "substitutions_updated_${safeAccountId}_$date",
            )
            val completed = preferences[completedKey]?.let { encoded ->
                SyncRecord(
                    value = decodeCompletedExercises(encoded),
                    updatedAtEpochMillis = preferences[completedUpdatedKey] ?: 0L,
                )
            }
            val substitutions = preferences[substitutionsKey]?.let { encoded ->
                SyncRecord(
                    value = decodeExerciseSubstitutions(encoded),
                    updatedAtEpochMillis = preferences[substitutionsUpdatedKey] ?: 0L,
                )
            }
            if (completed == null && substitutions == null) {
                null
            } else {
                date to CloudDaySnapshot(
                    completedExercises = completed,
                    substitutions = substitutions,
                )
            }
        }.toMap()

        return CloudAccountSnapshot(
            profile = profile,
            reminderTime = reminder,
            days = days,
        )
    }

    suspend fun applyCloudSnapshot(
        accountId: String,
        snapshot: CloudAccountSnapshot,
    ) {
        val safeAccountId = safeId(accountId)
        context.dataStore.edit { preferences ->
            snapshot.profile?.let { record ->
                preferences[stringPreferencesKey("profile_$safeAccountId")] =
                    encodeProfile(record.value)
                preferences[longPreferencesKey("profile_updated_$safeAccountId")] =
                    record.updatedAtEpochMillis
            }
            snapshot.reminderTime?.let { record ->
                preferences[intPreferencesKey("reminder_hour_$safeAccountId")] =
                    record.value.hour
                preferences[intPreferencesKey("reminder_minute_$safeAccountId")] =
                    record.value.minute
                preferences[longPreferencesKey("reminder_updated_$safeAccountId")] =
                    record.updatedAtEpochMillis
            }
            snapshot.days.forEach { (date, day) ->
                day.completedExercises?.let { record ->
                    preferences[stringPreferencesKey("completed_${safeAccountId}_$date")] =
                        record.value.sorted().joinToString(",")
                    preferences[
                        longPreferencesKey("completed_updated_${safeAccountId}_$date")
                    ] = record.updatedAtEpochMillis
                }
                day.substitutions?.let { record ->
                    preferences[
                        stringPreferencesKey("substitutions_${safeAccountId}_$date")
                    ] = record.value
                        .toSortedMap()
                        .entries
                        .joinToString(",") { (originalId, replacementId) ->
                            "$originalId:$replacementId"
                        }
                    preferences[
                        longPreferencesKey("substitutions_updated_${safeAccountId}_$date")
                    ] = record.updatedAtEpochMillis
                }
            }
        }
    }

    private fun safeId(accountId: String) = accountId.hashCode().toUInt().toString(16)

    private fun decodeCompletedExercises(value: String?): Set<String> =
        value
            .orEmpty()
            .split(",")
            .filter(String::isNotBlank)
            .toSet()

    private fun decodeExerciseSubstitutions(value: String?): Map<String, String> =
        value
            .orEmpty()
            .split(",")
            .mapNotNull { encoded ->
                val parts = encoded.split(":", limit = 2)
                if (parts.size == 2 && parts.all(String::isNotBlank)) {
                    parts[0] to parts[1]
                } else {
                    null
                }
            }
            .toMap()

    private fun encodeProfile(profile: UserFitnessProfile): String = listOf(
        profile.displayName,
        profile.goal.name,
        profile.experience.name,
        profile.personality.name,
        profile.equipment.name,
        profile.daysPerWeek.toString(),
        profile.sessionMinutes.toString(),
        profile.movementNotes,
        profile.movementPreferences
            .map(MovementPreference::name)
            .sorted()
            .joinToString(","),
    ).joinToString(SEPARATOR) { value ->
        value.replace(SEPARATOR, " ")
    }

    private fun decodeProfile(value: String): UserFitnessProfile? = runCatching {
        val parts = value.split(SEPARATOR)
        UserFitnessProfile(
            displayName = parts[0],
            goal = FitnessGoal.valueOf(parts[1]),
            experience = ExperienceLevel.valueOf(parts[2]),
            personality = WorkoutPersonality.valueOf(parts[3]),
            equipment = Equipment.valueOf(parts[4]),
            daysPerWeek = parts[5].toInt(),
            sessionMinutes = parts[6].toInt(),
            movementNotes = parts.getOrElse(7) { "" },
            movementPreferences = parts.getOrElse(8) { "" }
                .split(",")
                .filter(String::isNotBlank)
                .mapNotNull { encoded ->
                    runCatching { MovementPreference.valueOf(encoded) }.getOrNull()
                }
                .toSet(),
        )
    }.getOrNull()

    private companion object {
        const val SEPARATOR = "¦"
    }
}
