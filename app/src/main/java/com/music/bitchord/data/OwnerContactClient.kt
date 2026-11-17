package com.music.bitchord.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Sends a support note to the app backend. The Discord webhook stays on the
 * server; this client knows only the public contact endpoint.
 */
class OwnerContactClient(
    private val endpoint: String = ENDPOINT,
    private val client: OkHttpClient = Http.client,
) {
    enum class Failure { NOT_CONFIGURED, RATE_LIMITED, NETWORK, REJECTED }

    data class DeliveryResult(
        val delivered: Boolean,
        val failure: Failure? = null,
    )

    suspend fun send(type: String, message: String): DeliveryResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("type", type)
            .put("message", message)
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url(endpoint)
            .post(payload)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    DeliveryResult(delivered = true)
                } else {
                    val failure = when (response.code) {
                        503 -> Failure.NOT_CONFIGURED
                        429 -> Failure.RATE_LIMITED
                        else -> Failure.REJECTED
                    }
                    DeliveryResult(delivered = false, failure = failure)
                }
            }
        } catch (_: Exception) {
            DeliveryResult(delivered = false, failure = Failure.NETWORK)
        }
    }

    companion object {
        const val ENDPOINT = "https://api.bitchord.kushagrasingh.in/api/contact"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
