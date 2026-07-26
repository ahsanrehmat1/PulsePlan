package com.ahsanrehmat.pulseplan.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionPhase
import com.ahsanrehmat.pulseplan.domain.WorkoutRestKind
import com.ahsanrehmat.pulseplan.domain.WorkoutSessionState
import com.ahsanrehmat.pulseplan.model.Equipment
import com.ahsanrehmat.pulseplan.model.ExerciseEffort
import com.ahsanrehmat.pulseplan.model.ExerciseMetricType
import com.ahsanrehmat.pulseplan.model.ExerciseResult
import com.ahsanrehmat.pulseplan.model.ExerciseSetDraft
import com.ahsanrehmat.pulseplan.model.ExerciseSetResult
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
import java.util.Base64

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

    fun exerciseResults(
        accountId: String,
        date: LocalDate,
    ): Flow<Map<String, ExerciseResult>> {
        val key = stringPreferencesKey("results_${safeId(accountId)}_$date")
        return context.dataStore.data.map { preferences ->
            decodeExerciseResults(preferences[key])
        }
    }

    fun resultHistory(
        accountId: String,
        dates: List<LocalDate>,
    ): Flow<Map<LocalDate, Map<String, ExerciseResult>>> {
        val safeAccountId = safeId(accountId)
        val keys = dates.associateWith { date ->
            stringPreferencesKey("results_${safeAccountId}_$date")
        }
        return context.dataStore.data.map { preferences ->
            keys.mapValues { (_, key) ->
                decodeExerciseResults(preferences[key])
            }
        }
    }

    suspend fun saveExerciseResults(
        accountId: String,
        date: LocalDate,
        results: Map<String, ExerciseResult>,
        updatedAtEpochMillis: Long = System.currentTimeMillis(),
    ) {
        val safeAccountId = safeId(accountId)
        val key = stringPreferencesKey("results_${safeAccountId}_$date")
        val updatedKey = longPreferencesKey("results_updated_${safeAccountId}_$date")
        context.dataStore.edit { preferences ->
            preferences[key] = encodeExerciseResults(results)
            preferences[updatedKey] = updatedAtEpochMillis
        }
    }

    fun workoutSession(
        accountId: String,
        date: LocalDate,
    ): Flow<WorkoutSessionState?> {
        val key = stringPreferencesKey("session_${safeId(accountId)}_$date")
        return context.dataStore.data.map { preferences ->
            preferences[key]?.let(::decodeWorkoutSession)
        }
    }

    suspend fun saveWorkoutSession(
        accountId: String,
        date: LocalDate,
        session: WorkoutSessionState,
    ) {
        val key = stringPreferencesKey("session_${safeId(accountId)}_$date")
        context.dataStore.edit { preferences ->
            preferences[key] = encodeWorkoutSession(session)
        }
    }

    suspend fun clearWorkoutSession(
        accountId: String,
        date: LocalDate,
    ) {
        val key = stringPreferencesKey("session_${safeId(accountId)}_$date")
        context.dataStore.edit { preferences ->
            preferences.remove(key)
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
            val resultsKey = stringPreferencesKey("results_${safeAccountId}_$date")
            val resultsUpdatedKey = longPreferencesKey(
                "results_updated_${safeAccountId}_$date",
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
            val exerciseResults = preferences[resultsKey]?.let { encoded ->
                SyncRecord(
                    value = decodeExerciseResults(encoded),
                    updatedAtEpochMillis = preferences[resultsUpdatedKey] ?: 0L,
                )
            }
            if (completed == null && substitutions == null && exerciseResults == null) {
                null
            } else {
                date to CloudDaySnapshot(
                    completedExercises = completed,
                    substitutions = substitutions,
                    exerciseResults = exerciseResults,
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
                day.exerciseResults?.let { record ->
                    preferences[
                        stringPreferencesKey("results_${safeAccountId}_$date")
                    ] = encodeExerciseResults(record.value)
                    preferences[
                        longPreferencesKey("results_updated_${safeAccountId}_$date")
                    ] = record.updatedAtEpochMillis
                }
            }
        }
    }

    suspend fun clearAccount(accountId: String) {
        val safeAccountId = safeId(accountId)
        context.dataStore.edit { preferences ->
            val accountKeys = preferences.asMap().keys.filter { key ->
                key.name.endsWith("_$safeAccountId") ||
                    key.name.contains("_${safeAccountId}_")
            }
            accountKeys.forEach { key ->
                @Suppress("UNCHECKED_CAST")
                preferences.remove(key as Preferences.Key<Any>)
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

    private fun encodeExerciseResults(
        results: Map<String, ExerciseResult>,
    ): String = results
        .toSortedMap()
        .values
        .joinToString(RESULT_SEPARATOR) { result ->
            listOf(
                encodeText(result.exerciseId),
                encodeText(result.exerciseName),
                encodeText(result.prescription),
                result.metricType.name,
                result.reps?.toString().orEmpty(),
                result.weightKg?.toString().orEmpty(),
                result.durationSeconds?.toString().orEmpty(),
                result.distanceKm?.toString().orEmpty(),
                result.effort.name,
                encodeText(result.notes),
                result.loggedAtEpochMillis.toString(),
                encodeExerciseSets(result.sets),
            ).joinToString(RESULT_FIELD_SEPARATOR)
        }

    private fun decodeExerciseResults(value: String?): Map<String, ExerciseResult> =
        value
            .orEmpty()
            .split(RESULT_SEPARATOR)
            .mapNotNull { encoded ->
                val parts = encoded.split(RESULT_FIELD_SEPARATOR)
                if (parts.size !in setOf(LEGACY_RESULT_FIELD_COUNT, RESULT_FIELD_COUNT)) {
                    return@mapNotNull null
                }
                runCatching {
                    ExerciseResult(
                        exerciseId = decodeText(parts[0]),
                        exerciseName = decodeText(parts[1]),
                        prescription = decodeText(parts[2]),
                        metricType = ExerciseMetricType.valueOf(parts[3]),
                        reps = parts[4].toIntOrNull(),
                        weightKg = parts[5].toDoubleOrNull(),
                        durationSeconds = parts[6].toIntOrNull(),
                        distanceKm = parts[7].toDoubleOrNull(),
                        effort = ExerciseEffort.valueOf(parts[8]),
                        notes = decodeText(parts[9]),
                        loggedAtEpochMillis = parts[10].toLong(),
                        sets = parts.getOrNull(11)
                            ?.let(::decodeExerciseSets)
                            .orEmpty(),
                    )
                }.getOrNull()
            }
            .filter { it.exerciseId.isNotBlank() }
            .associateBy(ExerciseResult::exerciseId)

    private fun encodeWorkoutSession(session: WorkoutSessionState): String = listOf(
        session.currentExerciseIndex,
        session.phase.name,
        session.restSecondsRemaining,
        session.isRestTimerRunning,
        session.currentSetIndex,
        session.restKind.name,
        encodeExerciseSets(session.completedSets),
        encodeSetDraft(session.currentSetDraft),
        session.exerciseEffort.name,
        encodeText(session.exerciseNotes),
        session.setTimerSecondsRemaining,
        session.isSetTimerRunning,
        session.sessionStartedAtEpochMillis,
    ).joinToString(RESULT_FIELD_SEPARATOR)

    private fun decodeWorkoutSession(value: String): WorkoutSessionState? = runCatching {
        val parts = value.split(RESULT_FIELD_SEPARATOR)
        when (parts.size) {
            LEGACY_SESSION_FIELD_COUNT -> WorkoutSessionState(
                currentExerciseIndex = parts[0].toInt(),
                phase = WorkoutSessionPhase.valueOf(parts[1]),
                restSecondsRemaining = parts[2].toInt().coerceAtLeast(0),
                isRestTimerRunning = parts[3].toBooleanStrict(),
            )

            SESSION_FIELD_COUNT -> WorkoutSessionState(
                currentExerciseIndex = parts[0].toInt(),
                phase = WorkoutSessionPhase.valueOf(parts[1]),
                restSecondsRemaining = parts[2].toInt().coerceAtLeast(0),
                isRestTimerRunning = parts[3].toBooleanStrict(),
                currentSetIndex = parts[4].toInt().coerceAtLeast(0),
                restKind = WorkoutRestKind.valueOf(parts[5]),
                completedSets = decodeExerciseSets(parts[6]),
                currentSetDraft = decodeSetDraft(parts[7]),
                exerciseEffort = ExerciseEffort.valueOf(parts[8]),
                exerciseNotes = decodeText(parts[9]),
                setTimerSecondsRemaining = parts[10].toInt().coerceAtLeast(0),
                isSetTimerRunning = parts[11].toBooleanStrict(),
                sessionStartedAtEpochMillis = parts[12].toLong().coerceAtLeast(0L),
            )

            else -> error("Unsupported workout session format")
        }
    }.getOrNull()

    private fun encodeExerciseSets(sets: List<ExerciseSetResult>): String =
        encodeText(
            sets.joinToString(SET_SEPARATOR) { set ->
                listOf(
                    set.setNumber,
                    set.metricType.name,
                    set.reps?.toString().orEmpty(),
                    set.weightKg?.toString().orEmpty(),
                    set.durationSeconds?.toString().orEmpty(),
                    set.distanceKm?.toString().orEmpty(),
                ).joinToString(SET_FIELD_SEPARATOR)
            },
        )

    private fun decodeExerciseSets(value: String): List<ExerciseSetResult> =
        runCatching {
            decodeText(value)
                .split(SET_SEPARATOR)
                .filter(String::isNotBlank)
                .mapNotNull { encoded ->
                    val parts = encoded.split(SET_FIELD_SEPARATOR)
                    if (parts.size != SET_FIELD_COUNT) return@mapNotNull null
                    runCatching {
                        ExerciseSetResult(
                            setNumber = parts[0].toInt(),
                            metricType = ExerciseMetricType.valueOf(parts[1]),
                            reps = parts[2].toIntOrNull(),
                            weightKg = parts[3].toDoubleOrNull(),
                            durationSeconds = parts[4].toIntOrNull(),
                            distanceKm = parts[5].toDoubleOrNull(),
                        )
                    }.getOrNull()
                }
        }.getOrDefault(emptyList())

    private fun encodeSetDraft(draft: ExerciseSetDraft?): String =
        draft?.let {
            encodeText(
                listOf(
                    it.metricType.name,
                    it.reps?.toString().orEmpty(),
                    it.weightKg?.toString().orEmpty(),
                    it.durationSeconds?.toString().orEmpty(),
                    it.distanceKm?.toString().orEmpty(),
                ).joinToString(SET_FIELD_SEPARATOR),
            )
        }.orEmpty()

    private fun decodeSetDraft(value: String): ExerciseSetDraft? {
        if (value.isBlank()) return null
        return runCatching {
            val parts = decodeText(value).split(SET_FIELD_SEPARATOR)
            require(parts.size == SET_DRAFT_FIELD_COUNT)
            ExerciseSetDraft(
                metricType = ExerciseMetricType.valueOf(parts[0]),
                reps = parts[1].toIntOrNull(),
                weightKg = parts[2].toDoubleOrNull(),
                durationSeconds = parts[3].toIntOrNull(),
                distanceKm = parts[4].toDoubleOrNull(),
            )
        }.getOrNull()
    }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)

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
        const val RESULT_SEPARATOR = ";"
        const val RESULT_FIELD_SEPARATOR = "|"
        const val LEGACY_RESULT_FIELD_COUNT = 11
        const val RESULT_FIELD_COUNT = 12
        const val LEGACY_SESSION_FIELD_COUNT = 4
        const val SESSION_FIELD_COUNT = 13
        const val SET_SEPARATOR = ","
        const val SET_FIELD_SEPARATOR = "~"
        const val SET_FIELD_COUNT = 6
        const val SET_DRAFT_FIELD_COUNT = 5
        const val SEPARATOR = "¦"
    }
}
