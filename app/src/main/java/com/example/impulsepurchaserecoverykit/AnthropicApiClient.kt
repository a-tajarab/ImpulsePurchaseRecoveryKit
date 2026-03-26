package com.example.impulsepurchaserecoverykit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AnthropicApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5-20251001"

    suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        messages: List<Pair<String, String>> // role to content
    ): String = withContext(Dispatchers.IO) {

        val messagesArray = JSONArray()
        messages.forEach { (role, content) ->
            messagesArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("content", content)
                }
            )
        }

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1024)
            put("system", systemPrompt)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("API error ${response.code}: $responseBody")
        }

        val json = JSONObject(responseBody)
        json.getJSONArray("content")
            .getJSONObject(0)
            .getString("text")
    }
}