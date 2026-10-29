package com.example.data.service

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

data class AbVariantStats(
    val variantId: String,
    val sentCount: Int = 0,
    val replyCount: Int = 0
) {
    val replyRate: Double
        get() = if (sentCount > 0) (replyCount.toDouble() / sentCount.toDouble()) * 100.0 else 0.0

    fun toJson(): JSONObject = JSONObject().apply {
        put("variantId", variantId)
        put("sentCount", sentCount)
        put("replyCount", replyCount)
    }

    companion object {
        fun fromJson(json: JSONObject): AbVariantStats = AbVariantStats(
            variantId = json.optString("variantId", "VARIANT_A"),
            sentCount = json.optInt("sentCount", 0),
            replyCount = json.optInt("replyCount", 0)
        )

        fun fromMap(map: Map<String, Any>): AbVariantStats = AbVariantStats(
            variantId = map["variantId"] as? String ?: "VARIANT_A",
            sentCount = (map["sentCount"] as? Number)?.toInt() ?: 0,
            replyCount = (map["replyCount"] as? Number)?.toInt() ?: 0
        )
    }
}

class AbTestingEngine(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
) {
    private val tag = "AbTestingEngine"
    private val storageFile: File? = context?.filesDir?.let { File(it, "ab_testing_stats.json") }

    private var statsA = AbVariantStats("VARIANT_A")
    private var statsB = AbVariantStats("VARIANT_B")

    init {
        loadLocalStats()
    }

    private fun loadLocalStats() {
        val file = storageFile ?: return
        if (!file.exists()) return
        try {
            val content = file.readText()
            if (content.isNotBlank()) {
                val json = JSONObject(content)
                val jsonA = json.optJSONObject("VARIANT_A")
                val jsonB = json.optJSONObject("VARIANT_B")
                if (jsonA != null) statsA = AbVariantStats.fromJson(jsonA)
                if (jsonB != null) statsB = AbVariantStats.fromJson(jsonB)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load local A/B stats: ${e.message}")
        }
    }

    private fun saveLocalStats() {
        val file = storageFile ?: return
        try {
            val json = JSONObject().apply {
                put("VARIANT_A", statsA.toJson())
                put("VARIANT_B", statsB.toJson())
            }
            file.writeText(json.toString())
        } catch (e: Exception) {
            Log.w(tag, "Failed to save local A/B stats: ${e.message}")
        }
    }

    /**
     * Epsilon-greedy multi-armed bandit algorithm:
     * 75% of the time, pick the winning variant if sufficient sample size (> 5 sends).
     * 25% of the time, explore the other variant.
     */
    fun selectWinningVariant(): String {
        val totalSends = statsA.sentCount + statsB.sentCount
        if (totalSends < 6) {
            return if (Random.nextBoolean()) "VARIANT_A" else "VARIANT_B"
        }

        // 25% random exploration
        if (Random.nextDouble() < 0.25) {
            return if (Random.nextBoolean()) "VARIANT_A" else "VARIANT_B"
        }

        // 75% exploit the winner
        return if (statsA.replyRate >= statsB.replyRate) "VARIANT_A" else "VARIANT_B"
    }

    fun selectVariant(): String = selectWinningVariant()

    suspend fun recordSend(variantId: String) = withContext(Dispatchers.IO) {
        if (variantId == "VARIANT_A") {
            statsA = statsA.copy(sentCount = statsA.sentCount + 1)
        } else {
            statsB = statsB.copy(sentCount = statsB.sentCount + 1)
        }
        saveLocalStats()
        syncToCloud()
    }

    suspend fun recordReply(variantId: String) = withContext(Dispatchers.IO) {
        if (variantId == "VARIANT_A") {
            statsA = statsA.copy(replyCount = statsA.replyCount + 1)
        } else {
            statsB = statsB.copy(replyCount = statsB.replyCount + 1)
        }
        saveLocalStats()
        syncToCloud()
    }

    fun getStats(): Pair<AbVariantStats, AbVariantStats> = Pair(statsA, statsB)

    private suspend fun syncToCloud() {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        if (firestore != null && uid != null) {
            try {
                firestore.collection("users").document(uid).collection("ab_testing").document("stats")
                    .set(
                        mapOf(
                            "variantA_sends" to statsA.sentCount,
                            "variantA_replies" to statsA.replyCount,
                            "variantA_rate" to statsA.replyRate,
                            "variantB_sends" to statsB.sentCount,
                            "variantB_replies" to statsB.replyCount,
                            "variantB_rate" to statsB.replyRate,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
            } catch (e: Exception) {
                Log.w(tag, "Failed to sync A/B test stats to Firestore: ${e.message}")
            }
        }
    }
}
