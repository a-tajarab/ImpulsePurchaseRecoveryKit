package com.example.impulsepurchaserecoverykit

import java.util.Calendar

data class EmotionalResponse(
    val emoji: String,
    val headline: String,
    val message: String,
    val tip: String
)

object EmotionalResponseEngine {

    fun getResponse(
        regretScore: Int,
        storeName: String?,
        impulseLabel: String?,
        emotionalNote: String?
    ): EmotionalResponse {

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val store = storeName?.lowercase().orEmpty()
        val isLateNight = hour in 21..23 || hour in 0..4
        val isOnlineStore = listOf("amazon", "asos", "shein", "ebay", "temu", "nike", "adidas")
            .any { it in store }
        val isFashionStore = listOf("zara", "h&m", "hm", "primark", "next", "matalan", "jd sports")
            .any { it in store }
        val isCafeStore = listOf("starbucks", "costa", "greggs", "pret", "dunkin")
            .any { it in store }

        // Score 10 — maximum regret
        if (regretScore == 10) {
            return EmotionalResponse(
                emoji = "😩",
                headline = "A perfect 10... for regret.",
                message = "You've given this purchase the highest regret score possible. That's actually useful information — your gut is telling you something loud and clear.",
                tip = "Before your next purchase, try the 10-minute rule: if you still want it after 10 minutes of doing something else, it might be worth it."
            )
        }

        // Late night purchase
        if (isLateNight && regretScore >= 8) {
            return EmotionalResponse(
                emoji = "🌙",
                headline = "Late night, loose wallet.",
                message = "Shopping after 9pm hits different, doesn't it? Your tired brain is much more likely to say yes to things your morning self wouldn't.",
                tip = "Try putting your phone on Do Not Disturb after 9pm. Your bank account will thank you."
            )
        }

        // Online store
        if (isOnlineStore && regretScore >= 8) {
            return EmotionalResponse(
                emoji = "📦",
                headline = "The package felt better in the cart.",
                message = "Online shopping makes it incredibly easy to buy on impulse — no queues, no friction, just one tap. The excitement of buying often fades fast.",
                tip = "Use the 24-hour rule: add to cart, then wait a full day before buying. Most of the time, the urge passes."
            )
        }

        // Fashion / clothing store
        if (isFashionStore && regretScore >= 8) {
            return EmotionalResponse(
                emoji = "👗",
                headline = "The tag is still on, isn't it?",
                message = "Fashion stores are designed to make you feel like you need a new look right now. That urgency is manufactured — you were fine before you walked in.",
                tip = "Before buying clothes, ask: 'Do I have three things in my wardrobe this goes with?' If not, it might just sit there."
            )
        }

        // Café / food treat
        if (isCafeStore && regretScore >= 8) {
            return EmotionalResponse(
                emoji = "☕️",
                headline = "Treat yourself... again?",
                message = "Small treats feel harmless in the moment, but they add up faster than you'd think. There's nothing wrong with enjoying a coffee — just worth being intentional about it.",
                tip = "Try tracking your weekly café spend for one month. Seeing the total is often more motivating than any rule."
            )
        }

        // High impulse label
        if (impulseLabel == "HIGH" && regretScore >= 8) {
            return EmotionalResponse(
                emoji = "⚡️",
                headline = "Your impulse score flagged this one.",
                message = "This purchase had several signals of impulse buying — the store type, timing, and basket size all pointed the same way. You felt it too.",
                tip = "When you notice the impulse feeling building, try naming it out loud: 'I'm about to impulse buy.' Just naming it gives you a pause."
            )
        }

        // Score 8–9 generic high regret
        return if (regretScore == 9) {
            EmotionalResponse(
                emoji = "😬",
                headline = "That one stings a little.",
                message = "A 9/10 is a strong signal. It's okay — everyone has purchases they wish they could undo. What matters is noticing the pattern.",
                tip = "Look back at your high-regret purchases. Is there a time of day, store type, or emotion that keeps coming up?"
            )
        } else {
            EmotionalResponse(
                emoji = "😔",
                headline = "Not your best purchase.",
                message = "An 8/10 regret score means this one didn't sit right with you. That feeling is worth listening to — it's your financial instincts talking.",
                tip = "Next time you're about to buy something similar, pause and ask: 'Will I feel good about this tomorrow?'"
            )
        }
    }
}
