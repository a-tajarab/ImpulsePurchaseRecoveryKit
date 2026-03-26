package com.example.impulsepurchaserecoverykit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.impulsepurchaserecoverykit.AnthropicApiClient
import com.example.impulsepurchaserecoverykit.BuildConfig
import com.example.impulsepurchaserecoverykit.database.AppDatabase
import com.example.impulsepurchaserecoverykit.database.ReceiptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ChatMessage(
    val role: String,   // "user" or "assistant"
    val content: String,
    val isLoading: Boolean = false
)

class SuggestionBotViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReceiptRepository(AppDatabase.getDatabase(application))

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "assistant",
                content = "Hi! I'm your spending coach 🤖 Ask me about a purchase you're considering, or how you're doing with your spending this month."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return

        // Add user message immediately
        val updatedMessages = _messages.value + ChatMessage(role = "user", content = userInput)
        _messages.value = updatedMessages
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val systemPrompt = buildSystemPrompt()

                // Build message history for API (exclude loading messages)
                val apiMessages = _messages.value
                    .filter { !it.isLoading }
                    .map { it.role to it.content }

                val response = AnthropicApiClient.sendMessage(
                    apiKey = BuildConfig.ANTHROPIC_API_KEY,
                    systemPrompt = systemPrompt,
                    messages = apiMessages
                )

                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = response
                )

            } catch (e: Exception) {
                _error.value = "Couldn't reach the bot. Check your connection and try again."
                android.util.Log.e("SuggestionBot", "API error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun buildSystemPrompt(): String {
        // Pull real user data from the database to personalise responses
        val receipts = repository.getAllReceipts().first()
        val totalSpend = receipts.sumOf { it.totalAmount ?: 0.0 }
        val ratedReceipts = receipts.filter { it.regretScore != null }
        val avgRegret = if (ratedReceipts.isNotEmpty())
            ratedReceipts.mapNotNull { it.regretScore }.average()
        else null
        val receiptCount = receipts.size
        val highRegretCount = receipts.count { (it.regretScore ?: 0) >= 8 }

        val topStores = receipts
            .groupBy { it.storeName ?: "Unknown" }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .joinToString(", ") { it.key }

        val recentHighRegret = receipts
            .filter { (it.regretScore ?: 0) >= 8 }
            .take(2)
            .joinToString(", ") { "${it.storeName ?: "Unknown"} (regret: ${it.regretScore}/10)" }

        return """
            You are a warm, honest, and slightly humorous spending coach inside an app called 
            the Impulse Purchase Recovery Kit. Your job is to help the user reflect on their 
            spending habits and make more intentional purchases.
            
            Here is the user's real spending data:
            - Total receipts logged: $receiptCount
            - Total spend logged: £${String.format("%.2f", totalSpend)}
            - Average regret score: ${avgRegret?.let { String.format("%.1f", it) } ?: "not rated yet"} / 10
            - High regret purchases (8+): $highRegretCount
            - Most visited stores: $topStores
            ${if (recentHighRegret.isNotEmpty()) "- Recent high-regret purchases: $recentHighRegret" else ""}
            
            Guidelines:
            - Keep responses concise — 3 to 5 sentences max
            - Be supportive and non-judgemental, but honest
            - Reference their actual data when relevant
            - If they ask about a potential purchase, give a direct recommendation
            - Use the 24-hour rule, the 10-minute rule, or the cost-per-use framework when helpful
            - Occasionally use a light touch of humour
            - Never lecture or repeat the same advice twice in a conversation
            - Always end with either a question or a short actionable tip
        """.trimIndent()
    }

    fun clearError() {
        _error.value = null
    }
}
