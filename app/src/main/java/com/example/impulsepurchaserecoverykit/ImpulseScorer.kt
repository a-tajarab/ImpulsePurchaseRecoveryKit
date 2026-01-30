package com.example.impulsepurchaserecoverykit

import org.json.JSONArray
import kotlin.math.roundToInt

enum class ImpulseLabel { LOW, MEDIUM, HIGH }

data class ImpulseScoreResult(
    val score: Int,                 // 0..100
    val label: ImpulseLabel,
    val reasons: List<String>
) {
    fun reasonsJson(): String = JSONArray(reasons).toString()
}

object ImpulseScorer {

    fun score(receipt: ParsedReceipt): ImpulseScoreResult {
        val reasons = mutableListOf<String>()

        val total = receipt.total ?: 0.0
        val items = receipt.items
        val itemCount = items.size

        // --- A) Total amount (0..30) ---
        val totalPts = when {
            total >= 100 -> 30
            total >= 70  -> 26
            total >= 30  -> 18
            total >= 10  -> 10
            total > 0    -> 4
            else -> 0
        }.also {
            if (it >= 18) reasons.add("High spend (£%.2f)".format(total))
        }

        // --- B) Category risk (0..25) ---
        val highRiskCats = setOf("tops", "bottoms", "outerwear", "shoes", "accessories", "bags")
        val treatCats = setOf("beverage", "bakery")

        val highRiskCount = items.count { it.category.lowercase() in highRiskCats }
        val treatCount = items.count { it.category.lowercase() in treatCats }

        val catPts = when {
            itemCount == 0 -> 0
            highRiskCount >= 2 -> 25
            highRiskCount == 1 -> 18
            treatCount >= 2 -> 14
            treatCount == 1 -> 8
            else -> 3
        }.also {
            if (highRiskCount >= 1) reasons.add("Clothing/accessories detected")
            else if (treatCount >= 2) reasons.add("Mostly treats (coffee/snacks)")
        }

        // --- C) Time-of-day risk (0..15) ---
        val hour = extractHour(receipt.rawText)
        val timePts = when {
            hour == null -> 0
            hour in 20..23 -> 15
            hour in 0..6 -> 12
            hour in 11..14 -> 7
            else -> 2
        }.also {
            if (hour != null && it >= 10) reasons.add("Late/night purchase (${hour}:xx)")
        }

        // --- D) Treat ratio (0..10) ---
        val treatRatio = if (itemCount == 0) 0.0 else treatCount.toDouble() / itemCount.toDouble()
        val treatPts = when {
            treatRatio >= 0.75 -> 10
            treatRatio >= 0.50 -> 7
            treatRatio >= 0.25 -> 4
            else -> 1
        }.also {
            if (treatRatio >= 0.5) reasons.add("High treat ratio (${(treatRatio * 100).roundToInt()}%)")
        }

        // --- E) Basket size (0..10) ---
        // Smaller baskets are more “impulse”, big baskets more “planned”
        val basketPts = when {
            itemCount == 0 -> 0
            itemCount <= 2 -> 10
            itemCount <= 4 -> 7
            itemCount <= 8 -> 4
            else -> 1
        }.also {
            if (itemCount <= 2) reasons.add("Small basket ($itemCount items)")
        }

        // --- F) Duplicate items (0..5) ---
        // A couple duplicates can hint “unplanned repeat / double-buy”
        val duplicateCount = items
            .groupBy { it.name.trim().lowercase() }
            .count { it.value.size >= 2 }

        val dupPts = when {
            duplicateCount >= 2 -> 5
            duplicateCount == 1 -> 3
            else -> 0
        }.also {
            if (duplicateCount >= 1) reasons.add("Repeated items detected")
        }

        // --- G) Store-type risk (0..5) ---
        val storePts = storeRiskPoints(receipt.storeName).also {
            if (it >= 4) reasons.add("High-impulse store type")
        }

        var score = totalPts + catPts + timePts + treatPts + basketPts + dupPts + storePts
        score = score.coerceIn(0, 100)

        val label = when {
            score >= 70 -> ImpulseLabel.HIGH
            score >= 40 -> ImpulseLabel.MEDIUM
            else -> ImpulseLabel.LOW
        }

        if (reasons.isEmpty()) reasons.add("No strong impulse signals detected")

        return ImpulseScoreResult(score, label, reasons)
    }

    private fun storeRiskPoints(storeName: String?): Int {
        val s = storeName?.lowercase()?.trim().orEmpty()

        val high = listOf("asos", "amazon", "shein", "zara", "hm", "h&m", "primark", "nike", "adidas")
        val medium = listOf("starbucks", "dunkin", "pret", "costa", "mcdonald", "greggs")

        return when {
            high.any { it in s } -> 5
            medium.any { it in s } -> 3
            else -> 0
        }
    }

    private fun extractHour(text: String): Int? {
        val m = Regex("""\b([01]?\d|2[0-3])\s*:\s*([0-5]\d)\b""").find(text)
        return m?.groupValues?.get(1)?.toIntOrNull()
    }
}