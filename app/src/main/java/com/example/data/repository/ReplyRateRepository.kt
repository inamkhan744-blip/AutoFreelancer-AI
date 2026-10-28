package com.example.data.repository

import com.example.data.model.OutreachTone
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class ReplyRateRepository(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val auth: FirebaseAuth? = runCatching { Firebase.auth }.getOrNull()
) {
    suspend fun bestPerformingTone(): OutreachTone {
        val collection = userEvents() ?: return OutreachTone.PROFESSIONAL
        return runCatching {
            val replyWindowCutoff = System.currentTimeMillis() - REPLY_WINDOW_MILLIS
            val outcomes = collection
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(MAX_OUTCOME_HISTORY)
                .get().await().documents.mapNotNull { document ->
                val tone = OutreachTone.values().firstOrNull { it.name == document.getString("tone") }
                val replied = document.getBoolean("replied")
                val sentAt = document.getTimestamp("sentAt")?.toDate()?.time
                if (tone == null || replied == null || sentAt == null || (!replied && sentAt > replyWindowCutoff)) {
                    null
                } else {
                    tone to replied
                }
            }
            bestToneFromOutcomes(outcomes)
        }.getOrDefault(OutreachTone.PROFESSIONAL)
    }

    suspend fun recordSentPitch(recipientEmail: String, tone: OutreachTone) {
        val collection = userEvents() ?: return
        if (!EMAIL_PATTERN.matches(recipientEmail.trim())) return
        runCatching {
            collection.document().set(
                mapOf(
                    "recipientHash" to hashEmail(recipientEmail),
                    "tone" to tone.name,
                    "replied" to false,
                    "sentAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun recordInboundReply(senderEmail: String) {
        val collection = userEvents() ?: return
        if (!EMAIL_PATTERN.matches(senderEmail.trim())) return
        runCatching {
            val event = collection
                .whereEqualTo("recipientHash", hashEmail(senderEmail))
                .get().await()
                .documents
                .filter { it.getBoolean("replied") == false }
                .maxByOrNull { it.getTimestamp("sentAt")?.toDate()?.time ?: 0L }
            event?.reference?.update(
                mapOf(
                    "replied" to true,
                    "repliedAt" to FieldValue.serverTimestamp()
                )
            )?.await()
        }
    }

    private fun userEvents() = auth?.currentUser?.uid?.let { uid ->
        firestore?.collection("users")?.document(uid)?.collection("reply_rate_events")
    }

    private fun hashEmail(email: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(email.trim().lowercase().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val REPLY_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val MAX_OUTCOME_HISTORY = 300L
        private val EMAIL_PATTERN = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)

        fun bestToneFromOutcomes(outcomes: List<Pair<OutreachTone, Boolean>>): OutreachTone {
            if (outcomes.isEmpty()) return OutreachTone.PROFESSIONAL
            val rates: Map<OutreachTone, Double?> = OutreachTone.values().toList().associate { tone: OutreachTone ->
                val toneOutcomes: List<Pair<OutreachTone, Boolean>> = outcomes.filter { outcome -> outcome.first == tone }
                if (toneOutcomes.isEmpty()) null else toneOutcomes.count { it.second }.toDouble() / toneOutcomes.size
            }
            val bestRate = rates.values.filterNotNull().maxOrNull() ?: return OutreachTone.PROFESSIONAL
            val leaders: List<OutreachTone> = rates.filterValues { rate -> rate == bestRate }.keys.toList()
            return if (OutreachTone.PROFESSIONAL in leaders) OutreachTone.PROFESSIONAL else leaders.first()
        }
    }
}