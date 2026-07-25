package com.ahsanrehmat.pulseplan.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface CloudSyncRepository {
    val isConfigured: Boolean

    suspend fun load(
        accountId: String,
        earliestDate: LocalDate,
    ): CloudAccountSnapshot?

    suspend fun saveSnapshot(
        accountId: String,
        snapshot: CloudAccountSnapshot,
    )

    suspend fun saveProfile(
        accountId: String,
        profile: SyncRecord<com.ahsanrehmat.pulseplan.model.UserFitnessProfile>,
    )

    suspend fun saveReminder(
        accountId: String,
        reminder: SyncRecord<com.ahsanrehmat.pulseplan.model.ReminderTime>,
    )

    suspend fun saveDay(
        accountId: String,
        date: LocalDate,
        day: CloudDaySnapshot,
    )
}

class FirebaseCloudSyncRepository(context: Context) : CloudSyncRepository {
    private val firestore: FirebaseFirestore? = FirebaseApp.getApps(context)
        .firstOrNull()
        ?.let(FirebaseFirestore::getInstance)

    override val isConfigured: Boolean = firestore != null

    override suspend fun load(
        accountId: String,
        earliestDate: LocalDate,
    ): CloudAccountSnapshot? {
        val database = requireNotNull(firestore) { "Cloud sync is not configured." }
        val userDocument = database.collection(USERS_COLLECTION).document(accountId)
        val accountDocument = userDocument.get().awaitResult()
        val dayDocuments = userDocument
            .collection(DAYS_COLLECTION)
            .whereGreaterThanOrEqualTo("date", earliestDate.toString())
            .get()
            .awaitResult()

        if (!accountDocument.exists() && dayDocuments.isEmpty) return null

        val account = CloudSyncCodec.accountFromMap(accountDocument.data.orEmpty())
        val days = dayDocuments.documents.mapNotNull { document ->
            CloudSyncCodec.dayFromMap(document.data.orEmpty())
        }.toMap()
        return account.copy(days = days)
    }

    override suspend fun saveSnapshot(
        accountId: String,
        snapshot: CloudAccountSnapshot,
    ) {
        val database = requireNotNull(firestore) { "Cloud sync is not configured." }
        val userDocument = database.collection(USERS_COLLECTION).document(accountId)
        database.runBatch { batch ->
            batch.set(
                userDocument,
                CloudSyncCodec.accountToMap(snapshot) + serverWriteMetadata(),
                SetOptions.merge(),
            )
            snapshot.days.forEach { (date, day) ->
                batch.set(
                    userDocument.collection(DAYS_COLLECTION).document(date.toString()),
                    CloudSyncCodec.dayToMap(date, day) + serverWriteMetadata(),
                    SetOptions.merge(),
                )
            }
        }.awaitResult()
    }

    override suspend fun saveProfile(
        accountId: String,
        profile: SyncRecord<com.ahsanrehmat.pulseplan.model.UserFitnessProfile>,
    ) {
        val database = requireNotNull(firestore) { "Cloud sync is not configured." }
        database.collection(USERS_COLLECTION)
            .document(accountId)
            .set(
                CloudSyncCodec.accountToMap(CloudAccountSnapshot(profile = profile)) +
                    serverWriteMetadata(),
                SetOptions.merge(),
            )
            .awaitResult()
    }

    override suspend fun saveReminder(
        accountId: String,
        reminder: SyncRecord<com.ahsanrehmat.pulseplan.model.ReminderTime>,
    ) {
        val database = requireNotNull(firestore) { "Cloud sync is not configured." }
        database.collection(USERS_COLLECTION)
            .document(accountId)
            .set(
                CloudSyncCodec.accountToMap(CloudAccountSnapshot(reminderTime = reminder)) +
                    serverWriteMetadata(),
                SetOptions.merge(),
            )
            .awaitResult()
    }

    override suspend fun saveDay(
        accountId: String,
        date: LocalDate,
        day: CloudDaySnapshot,
    ) {
        val database = requireNotNull(firestore) { "Cloud sync is not configured." }
        database.collection(USERS_COLLECTION)
            .document(accountId)
            .collection(DAYS_COLLECTION)
            .document(date.toString())
            .set(
                CloudSyncCodec.dayToMap(date, day) + serverWriteMetadata(),
                SetOptions.merge(),
            )
            .awaitResult()
    }

    private fun serverWriteMetadata(): Map<String, Any> = mapOf(
        "serverUpdatedAt" to FieldValue.serverTimestamp(),
    )

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                when {
                    !continuation.isActive -> Unit
                    task.isSuccessful -> continuation.resume(task.result)
                    else -> continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Cloud operation failed."),
                    )
                }
            }
        }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val DAYS_COLLECTION = "days"
    }
}
