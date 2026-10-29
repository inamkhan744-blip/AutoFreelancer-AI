package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class OfflineAction(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "CLIENT_STAGE_CHANGE", "CRM_CREATE", "EMAIL_OUTREACH", "DELIVERABLE_UPDATE"
    val entityId: String,
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type)
        put("entityId", entityId)
        put("payloadJson", payloadJson)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): OfflineAction = OfflineAction(
            id = json.optString("id", UUID.randomUUID().toString()),
            type = json.optString("type", "UNKNOWN"),
            entityId = json.optString("entityId", ""),
            payloadJson = json.optString("payloadJson", "{}"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

class OfflineSyncQueueRepository(
    context: Context
) {
    private val tag = "OfflineSyncQueueRepo"
    private val queueFile = File(context.applicationContext.filesDir, "offline_sync_queue.json")

    @Synchronized
    fun enqueueAction(action: OfflineAction) {
        val current = loadQueue().toMutableList()
        current.removeAll { it.id == action.id }
        current.add(action)
        saveQueue(current)
        Log.i(tag, "Enqueued offline action: ${action.type} for ${action.entityId} (Total: ${current.size})")
    }

    @Synchronized
    fun getPendingActions(): List<OfflineAction> = loadQueue()

    @Synchronized
    fun removeAction(actionId: String) {
        val current = loadQueue().toMutableList()
        current.removeAll { it.id == actionId }
        saveQueue(current)
    }

    @Synchronized
    fun clearQueue() {
        if (queueFile.exists()) {
            queueFile.delete()
        }
    }

    private fun loadQueue(): List<OfflineAction> {
        if (!queueFile.exists()) return emptyList()
        return try {
            val text = queueFile.readText()
            val arr = JSONArray(text)
            val list = mutableListOf<OfflineAction>()
            for (i in 0 until arr.length()) {
                list.add(OfflineAction.fromJson(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            Log.e(tag, "Failed to read offline queue: ${e.message}")
            emptyList()
        }
    }

    private fun saveQueue(actions: List<OfflineAction>) {
        try {
            val arr = JSONArray()
            actions.forEach { arr.put(it.toJson()) }
            queueFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(tag, "Failed to write offline queue: ${e.message}")
        }
    }

    /**
     * Flushes queued actions to Firestore when network connectivity is restored.
     */
    suspend fun flushSyncToFirestore(
        firestore: FirebaseFirestore?,
        userId: String?
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (firestore == null || userId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Firestore or User ID unavailable for sync."))
        }

        val pending = getPendingActions()
        if (pending.isEmpty()) {
            return@withContext Result.success(0)
        }

        var syncedCount = 0
        val remaining = mutableListOf<OfflineAction>()

        for (action in pending) {
            try {
                when (action.type) {
                    "CLIENT_STAGE_CHANGE", "CRM_CREATE" -> {
                        val payload = JSONObject(action.payloadJson)
                        val map = mutableMapOf<String, Any?>()
                        val keys = payload.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = payload.get(k)
                        }
                        firestore.collection("users").document(userId)
                            .collection("crm_clients").document(action.entityId)
                            .set(map, com.google.firebase.firestore.SetOptions.merge())
                            .await()
                        syncedCount++
                    }
                    "DELIVERABLE_UPDATE" -> {
                        val payload = JSONObject(action.payloadJson)
                        val map = mutableMapOf<String, Any?>()
                        val keys = payload.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            map[k] = payload.get(k)
                        }
                        firestore.collection("users").document(userId)
                            .collection("deliverables").document(action.entityId)
                            .set(map, com.google.firebase.firestore.SetOptions.merge())
                            .await()
                        syncedCount++
                    }
                    else -> {
                        syncedCount++
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed to flush offline action ${action.id}: ${e.message}")
                remaining.add(action)
            }
        }

        saveQueue(remaining)
        Log.i(tag, "Synced $syncedCount offline actions to Firestore (${remaining.size} remaining)")
        Result.success(syncedCount)
    }
}
