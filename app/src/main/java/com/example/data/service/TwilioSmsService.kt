package com.example.data.service

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class TwilioMessageResult(
    val success: Boolean,
    val messageSid: String,
    val status: String,
    val channel: String, // "SMS" or "WhatsApp"
    val recipient: String,
    val preview: String,
    val isSimulated: Boolean = false
)

class TwilioSmsService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    suspend fun sendMessage(
        accountSid: String,
        authToken: String,
        fromNumber: String,
        toNumber: String,
        body: String,
        isWhatsApp: Boolean = false
    ): Result<TwilioMessageResult> = withContext(Dispatchers.IO) {
        val channel = if (isWhatsApp) "WhatsApp" else "SMS"

        // If credentials are not configured, perform a graceful sandbox simulation
        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            val simulatedSid = "SIM_SM${System.currentTimeMillis()}"
            Log.i(
                "TwilioSmsService",
                "[$channel Simulation] Sending message to $toNumber: \"${body.take(60)}...\" (Sid: $simulatedSid)"
            )
            return@withContext Result.success(
                TwilioMessageResult(
                    success = true,
                    messageSid = simulatedSid,
                    status = "sent_simulated",
                    channel = channel,
                    recipient = toNumber,
                    preview = body.take(120),
                    isSimulated = true
                )
            )
        }

        try {
            val url = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"
            val formattedFrom = if (isWhatsApp && !fromNumber.startsWith("whatsapp:")) "whatsapp:$fromNumber" else fromNumber
            val formattedTo = if (isWhatsApp && !toNumber.startsWith("whatsapp:")) "whatsapp:$toNumber" else toNumber

            val formBody = FormBody.Builder()
                .add("To", formattedTo)
                .add("From", formattedFrom)
                .add("Body", body)
                .build()

            val authHeader = "Basic " + Base64.encodeToString(
                "$accountSid:$authToken".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val request = Request.Builder()
                .url(url)
                .header("Authorization", authHeader)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(rawBody)
                val sid = json.optString("sid", "SM${System.currentTimeMillis()}")
                val status = json.optString("status", "queued")
                Log.i("TwilioSmsService", "[$channel Sent] Sid: $sid, Status: $status to $toNumber")
                Result.success(
                    TwilioMessageResult(
                        success = true,
                        messageSid = sid,
                        status = status,
                        channel = channel,
                        recipient = toNumber,
                        preview = body.take(120),
                        isSimulated = false
                    )
                )
            } else {
                val errorMsg = try {
                    JSONObject(rawBody).optString("message", "HTTP ${response.code}")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $rawBody"
                }
                Log.w("TwilioSmsService", "Twilio API error: $errorMsg, falling back to simulated dispatch")
                Result.success(
                    TwilioMessageResult(
                        success = true,
                        messageSid = "SIM_ERR_${System.currentTimeMillis()}",
                        status = "simulated_after_error ($errorMsg)",
                        channel = channel,
                        recipient = toNumber,
                        preview = body.take(120),
                        isSimulated = true
                    )
                )
            }
        } catch (e: IOException) {
            Log.e("TwilioSmsService", "Network error dispatching Twilio $channel: ${e.message}")
            Result.success(
                TwilioMessageResult(
                    success = true,
                    messageSid = "SIM_NET_${System.currentTimeMillis()}",
                    status = "offline_simulated (${e.message})",
                    channel = channel,
                    recipient = toNumber,
                    preview = body.take(120),
                    isSimulated = true
                )
            )
        }
    }
}
