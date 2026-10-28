package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.InboxEmail
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

class InboxRepository(
    private val db: FirebaseFirestore? = null,
    private val context: Context? = null
) {
    private val tag = "InboxRepository"
    private val auth by lazy { runCatching { Firebase.auth }.getOrNull() }
    private val localInboxFlow = MutableStateFlow<List<InboxEmail>>(emptyList())
    private val storageFile: File? = context?.filesDir?.let { File(it, "autofreelancer_inbox.json") }

    constructor(context: Context) : this(db = null, context = context.applicationContext)
    constructor(db: FirebaseFirestore) : this(db = db, context = null)

    init {
        loadLocalInbox()
    }

    private fun loadLocalInbox() {
        val file = storageFile ?: return
        if (!file.exists()) {
            localInboxFlow.value = emptyList()
            return
        }
        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                val jsonArr = JSONArray(content)
                val list = mutableListOf<InboxEmail>()
                for (i in 0 until jsonArr.length()) {
                    list.add(InboxEmail.fromJson(jsonArr.getJSONObject(i)))
                }
                localInboxFlow.value = list
            } else {
                localInboxFlow.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load local inbox emails: ${e.message}", e)
            localInboxFlow.value = emptyList()
        }
    }

    private fun saveLocalInbox(emails: List<InboxEmail>) {
        localInboxFlow.value = emails
        val file = storageFile ?: return
        try {
            val jsonArr = JSONArray()
            emails.forEach { jsonArr.put(it.toJson()) }
            file.writeText(jsonArr.toString())
        } catch (e: Exception) {
            Log.e(tag, "Failed to save local inbox emails: ${e.message}", e)
        }
    }

    fun observeInbox(): Flow<List<InboxEmail>> {
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            return callbackFlow {
                val collectionRef = db.collection("users").document(uid).collection("inbox")
                val registration = collectionRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Firestore observe inbox error: ${error.message}")
                        trySend(localInboxFlow.value)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val emails = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { InboxEmail.fromMap(doc.id, it) }
                        }
                        if (emails.isNotEmpty()) {
                            saveLocalInbox(emails)
                            trySend(emails)
                        } else {
                            trySend(localInboxFlow.value)
                        }
                    }
                }
                awaitClose { registration.remove() }
            }
        }

        return localInboxFlow.asStateFlow()
    }

    suspend fun upsertMessage(message: InboxEmail): Result<String> = withContext(Dispatchers.IO) {
        val existing = localInboxFlow.value.toMutableList()
        val index = existing.indexOfFirst { it.id == message.id }

        if (index != -1) {
            existing[index] = message.copy(updatedAt = System.currentTimeMillis())
        } else {
            existing.add(0, message)
        }
        saveLocalInbox(existing)

        // Sync to Firestore
        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            try {
                db.collection("users").document(uid).collection("inbox")
                    .document(message.id)
                    .set(message.toMap().filterValues { it != null })
                    .await()
            } catch (e: Exception) {
                Log.w(tag, "Firestore inbox sync error: ${e.message}")
            }
        }

        Result.success(message.id)
    }

    suspend fun updateDraftedReply(messageId: String, newDraft: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = localInboxFlow.value.toMutableList()
        val index = existing.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val updated = existing[index].copy(
                draftedReply = newDraft,
                updatedAt = System.currentTimeMillis()
            )
            existing[index] = updated
            saveLocalInbox(existing)

            val uid = auth?.currentUser?.uid
            if (db != null && uid != null) {
                try {
                    db.collection("users").document(uid).collection("inbox")
                        .document(messageId)
                        .set(updated.toMap().filterValues { it != null })
                        .await()
                } catch (e: Exception) {
                    Log.w(tag, "Firestore update draft error: ${e.message}")
                }
            }
        }
        Result.success(Unit)
    }

    suspend fun markApprovedAndSent(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = localInboxFlow.value.toMutableList()
        val index = existing.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val updated = existing[index].copy(
                status = "Approved & Sent",
                sentAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            existing[index] = updated
            saveLocalInbox(existing)

            val uid = auth?.currentUser?.uid
            if (db != null && uid != null) {
                try {
                    db.collection("users").document(uid).collection("inbox")
                        .document(messageId)
                        .set(updated.toMap().filterValues { it != null })
                        .await()
                } catch (e: Exception) {
                    Log.w(tag, "Firestore mark sent error: ${e.message}")
                }
            }
        }
        Result.success(Unit)
    }

    suspend fun dismissMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = localInboxFlow.value.toMutableList()
        val index = existing.indexOfFirst { it.id == messageId }
        if (index != -1) {
            val updated = existing[index].copy(
                status = "Dismissed",
                updatedAt = System.currentTimeMillis()
            )
            existing[index] = updated
            saveLocalInbox(existing)

            val uid = auth?.currentUser?.uid
            if (db != null && uid != null) {
                try {
                    db.collection("users").document(uid).collection("inbox")
                        .document(messageId)
                        .set(updated.toMap().filterValues { it != null })
                        .await()
                } catch (e: Exception) {
                    Log.w(tag, "Firestore dismiss error: ${e.message}")
                }
            }
        }
        Result.success(Unit)
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = localInboxFlow.value.toMutableList()
        existing.removeAll { it.id == messageId }
        saveLocalInbox(existing)

        val uid = auth?.currentUser?.uid
        if (db != null && uid != null) {
            try {
                db.collection("users").document(uid).collection("inbox")
                    .document(messageId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.w(tag, "Firestore delete message error: ${e.message}")
            }
        }
        Result.success(Unit)
    }
}
