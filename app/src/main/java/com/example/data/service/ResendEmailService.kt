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

class ResendEmailService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    private val tag = "ResendEmailService"
    private val endpoint = "https://api.resend.com/emails"

    /**
     * Sends an email using the Resend REST API (https://api.resend.com/emails).
     * @param apiKey The user's Resend API key (e.g. re_...)
     * @param toEmail The recipient email address
     * @param subject The email subject line
     * @param bodyText The plain text content (or generated proposal)
     * @param fromAddress Sender address; defaults to Resend's verified onboarding sender
     * @param attachments Optional list of file attachments (e.g. ElevenLabs generated audio)
     */
    suspend fun sendEmail(
        apiKey: String,
        toEmail: String,
        subject: String,
        bodyText: String,
        fromAddress: String = "AutoFreelancer AI <onboarding@resend.dev>",
        attachments: List<EmailAttachment> = emptyList()
    ): Result<EmailSendResult> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanTo = toEmail.trim()

        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Resend API Key is not configured. Please add your Resend API Key in Settings.")
            )
        }

        if (cleanTo.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Recipient email address is missing.")
            )
        }

        try {
            val jsonMediaType = "application/json; charset=utf-8".toMediaType()

            val toArray = JSONArray().apply {
                put(cleanTo)
            }

            val requestJson = JSONObject().apply {
                put("from", fromAddress)
                put("to", toArray)
                put("subject", subject.ifBlank { "SEO Audit & Growth Proposal" })
                put("text", bodyText)

                if (attachments.isNotEmpty()) {
                    val attachmentsArray = JSONArray()
                    for (att in attachments) {
                        val attObj = JSONObject().apply {
                            put("filename", att.filename)
                            put("content", att.contentBase64)
                        }
                        attachmentsArray.put(attObj)
                    }
                    put("attachments", attachmentsArray)
                }
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $cleanKey")
                .addHeader("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val obj = JSONObject(body)
                        obj.optString("message").ifBlank {
                            obj.optString("error", "HTTP ${response.code}: $body")
                        }
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $body"
                    }
                    Log.e(tag, "Resend API error: $errorMsg")
                    return@withContext Result.failure(Exception("Resend error: $errorMsg"))
                }

                val jsonResponse = JSONObject(body)
                val id = jsonResponse.optString("id", "")
                Log.i(tag, "Email successfully dispatched via Resend: id=$id to=$cleanTo")

                Result.success(
                    EmailSendResult(
                        emailId = id,
                        recipient = cleanTo,
                        subject = subject
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Resend network request failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifies the Resend API key by sending a test ping to Resend's safe sandbox address.
     */
    suspend fun testConnection(apiKey: String, testEmail: String? = null): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a Resend API Key first."))
        }

        val target = if (!testEmail.isNullOrBlank()) testEmail.trim() else "delivered@resend.dev"
        val res = sendEmail(
            apiKey = apiKey,
            toEmail = target,
            subject = "AutoFreelancer AI - Connection Test",
            bodyText = "Success! Your Resend API key is connected and working correctly with AutoFreelancer AI."
        )

        if (res.isSuccess) {
            val details = res.getOrThrow()
            Result.success("Connected! Test email sent (id: ${details.emailId.take(12)}...)")
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Resend connection failed"))
        }
    }
}
