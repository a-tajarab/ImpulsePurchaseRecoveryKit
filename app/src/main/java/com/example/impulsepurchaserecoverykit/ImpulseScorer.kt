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

        // --- A) Total amount (0..35) ---
        val totalPts = when {
            total >= 70 -> 35
            total >= 30 -> 25
            total >= 10 -> 15
            total >  0  -> 5
            else -> 0
        }.also {
            if (it >= 25) reasons.add("High spend (£%.2f)".format(total))
        }

        // --- B) Category risk (0..25) ---
        // Use your ParsedItem.category values from the parser
        val highRiskCats = setOf("tops", "bottoms", "outerwear", "shoes", "accessories", "bags")
        val treatCats = setOf("beverage", "bakery") // café-style treats in your parser

        val highRiskCount = items.count { it.category.lowercase() in highRiskCats }
        val treatCount = items.count { it.category.lowercase() in treatCats }

        val catPts = when {
            itemCount == 0 -> 0
            highRiskCount >= 2 -> 25
            highRiskCount == 1 -> 18
            treatCount >= 2 -> 14
            treatCount == 1 -> 8
            else -> 4
        }.also {
            if (highRiskCount >= 1) reasons.add("Clothing/accessories items detected")
            else if (treatCount >= 2) reasons.add("Mostly treat items (coffee/snacks)")
        }

        // --- C) Time-of-day risk (0..20) ---
        // We try to detect HH:mm from rawText (your OCR text includes time often)
        val hour = extractHour(receipt.rawText)
        val timePts = when {
            hour == null -> 0
            hour in 20..23 -> 20
            hour in 11..14 -> 10
            hour in 0..6 -> 12
            else -> 3
        }.also {
            if (hour != null && it >= 10) reasons.add("Purchase time suggests impulse (${hour}:xx)")
        }

        // --- D) Treat ratio (0..20) ---
        val treatRatio = if (itemCount == 0) 0.0 else (treatCount.toDouble() / itemCount.toDouble())
        val treatPts = when {
            treatRatio >= 0.75 -> 20
            treatRatio >= 0.50 -> 14
            treatRatio >= 0.25 -> 8
            else -> 2
        }.also {
            if (treatRatio >= 0.5) reasons.add("High treat ratio (${(treatRatio*100).roundToInt()}%)")
        }

        val necessityCats = setOf("produce", "dairy", "meat", "bakery")
        val necessityCount = items.count { it.category in necessityCats}

        // --- Combine ---
        var score = totalPts + catPts + timePts + treatPts
        score = score.coerceIn(0, 100)

        if (itemCount > 0 && necessityCount >= itemCount * 0.6){
            score -= 10
            reasons.add("Mostly essential grocery items")
        }

        val label = when {
            score >= 70 -> ImpulseLabel.HIGH
            score >= 40 -> ImpulseLabel.MEDIUM
            else -> ImpulseLabel.LOW
        }

        if (receipt.storeName?.lowercase() in listOf("asos", "amazon", "shein", "temu")) {
            score += 8
            reasons.add("Online purchase")
        }

        val duplicates = items.groupBy { it.name }.any { it.value.size >= 2 }
        if (duplicates) {
            score += 6
            reasons.add("Duplicate items purchased")
        }

        if (reasons.isEmpty()) reasons.add("No strong impulse signals detected")

        return ImpulseScoreResult(score, label, reasons)
    }

    // Extract first HH:mm we find
    private fun extractHour(text: String): Int? {
        val m = Regex("""\b([01]?\d|2[0-3])\s*:\s*([0-5]\d)\b""").find(text)
        return m?.groupValues?.get(1)?.toIntOrNull()
    }
}