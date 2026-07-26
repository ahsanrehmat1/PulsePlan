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
import java.time.LocalDate

data class SyncRecord<T>(
    val value: T,
    val updatedAtEpochMillis: Long,
)

data class CloudDaySnapshot(
    val completedExercises: SyncRecord<Set<String>>? = null,
    val substitutions: SyncRecord<Map<String, String>>? = null,
    val exerciseResults: SyncRecord<Map<String, ExerciseResult>>? = null,
)

data class CloudAccountSnapshot(
    val profile: SyncRecord<UserFitnessProfile>? = null,
    val reminderTime: SyncRecord<ReminderTime>? = null,
    val days: Map<LocalDate, CloudDaySnapshot> = emptyMap(),
)

object CloudSyncMerger {
    fun merge(
        local: CloudAccountSnapshot,
        remote: CloudAccountSnapshot?,
    ): CloudAccountSnapshot {
        if (remote == null) return local

        val allDates = local.days.keys + remote.days.keys
        return CloudAccountSnapshot(
            profile = newest(local.profile, remote.profile),
            reminderTime = newest(local.reminderTime, remote.reminderTime),
            days = allDates.associateWith { date ->
                val localDay = local.days[date]
                val remoteDay = remote.days[date]
                CloudDaySnapshot(
                    completedExercises = newest(
                        localDay?.completedExercises,
                        remoteDay?.completedExercises,
                    ),
                    substitutions = newest(
                        localDay?.substitutions,
                        remoteDay?.substitutions,
                    ),
                    exerciseResults = newest(
                        localDay?.exerciseResults,
                        remoteDay?.exerciseResults,
                    ),
                )
            },
        )
    }

    private fun <T> newest(
        local: SyncRecord<T>?,
        remote: SyncRecord<T>?,
    ): SyncRecord<T>? = when {
        local == null -> remote
        remote == null -> local
        local.updatedAtEpochMillis > remote.updatedAtEpochMillis -> local
        else -> remote
    }
}

object CloudSyncCodec {
    const val SCHEMA_VERSION = 4L

    fun accountToMap(snapshot: CloudAccountSnapshot): Map<String, Any> = buildMap {
        put("schemaVersion", SCHEMA_VERSION)
        snapshot.profile?.let { record ->
            put("profile", profileToMap(record.value))
            put("profileUpdatedAt", record.updatedAtEpochMillis)
        }
        snapshot.reminderTime?.let { record ->
            put(
                "reminder",
                mapOf(
                    "hour" to record.value.hour,
                    "minute" to record.value.minute,
                ),
            )
            put("reminderUpdatedAt", record.updatedAtEpochMillis)
        }
    }

    fun accountFromMap(data: Map<String, Any>): CloudAccountSnapshot {
        val profile = mapValue(data["profile"])
            ?.let(::profileFromMap)
            ?.let { SyncRecord(it, longValue(data["profileUpdatedAt"])) }
        val reminder = mapValue(data["reminder"])
            ?.let(::reminderFromMap)
            ?.let { SyncRecord(it, longValue(data["reminderUpdatedAt"])) }
        return CloudAccountSnapshot(
            profile = profile,
            reminderTime = reminder,
        )
    }

    fun dayToMap(
        date: LocalDate,
        snapshot: CloudDaySnapshot,
    ): Map<String, Any> = buildMap {
        put("date", date.toString())
        put("schemaVersion", SCHEMA_VERSION)
        snapshot.completedExercises?.let { record ->
            put("completedExerciseIds", record.value.sorted())
            put("completedUpdatedAt", record.updatedAtEpochMillis)
        }
        snapshot.substitutions?.let { record ->
            put("substitutions", record.value.toSortedMap())
            put("substitutionsUpdatedAt", record.updatedAtEpochMillis)
        }
        snapshot.exerciseResults?.let { record ->
            put(
                "exerciseResults",
                record.value.toSortedMap().values.map(::exerciseResultToMap),
            )
            put("exerciseResultsUpdatedAt", record.updatedAtEpochMillis)
        }
    }

    fun dayFromMap(data: Map<String, Any>): Pair<LocalDate, CloudDaySnapshot>? {
        val date = runCatching {
            LocalDate.parse(data["date"] as? String)
        }.getOrNull() ?: return null

        val completed = (data["completedExerciseIds"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.filter(String::isNotBlank)
            ?.toSet()
            ?.let { SyncRecord(it, longValue(data["completedUpdatedAt"])) }
        val substitutions = mapValue(data["substitutions"])
            ?.mapNotNull { (key, value) ->
                (value as? String)
                    ?.takeIf(String::isNotBlank)
                    ?.let { key to it }
            }
            ?.toMap()
            ?.let { SyncRecord(it, longValue(data["substitutionsUpdatedAt"])) }
        val exerciseResults = (data["exerciseResults"] as? List<*>)
            ?.mapNotNull { mapValue(it)?.let(::exerciseResultFromMap) }
            ?.associateBy(ExerciseResult::exerciseId)
            ?.let { SyncRecord(it, longValue(data["exerciseResultsUpdatedAt"])) }

        return date to CloudDaySnapshot(
            completedExercises = completed,
            substitutions = substitutions,
            exerciseResults = exerciseResults,
        )
    }

    private fun exerciseResultToMap(result: ExerciseResult): Map<String, Any> = buildMap {
        put("exerciseId", result.exerciseId)
        put("exerciseName", result.exerciseName)
        put("prescription", result.prescription)
        put("metricType", result.metricType.name)
        result.reps?.let { put("reps", it) }
        result.weightKg?.let { put("weightKg", it) }
        result.durationSeconds?.let { put("durationSeconds", it) }
        result.distanceKm?.let { put("distanceKm", it) }
        put("effort", result.effort.name)
        put("notes", result.notes)
        put("loggedAtEpochMillis", result.loggedAtEpochMillis)
        if (result.sets.isNotEmpty()) {
            put(
                "sets",
                result.sets.map { set ->
                    buildMap {
                        put("setNumber", set.setNumber)
                        put("metricType", set.metricType.name)
                        set.reps?.let { put("reps", it) }
                        set.weightKg?.let { put("weightKg", it) }
                        set.durationSeconds?.let { put("durationSeconds", it) }
                        set.distanceKm?.let { put("distanceKm", it) }
                    }
                },
            )
        }
    }

    private fun exerciseResultFromMap(data: Map<String, Any>): ExerciseResult? = runCatching {
        ExerciseResult(
            exerciseId = data["exerciseId"] as String,
            exerciseName = data["exerciseName"] as String,
            prescription = data["prescription"] as? String ?: "",
            metricType = ExerciseMetricType.valueOf(data["metricType"] as String),
            reps = (data["reps"] as? Number)?.toInt(),
            weightKg = (data["weightKg"] as? Number)?.toDouble(),
            durationSeconds = (data["durationSeconds"] as? Number)?.toInt(),
            distanceKm = (data["distanceKm"] as? Number)?.toDouble(),
            effort = (data["effort"] as? String)
                ?.let(ExerciseEffort::valueOf)
                ?: ExerciseEffort.GOOD,
            notes = data["notes"] as? String ?: "",
            loggedAtEpochMillis = longValue(data["loggedAtEpochMillis"]),
            sets = (data["sets"] as? List<*>)
                .orEmpty()
                .mapNotNull { mapValue(it)?.let(::exerciseSetResultFromMap) },
        )
    }.getOrNull()?.takeIf { it.exerciseId.isNotBlank() }

    private fun exerciseSetResultFromMap(data: Map<String, Any>): ExerciseSetResult? =
        runCatching {
            ExerciseSetResult(
                setNumber = (data["setNumber"] as Number).toInt(),
                metricType = ExerciseMetricType.valueOf(data["metricType"] as String),
                reps = (data["reps"] as? Number)?.toInt(),
                weightKg = (data["weightKg"] as? Number)?.toDouble(),
                durationSeconds = (data["durationSeconds"] as? Number)?.toInt(),
                distanceKm = (data["distanceKm"] as? Number)?.toDouble(),
            )
        }.getOrNull()?.takeIf { it.setNumber > 0 }

    private fun profileToMap(profile: UserFitnessProfile): Map<String, Any> = mapOf(
        "displayName" to profile.displayName,
        "goal" to profile.goal.name,
        "experience" to profile.experience.name,
        "personality" to profile.personality.name,
        "equipment" to profile.equipment.name,
        "daysPerWeek" to profile.daysPerWeek,
        "sessionMinutes" to profile.sessionMinutes,
        "movementNotes" to profile.movementNotes,
        "movementPreferences" to profile.movementPreferences
            .map(MovementPreference::name)
            .sorted(),
    )

    private fun profileFromMap(data: Map<String, Any>): UserFitnessProfile? = runCatching {
        UserFitnessProfile(
            displayName = data["displayName"] as String,
            goal = FitnessGoal.valueOf(data["goal"] as String),
            experience = ExperienceLevel.valueOf(data["experience"] as String),
            personality = WorkoutPersonality.valueOf(data["personality"] as String),
            equipment = Equipment.valueOf(data["equipment"] as String),
            daysPerWeek = (data["daysPerWeek"] as Number).toInt(),
            sessionMinutes = (data["sessionMinutes"] as Number).toInt(),
            movementNotes = data["movementNotes"] as? String ?: "",
            movementPreferences = (data["movementPreferences"] as? List<*>)
                .orEmpty()
                .mapNotNull { encoded ->
                    (encoded as? String)?.let {
                        runCatching { MovementPreference.valueOf(it) }.getOrNull()
                    }
                }
                .toSet(),
        )
    }.getOrNull()

    private fun reminderFromMap(data: Map<String, Any>): ReminderTime? = runCatching {
        ReminderTime(
            hour = (data["hour"] as Number).toInt(),
            minute = (data["minute"] as Number).toInt(),
        )
    }.getOrNull()

    private fun mapValue(value: Any?): Map<String, Any>? {
        val rawMap = value as? Map<*, *> ?: return null
        return rawMap.entries.mapNotNull { (key, item) ->
            val stringKey = key as? String ?: return@mapNotNull null
            val nonNullItem = item ?: return@mapNotNull null
            stringKey to nonNullItem
        }.toMap()
    }

    private fun longValue(value: Any?): Long = (value as? Number)?.toLong() ?: 0L
}
