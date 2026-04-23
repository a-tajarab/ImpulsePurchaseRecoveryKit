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


/**
 * AnthropicApiClient is a singleton object that is responsible for all
 * communication between the IPRK and the Anthropic Claude API
 *
 * The client powers KIRA, Kind Impulse Recovery Advisor, by sending the messages
 * the user's messages along with a system that contains their spending context
 * data to the Claude model, and returning a generated response
 *
 * The client is implemented as a Kotlin singleton object to make sure that
 * only one OkHttpClient instance exists throughout the application lifecycle,
 * enabling connection pooling and reducing unnecessary resource consumption.
 *
 * All network requests are executed on the IO dispatcher using Kotlin coroutines
 * to make sure that the API calls never block the main UI thread, keeping the
 * application responsive during the response generation process.
 *
 * The client uses the claude-haiku model which was selected for its balance
 * of response quality and speed, making it suitable for a conversational
 * assistant in a app
 */
object AnthropicApiClient {

    /**
     * The shared OkHttpClient instance is used for all API requests.
     *
     * It has been configure to have :
     * - 30 second connection timeout: the maximum time to wait when
     *   establishing a connection to the Anthropic API server
     * - 60 second read timeout: the maximum time to wait for the API
     *   to return a response, set higher than the connection timeout,
     *   enough time for the model to generate a reply
     */
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * The Anthropic Messages API endpoint URL
     * All requests are sent to this endpoint using the POST method.
     */
    private const val API_URL = "https://api.anthropic.com/v1/messages"

    /** The Claude model identifier that is used for all KIRA conversations.
    * Claude Haiku was selected for its fast response time and
    * suitability for conversational mobile application use cases.
    */
    private const val MODEL = "claude-haiku-4-5-20251001"

    /**
     * Sends a message to the Anthropic Claude API and returns the generated response.
     *
     * Constructs a JSON request body that has the model identifier, token limit,
     * system prompt, and the full conversation history.
     * The system prompt contains the user's spending context data —
     * including total spend, average regret score, number of receipts, and
     * most visited store — enabling KIRA to provide genuinely personalised responses
     * tailored to the individual user's financial behaviour.
     *
     * The conversation history is passed as a list of role-content pairs where
     * each pair represents one message in the conversation.
     *
     * The request includes three required HTTP headers:
     * - x-api-key: the Anthropic API key for authentication
     * - anthropic-version: the API version string required by Anthropic
     * - content-type: specifies that the request body is JSON
     *
     * The response is parsed from the JSON response body returned by the API.
     * The text content of the first content block is extracted and returned
     * as a plain String for display in the KIRA chat interface.
     *
     */
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