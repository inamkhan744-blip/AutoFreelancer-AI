package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CrmClient
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.UUID

class ClientRepository(
    private val db: FirebaseFirestore? = null,
    private val context: Context? = null
) {
    private val tag = "ClientRepository"
    private val auth by lazy { runCatching { Firebase.auth }.getOrNull() }
    private val localClientsFlow = MutableStateFlow<List<CrmClient>>(emptyList())
    private val storageFile: File? = context?.filesDir?.let { File(it, "autofreelancer_crm_clients.json") }

    constructor(context: Context) : this(db = null, context = context.applicationContext)
    constructor(db: FirebaseFirestore) : this(db = db, context = null)

    init {
        loadLocalClients()
    }

    private fun loadLocalClients() {
        val file = storageFile ?: return
        if (!file.exists()) {
            localClientsFlow.value = emptyList()
            return
        }
        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                val jsonArr = JSONArray(content)
                val list = mutableListOf<CrmClient>()
                for (i in 0 until jsonArr.length()) {
                    list.add(CrmClient.fromJson(jsonArr.getJSONObject(i)))
                }
                localClientsFlow.value = list
            } else {
                localClientsFlow.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load local CRM clients: ${e.message}", e)
            localClientsFlow.value = emptyList()
        }
    }

    private fun saveLocalClients(clients: List<CrmClient>) {
        localClientsFlow.value = clients
        val file = storageFile ?: return
        try {
            val jsonArr = JSONArray()
            clients.forEach { jsonArr.put(it.toJson()) }
            file.writeText(jsonArr.toString())
        } catch (e: Exception) {
            Log.e(tag, "Failed to save local CRM clients: ${e.message}", e)
        }
    }

    fun observeClients(): Flow<List<CrmClient>> {
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            return callbackFlow {
                val collectionRef = db.collection("users").document(uid).collection("clients")
                val registration = collectionRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Firestore observe clients error: ${error.message}")
                        trySend(localClientsFlow.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val clients = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { CrmClient.fromMap(doc.id, it) }
                        }
                        if (clients.isNotEmpty()) {
                            saveLocalClients(clients)
                            trySend(clients)
                        } else {
                            trySend(localClientsFlow.value)
                        }
                    }
                }
                awaitClose { registration.remove() }
            }
        }

        return localClientsFlow.asStateFlow()
    }

    suspend fun upsertClient(client: CrmClient): Result<String> = withContext(Dispatchers.IO) {
        val existing = localClientsFlow.value.toMutableList()
        val index = existing.indexOfFirst {
            it.id == client.id ||
                (client.email.isNotBlank() && it.email.equals(client.email, ignoreCase = true)) ||
                (client.websiteUrl.isNotBlank() && it.websiteUrl.equals(client.websiteUrl, ignoreCase = true))
        }

        val finalClient = if (index != -1) {
            val old = existing[index]
            client.copy(
                id = old.id,
                seoReport = if (client.seoReport.isNotBlank()) client.seoReport else old.seoReport,
                pitchMessage = if (client.pitchMessage.isNotBlank()) client.pitchMessage else old.pitchMessage,
                leadScore = if (client.leadScore > 0) client.leadScore else old.leadScore,
                pipelineStage = if (client.pipelineStage.isNotBlank()) client.pipelineStage else old.pipelineStage,
                lastContactedAt = if (client.lastContactedAt > 0L) client.lastContactedAt else old.lastContactedAt,
                nextFollowUpAt = if (client.nextFollowUpAt > 0L) client.nextFollowUpAt else old.nextFollowUpAt,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            client
        }

        if (index != -1) {
            existing[index] = finalClient
        } else {
            existing.add(0, finalClient)
        }
        saveLocalClients(existing)

        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            try {
                db.collection("users").document(uid).collection("clients")
                    .document(finalClient.id)
                    .set(finalClient.toMap())
                    .await()
            } catch (e: Exception) {
                Log.w(tag, "Firestore client sync skipped or failed: ${e.message}")
            }
        }

        Result.success(finalClient.id)
    }

    suspend fun updateClientStatus(clientId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        val validStatus = when (newStatus.lowercase()) {
            "paid" -> "Paid"
            "completed" -> "Completed"
            "interested" -> "Interested"
            "followup", "follow-up" -> "FollowUp"
            else -> "Pitched"
        }

        val existing = localClientsFlow.value.toMutableList()
        val index = existing.indexOfFirst { it.id == clientId }
        if (index != -1) {
            val updated = existing[index].copy(
                status = validStatus,
                pipelineStage = validStatus,
                updatedAt = System.currentTimeMillis(),
                lastContactedAt = if (validStatus != "Pitched") System.currentTimeMillis() else existing[index].lastContactedAt,
                nextFollowUpAt = when (validStatus) {
                    "Interested" -> System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 7
                    "FollowUp" -> System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 3
                    else -> existing[index].nextFollowUpAt
                }
            )
            existing[index] = updated
            saveLocalClients(existing)

            val uid = auth?.currentUser?.uid
            if (db != null && uid != null) {
                try {
                    db.collection("users").document(uid).collection("clients")
                        .document(clientId)
                        .update(
                            mapOf(
                                "status" to validStatus,
                                "pipelineStage" to validStatus,
                                "updatedAt" to System.currentTimeMillis(),
                                "lastContactedAt" to updated.lastContactedAt,
                                "nextFollowUpAt" to updated.nextFollowUpAt
                            )
                        )
                        .await()
                } catch (e: Exception) {
                    Log.w(tag, "Firestore status update failed: ${e.message}")
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Client $clientId not found"))
        }
    }

    suspend fun updateClientReport(clientId: String, report: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = localClientsFlow.value.toMutableList()
        val index = existing.indexOfFirst { it.id == clientId }
        if (index != -1) {
            val updated = existing[index].copy(seoReport = report, updatedAt = System.currentTimeMillis())
            existing[index] = updated
            saveLocalClients(existing)

            val uid = auth?.currentUser?.uid
            if (db != null && uid != null) {
                try {
                    db.collection("users").document(uid).collection("clients")
                        .document(clientId)
                        .update("seoReport", report, "updatedAt", System.currentTimeMillis())
                        .await()
                } catch (e: Exception) {
                    Log.w(tag, "Firestore report update failed: ${e.message}")
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Client $clientId not found"))
        }
    }

    suspend fun deleteClient(clientId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = localClientsFlow.value.toMutableList()
        val index = existing.indexOfFirst { it.id == clientId }
        if (index != -1) {
            existing.removeAt(index)
            saveLocalClients(existing)

            val uid = auth?.currentUser?.uid
            if (db != null && uid != null) {
                try {
                    db.collection("users").document(uid).collection("clients")
                        .document(clientId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.w(tag, "Firestore client delete failed: ${e.message}")
                }
            }
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Client $clientId not found"))
        }
    }
}
