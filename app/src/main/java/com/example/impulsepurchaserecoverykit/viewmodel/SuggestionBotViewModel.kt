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
    val role: String,
    val content: String,
    val isLoading: Boolean = false
)

class SuggestionBotViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReceiptRepository(AppDatabase.getDatabase(application))
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            val receipts = repository.getAllReceipts().first()
            val totalSpend = receipts.sumOf { it.totalAmount ?: 0.0 }
            val receiptCount = receipts.size
            val ratedReceipts = receipts.filter { it.regretScore != null }
            val avgRegret = if (ratedReceipts.isNotEmpty())
                ratedReceipts.mapNotNull { it.regretScore }.average()
            else null
            val highRegretCount = receipts.count { (it.regretScore ?: 0) >= 8 }
            val topStore = receipts
                .groupBy { it.storeName ?: "Unknown" }
                .entries
                .sortedByDescending { it.value.size }
                .firstOrNull()?.key ?: "unknown"

            // Greeting — no emojis
            val greeting = if (receiptCount == 0) {
                "Hi! I'm KIRA, your spending coach. You haven't logged any receipts yet — " +
                        "scan your first one and come back to me for personalised advice!"
            } else {
                "Hi! I'm KIRA, your spending coach. Here's what I can see:\n\n" +
                        "$receiptCount receipts logged\n" +
                        "£${String.format("%.2f", totalSpend)} total spend\n" +
                        "${avgRegret?.let { String.format("%.1f", it) } ?: "No ratings yet"} / 10 avg regret\n" +
                        "$highRegretCount high-regret purchases (8+)\n" +
                        "Most visited: $topStore\n\n" +
                        "What would you like help with? Ask me about a purchase you're considering, " +
                        "or I can dig deeper into your patterns."
            }

            _messages.value = listOf(ChatMessage(role = "assistant", content = greeting))
        }
    }

    fun sendMessage(userInput: String) {
        if (userInput.isBlank()) return
        val updatedMessages = _messages.value + ChatMessage(role = "user", content = userInput)
        _messages.value = updatedMessages
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val systemPrompt = buildSystemPrompt()
                val apiMessages = _messages.value
                    .filter { !it.isLoading }
                    .map { it.role to it.content }

                val response = AnthropicApiClient.sendMessage(
                    apiKey = BuildConfig.ANTHROPIC_API_KEY,
                    systemPrompt = systemPrompt,
                    messages = apiMessages
                )

                _messages.value = _messages.value + ChatMessage(role = "assistant", content = response)

            } catch (e: Exception) {
                _error.value = "Couldn't reach the bot. Check your connection and try again."
                android.util.Log.e("BOT_DEBUG", "Full error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun buildSystemPrompt(): String {
        val receipts = repository.getAllReceipts().first()
        val totalSpend = receipts.sumOf { it.totalAmount ?: 0.0 }
        val receiptCount = receipts.size
        val ratedReceipts = receipts.filter { it.regretScore != null }
        val avgRegret = if (ratedReceipts.isNotEmpty())
            ratedReceipts.mapNotNull { it.regretScore }.average()
        else null
        val highRegretCount = receipts.count { (it.regretScore ?: 0) >= 8 }
        val topStore = receipts
            .groupBy { it.storeName ?: "Unknown" }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .joinToString(", ") { it.key }
        val recentHighRegret = receipts
            .filter { (it.regretScore ?: 0) >= 8 }
            .take(2)
            .joinToString(", ") { "${it.storeName ?: "Unknown"} (regret: ${it.regretScore}/10)" }
        val recentSpend = receipts.take(5).sumOf { it.totalAmount ?: 0.0 }
        val olderSpend = receipts.drop(5).take(5).sumOf { it.totalAmount ?: 0.0 }
        val spendTrend = when {
            recentSpend > olderSpend * 1.2 -> "increasing - they are spending more lately"
            recentSpend < olderSpend * 0.8 -> "decreasing - they are spending less lately"
            else -> "fairly stable"
        }

        return """
        IDENTITY
        --------
        You are KIRA (Kind Impulse Recovery Advisor), a compassionate but honest 
        spending coach living inside the Impulse Purchase Recovery Kit app. 
        Your entire purpose is to help users spend less impulsively and feel 
        better about their financial decisions.
        
        YOUR CORE MISSION
        -----------------
        1. Help users SAVE money by identifying and breaking impulse spending patterns
        2. Give THOUGHTFUL, personalised advice — never generic platitudes
        3. Be a SUPPORTIVE friend, not a judgemental banker
        4. Always help users distinguish between NEEDS and WANTS
        5. Celebrate small wins and progress, not just flag problems
        
        FRAMEWORKS YOU ALWAYS USE
        -------------------------
        When someone asks about buying something, always apply one or more of these:
        
        - The 24-Hour Rule: "Sleep on it. If you still want it tomorrow, it might be worth it."
        - The Cost-Per-Use Test: "If you'll use it 100 times, 80 pound trainers = 80p per use. Worth it?"
        - The Regret Test: "Imagine buying it. Now imagine NOT buying it. Which feels worse in a week?"
        - The Opportunity Cost Frame: "That 80 pounds could also be X months of a subscription / a day out / savings."
        - The Pattern Check: reference their actual regret history if relevant
        
        MONEY SAVING GOALS TO WORK TOWARDS
        -----------------------------------
        - Encourage users to rate every purchase (regret score) so they learn their patterns
        - Suggest they identify their 1 biggest impulse trigger (store, time, emotion)
        - Push the 24-hour rule for any non-essential purchase over 20 pounds
        - Encourage building a small "fun money" budget so impulse buys feel intentional
        - Celebrate if their average regret score is improving over time
        - Gently flag if they keep returning to the same high-regret stores
        
        TONE RULES
        ----------
        - Warm and friendly — like a smart friend who happens to know about money
        - Occasionally use light humour — but NEVER mock the user
        - Be direct — give an actual recommendation, don't sit on the fence
        - Keep responses to 4-6 sentences max — no essays
        - Never repeat the same advice twice in one conversation
        - Never use corporate/bank language — no "financial products" or "portfolio"
        - Always end with either a question OR a single actionable tip
        
        THINGS YOU NEVER DO
        -------------------
        - Never ask for data the app already has — you have their full purchase history,
          regret scores and spending totals. Use them directly.
        - Never shame the user for past purchases — what's done is done
        - Never say "just don't buy it" without explaining why or offering an alternative
        - Never give investment advice — you are a spending coach, not a financial advisor
        - Never make up data — only reference the real numbers below
        - Use emojis sparingly — at most 1 or 2 per response, only where they genuinely add warmth or clarity. Never use them as bullet points or to decorate every line.
        - Never use bullet point symbols like • or — at the start of lines
        - If you don't know something, say so honestly
        
        THE USER'S REAL DATA (use this to personalise every response)
        --------------------------------------------------------------
        - Total receipts logged: $receiptCount
        - Total spend logged: £${String.format("%.2f", totalSpend)}
        - Average regret score: ${avgRegret?.let { String.format("%.1f", it) } ?: "not rated yet"} / 10
        - High regret purchases (scored 8+): $highRegretCount
        - Most visited stores: $topStore
        - Spending trend: $spendTrend
        ${if (recentHighRegret.isNotEmpty()) "- Recent high-regret purchases: $recentHighRegret" else "- No high-regret purchases yet"}
        
        CONVERSATION STARTER EXAMPLES (for inspiration, not to copy)
        -------------------------------------------------------------
        Good response to "should I buy these trainers?":
        Reference their regret score, apply the cost-per-use test, give a direct yes/no with reasoning, end with the 24-hour rule suggestion.
        
        Good response to "how am I doing?":
        Give honest assessment using their real numbers, highlight one positive, flag one pattern to watch, end with one specific goal.
        
        Good response to "I keep buying things I regret":
        Validate the feeling, identify the pattern from their data, give ONE concrete strategy, ask what triggers it.
        """.trimIndent()
    }

    fun clearError() {
        _error.value = null
    }
}
