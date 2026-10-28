package com.example.data.service

import android.content.Context
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

data class GeneratedVoiceNote(
    val file: File,
    val filePath: String,
    val base64Content: String,
    val fileName: String,
    val fileSizeKb: Long
)

class ElevenLabsVoiceService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "ElevenLabsVoiceService"

    companion object {
        const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM" // Rachel
        const val ADAM_VOICE_ID = "pNInz6obpgDQGcFmaJgB"    // Adam
        const val DREW_VOICE_ID = "29vD33N1CtxCmqQRPOHJ"    // Drew
    }

    /**
     * Converts pitch or message text into an MP3 audio file via ElevenLabs API.
     * Endpoint: POST https://api.elevenlabs.io/v1/text-to-speech/{voice_id}
     */
    suspend fun textToSpeech(
        context: Context,
        apiKey: String,
        text: String,
        voiceId: String = DEFAULT_VOICE_ID,
        outputFileName: String? = null
    ): Result<GeneratedVoiceNote> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("ElevenLabs API Key is not configured in Settings.")
            )
        }

        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Cannot generate voice note from empty text.")
            )
        }

        // Limit text length to avoid credit waste on demo accounts (max ~1000 chars for a 60-second pitch voice note)
        val textForAudio = if (cleanText.length > 1200) {
            cleanText.take(1200) + "..."
        } else {
            cleanText
        }

        val targetVoice = voiceId.trim().ifBlank { DEFAULT_VOICE_ID }
        val endpoint = "https://api.elevenlabs.io/v1/text-to-speech/$targetVoice"

        try {
            val jsonBody = JSONObject().apply {
                put("text", textForAudio)
                put("model_id", "eleven_multilingual_v2")
                val voiceSettings = JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                }
                put("voice_settings", voiceSettings)
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("xi-api-key", cleanKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "audio/mpeg")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    val msg = try {
                        JSONObject(errBody).optJSONObject("detail")?.optString("message")
                            ?: JSONObject(errBody).optString("message", errBody)
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $errBody"
                    }
                    Log.e(tag, "ElevenLabs TTS error: $msg")
                    return@withContext Result.failure(Exception("ElevenLabs error: $msg"))
                }

                val audioBytes = response.body?.bytes()
                    ?: return@withContext Result.failure(Exception("ElevenLabs returned empty audio body"))

                val dir = File(context.filesDir, "voice_notes").apply { mkdirs() }
                val fname = outputFileName ?: "pitch_voice_note_${UUID.randomUUID().toString().take(8)}.mp3"
                val audioFile = File(dir, fname)

                FileOutputStream(audioFile).use { fos ->
                    fos.write(audioBytes)
                }

                val base64Data = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                val sizeKb = audioFile.length() / 1024

                Log.i(tag, "ElevenLabs voice note created: ${audioFile.absolutePath} ($sizeKb KB)")

                Result.success(
                    GeneratedVoiceNote(
                        file = audioFile,
                        filePath = audioFile.absolutePath,
                        base64Content = base64Data,
                        fileName = fname,
                        fileSizeKb = sizeKb
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "ElevenLabs request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies the ElevenLabs API key by querying /v1/user or /v1/voices.
     */
    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter an ElevenLabs API Key first."))
        }

        try {
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/user")
                .addHeader("xi-api-key", cleanKey)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val obj = JSONObject(body)
                    val subscription = obj.optJSONObject("subscription")
                    val characterCount = subscription?.optInt("character_count", 0) ?: 0
                    val characterLimit = subscription?.optInt("character_limit", 10000) ?: 10000
                    Result.success("Connected! ElevenLabs active ($characterCount / $characterLimit chars used)")
                } else {
                    Result.failure(Exception("ElevenLabs authentication failed (HTTP ${response.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
