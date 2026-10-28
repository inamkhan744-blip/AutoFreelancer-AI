package com.example.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class EmailAttachment(
    val filename: String,
    val contentBase64: String
)

data class EmailSendResult(
    val emailId: String,
    val recipient: String,
    val subject: String
)

class BrevoEmailService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "BrevoEmailService"
    private val emailEndpoint = "https://api.brevo.com/v3/smtp/email"
    private val accountEndpoint = "https://api.brevo.com/v3/account"

    suspend fun sendEmail(
        apiKey: String,
        toEmail: String,
        subject: String,
        bodyText: String,
        fromEmail: String = VERIFIED_FROM_EMAIL,
        attachments: List<EmailAttachment> = emptyList()
    ): Result<EmailSendResult> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanTo = toEmail.trim()
        val cleanFrom = fromEmail.trim()

        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Brevo API key is not configured. Add your Brevo API key in Settings.")
            )
        }
        if (!EMAIL_PATTERN.matches(cleanTo)) {
            return@withContext Result.failure(IllegalArgumentException("A valid recipient email address is required."))
        }
        if (!EMAIL_PATTERN.matches(cleanFrom)) {
            return@withContext Result.failure(IllegalArgumentException("A valid verified Brevo sender email is required."))
        }

        try {
            val payload = JSONObject().apply {
                put("sender", JSONObject().put("name", "AutoFreelancer AI").put("email", cleanFrom))
                put("to", JSONArray().put(JSONObject().put("email", cleanTo)))
                put("subject", subject.ifBlank { "SEO Audit & Growth Proposal" })
                put("textContent", bodyText)
                if (attachments.isNotEmpty()) {
                    put("attachment", JSONArray().apply {
                        attachments.forEach { attachment ->
                            put(JSONObject()
                                .put("name", attachment.filename)
                                .put("content", attachment.contentBase64))
                        }
                    })
                }
            }

            val request = Request.Builder()
                .url(emailEndpoint)
                .addHeader("api-key", cleanKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = try {
                        JSONObject(responseBody).optString("message").ifBlank { "HTTP ${response.code}: $responseBody" }
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $responseBody"
                    }
                    Log.e(tag, "Brevo API error: $message")
                    return@withContext Result.failure(Exception("Brevo error: $message"))
                }

                val messageId = JSONObject(responseBody).optString("messageId")
                Log.i(tag, "Email dispatched via Brevo: messageId=$messageId recipient=$cleanTo")
                Result.success(
                    EmailSendResult(
                        emailId = messageId,
                        recipient = cleanTo,
                        subject = subject
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Brevo request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a Brevo API key first."))
        }

        try {
            val request = Request.Builder()
                .url(accountEndpoint)
                .addHeader("api-key", cleanKey)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success("Connected to Brevo successfully.")
                } else {
                    val message = try {
                        JSONObject(body).optString("message").ifBlank { "HTTP ${response.code}: $body" }
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $body"
                    }
                    Result.failure(Exception("Brevo connection failed: $message"))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Brevo connection check failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        const val VERIFIED_FROM_EMAIL = "inamkhan744@gmail.com"
        private val EMAIL_PATTERN = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
    }
}