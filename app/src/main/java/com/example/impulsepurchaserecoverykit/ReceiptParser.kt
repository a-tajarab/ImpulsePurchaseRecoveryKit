package com.example.impulsepurchaserecoverykit

import android.util.Log
import java.util.regex.Pattern
import kotlin.math.min

class ReceiptParser {
    /**
     * Main parsing function - takes raw OCR text and returns structured data
     */
    fun parseReceipt(rawText: String): ParsedReceipt {
        Log.d("PARSER", "Starting to parse receipt...")
        Log.d("PARSER_RAW", "Raw text length: ${rawText.length} characters")

        val storeName = extractStoreName(rawText)
        val date = extractDate(rawText)
        val items = extractItems(rawText, storeName)
        val total = extractTotal(rawText)
        val subtotal = extractSubtotal(rawText)
        val tax = extractTax(rawText)

        val receipt = ParsedReceipt(
            storeName = storeName,
            purchaseDate = date,
            items = items,
            subtotal = subtotal,
            tax = tax,
            total = total,
            rawText = rawText
        )

        Log.d("PARSER_RESULT", "Parsed receipt:\n${receipt.getSummary()}")
        return receipt
    }

    /**
     * Extract store name
     */
    private fun extractStoreName(text: String): String? {
        val lines = text.lines().map { it.trim()}.filter { it.isNotBlank() }

        // Common store names to look for
        val storePatterns = listOf(
            "walmart", "target", "costco", "kroger", "safeway",
            "whole foods", "trader joe", "albertsons", "publix",
            "cvs", "walgreens", "rite aid", "dollar", "aldi",
            "tesco", "sainsbury", "asda", "morrisons", "m&s",
            "marks", "marks and spencer", "boots", "primark",
            "jd sports", "shein"
        )

        // Check first 5 lines for store names
        for (line in lines.take(20)) {
            val lower = line.lowercase()
            if (storePatterns.any { it in lower}){
                Log.d("PARSER", "Found store name: $line")
                return line.trim()
            }
        }

        // If no known store found, return first non-empty line
        val firstLine = lines.firstOrNull{!it.contains("£") && !it.contains("$")}?.trim()
        Log.d("PARSER", "Using first line as store name: $firstLine")
        return firstLine
    }

    /**
     * Extract individual items and their prices
     */
    private fun extractItems(text: String, storeName: String?): List<ParsedItem> {
        val items = mutableListOf<ParsedItem>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val lowerStore = storeName?.lowercase()?.trim()
        // A) Real receipts: require £/$
        val itemWithCurrency = Pattern.compile(
            """(.{2,}?)\s+(?:£|\$)\s*([0-9]+(?:[.,][0-9]{1,2})?)\s*$""",
            Pattern.CASE_INSENSITIVE
        )

        // 1) Inline pass (only if OCR actually put item + price on same line)
        for (line in lines) {
            val lower = line.lowercase()
            if (isNoiseLine(line)) continue
            if (lowerStore != null && lower == lowerStore) continue

            val m = itemWithCurrency.matcher(line)
            if (!m.find()) continue

            val rawName = m.group(1)?.trim().orEmpty()
            val rawPrice = m.group(2)?.trim().orEmpty()

            val price = parseMoneyNumber(rawPrice) ?: continue
            if (price !in 0.01..9999.99) continue

            val name = cleanItemName(rawName)
            if (!name.any { it.isLetter() }) continue
            if (looksLikePhoneOrRef(name)) continue

            items.add(ParsedItem(name, price, 1, categorizeItem(name)))
            Log.d("PARSER", "Found item (inline): $name -> £$price")
        }

        if (items.isNotEmpty()) {
            Log.d("PARSER", "Found ${items.size} items total (inline)")
            return items
        }

        // 2) Block pairing fallback: names list + money list (common for real receipts)
        Log.d("PARSER", "No inline items found. Trying block pairing (names + money)...")

        val total = extractTotal(text)
        val subtotal = extractSubtotal(text)
        val tax = extractTax(text)

        fun approxEq(a: Double?, b: Double?, eps: Double = 0.01): Boolean {
            if (a == null || b == null) return false
            return kotlin.math.abs(a - b) <= eps
        }

        val allMoney = mutableListOf<Double>()
        for (line in lines){
            val money = extractMoneyFromLine(line)
            if (money != null && money in 0.0..9999.99){
                allMoney.add(money)
            }
        }


        val moneyCandidates = allMoney.filter{ v ->
            if (v==0.0) return@filter false

            if (approxEq(v, total)|| approxEq(v, subtotal)|| approxEq(v, tax)) return@filter false
            true
        }.toMutableList()

        val nameCandidates = mutableListOf<String>()
        for (line in lines) {
            if (isNoiseLine(line)) continue

            val lower = line.lowercase()

            // skip store name
            if (lowerStore != null && lower == lowerStore) continue

            // skip summary labels
            if (lower.contains("subtotal") || lower.contains("sub total")) continue
            if (lower.contains("tax") || lower.contains("vat")) continue
            if (lower == "total" || lower.startsWith("total ")) continue

            // skip stuff that isn't an item
            if (looksLikeDateOrTime(line)) continue
            if (looksLikeAddress(line)) continue
            if (looksLikePhoneOrRef(line)) continue

            // skip pure money lines
            if (extractMoneyFromLine(line) != null) continue

            val cleaned = cleanItemName(line)
            if (!cleaned.any { it.isLetter() }) continue

            // skip “k k k …” junk
            if (cleaned.lowercase().matches(Regex("""^[k\s]+$"""))) continue

            nameCandidates.add(cleaned)
            }


        val pairCount = minOf(nameCandidates.size, moneyCandidates.size)
        for (i in 0 until pairCount) {
            val name = nameCandidates[i]
            val price = moneyCandidates[i]
            items.add(ParsedItem(name, price, 1, categorizeItem(name)))
            Log.d("PARSER", "Paired item: $name -> £$price")
        }
        Log.d("PARSER", "Found ${items.size} items total (paired)")
        return items
    }
    private fun cleanItemName(name: String): String =
        name.replace(Regex("[*@#]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }

    private fun parseMoneyNumber(s: String): Double? {
        // Fix OCR mistakes: l / I used instead of 1
        val fixed = s
            .replace("l", "1", ignoreCase = false)
            .replace("I", "1", ignoreCase = false)
            .replace(",", ".")
        return fixed.toDoubleOrNull()
    }

    private val moneyRegex = Regex("""(?:£|\$)\s*([0-9lI]+(?:[.,][0-9]{2})?)""")

    private fun extractMoneyFromLine(line: String): Double? {
        val m = moneyRegex.find(line) ?: return null
        return parseMoneyNumber(m.groupValues[1])
    }

    private fun looksLikePhoneOrRef(line: String): Boolean {
        val l = line.lowercase().trim()
        if (l.startsWith("tel") || l.startsWith("phone")) return true
        if (l.contains("tel:")) return true
        if (l.matches(Regex("""^0\d{2,}.*"""))) return true // UK numbers often start 0...
        // long numeric reference / barcode-like
        if (l.replace(" ", "").matches(Regex("""^\d{8,}$"""))) return true
        return false
    }

    private fun looksLikeAddress(line: String): Boolean {
        val l = line.lowercase()
        if (l.contains("road") || l.contains("street") || l.contains("lane")) return true
        if (l.contains("london")) return true
        // UK postcode-ish quick check
        if (l.matches(Regex(""".*\b[A-Z]{1,2}\d[A-Z0-9]?\s*\d[A-Z]{2}\b.*""", RegexOption.IGNORE_CASE))) return true
        return false
    }

    private fun looksLikeDateOrTime(line: String): Boolean {
        val l = line.trim()
        if (l.matches(Regex("""^\d{1,2}:\d{2}.*$"""))) return true
        if (Regex("""\d{1,2}\s*[/-]\s*\d{1,2}\s*[/-]\s*\d{2,4}""").containsMatchIn(l)) return true
        return false
    }

    private fun isNoiseLine(line: String): Boolean {
        val l = line.lowercase().trim()
        if (l.contains("www.") || l.contains("http")) return true
        if (l.contains("tell us how") || l.contains("enter for a chance")) return true
        if (l.contains("gift card") || l.startsWith("win ")) return true
        if (l.contains("nectar") || l.contains("points")) return true

        // payment / card
        if (l.contains("visa") || l.contains("mastercard") || l.contains("debit") || l.contains("credit")) return true
        if (l.contains("auth") || l.contains("terminal") || l.contains("operator")) return true
        if (l.contains("****") || l.matches(Regex("""^\*{2,}\d+.*$"""))) return true

        // raw barcode-like long digit lines
        if (l.replace(" ", "").matches(Regex("""^\d{10,}$"""))) return true

        return false
    }

    private fun categorizeItem(itemName: String): String {
        val lower = itemName.lowercase()
        return when {
            listOf(
                "milk",
                "cheese",
                "yogurt",
                "butter",
                "cream",
                "egg"
            ).any { it in lower } -> "dairy"

            listOf(
                "banana",
                "apple",
                "orange",
                "lettuce",
                "tomato",
                "potato",
                "onion"
            ).any { it in lower } -> "produce"

            listOf(
                "water", "juice", "coffee", "tea", "soda", "latte",
                "cappuccino", "espresso", "mocha", "americano", "macchiato"
            ).any { it in lower } -> "beverage"

            listOf(
                "chicken",
                "beef",
                "pork",
                "fish",
                "turkey",
                "bacon"
            ).any { it in lower } -> "meat"

            listOf("bread", "bagel", "donut", "cake", "cookie").any { it in lower } -> "bakery"

            //clothings
            listOf(
                "shirt", "tshirt", "t-shirt", "tee", "blouse", "tank", "hoodie", "sweater",
                "jumper", "crop top", "top", "polo", "cardigan"
            ).any { it in lower } -> "tops"

            listOf(
                "jeans", "pants", "trousers", "shorts", "leggings", "skirt", "cargo",
                "joggers"
            ).any { it in lower } -> "bottoms"

            listOf(
                "jacket",
                "coat",
                "parka",
                "blazer",
                "windbreaker",
                "puffer"
            ).any { it in lower } -> "outerwear"

            listOf(
                "shoes", "sneakers", "trainers", "boots", "heels", "sandals",
                "flip flops", "slides"
            ).any { it in lower } -> "shoes"

            listOf(
                "hat",
                "cap",
                "scarf",
                "gloves",
                "belt",
                "sunglasses",
                "watch",
                "jewelry",
                "earrings",
                "necklace",
                "bracelet",
                "ring"
            ).any { it in lower } -> "accessories"

            listOf(
                "bag",
                "handbag",
                "purse",
                "backpack",
                "tote",
                "wallet",
                "duffel"
            ).any { it in lower } -> "bags"


            listOf(
                "subtotal", "sub total", "sub-total",
                "tax", "vat", "total",
                "clubcard", "savings", "saving",
                "visa", "mastercard", "amex", "debit", "credit",
                "payment", "paid", "change", "balance",
                "join", "today", "thank", "visit", "www",
                "cashier", "operator", "terminal", "auth", "ref"
            ).any { it in lower } -> "stopKeywords"

            else -> "other"
        }
    }


        /**
     * Extract purchase date
     */
    private fun extractDate(text: String): String? {
        // Common date patterns
        val patterns = listOf(
            // DD/MM/YYYY or MM/DD/YYYY with optional spaces around separators
            "\\d{1,2}\\s*[/-]\\s*\\d{1,2}\\s*[/-]\\s*\\d{2,4}",
            // YYYY-MM-DD with optional spaces
            "\\d{4}\\s*[/-]\\s*\\d{1,2}\\s*[/-]\\s*\\d{1,2}",
            // Month DD, YYYY
            "[A-Za-z]{3,9}\\s+\\d{1,2},?\\s+\\d{4}"
        )

        for (pattern in patterns) {
            val regex = Pattern.compile(pattern)
            val matcher = regex.matcher(text)
            if (matcher.find()) {
                val date = matcher.group()
                Log.d("PARSER", "Found date: $date")
                return date
            }
        }

        Log.d("PARSER", "No date found")
        return null
    }

    /**
     * Extract total amount
     */

    private fun extractTotal(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val money = Pattern.compile("""(?:£|\$)\s*([0-9]+(?:[.,][0-9]{2})?)""")
        fun parseMoney(s: String): Double? = s.replace(",", ".").toDoubleOrNull()

        // 1) Best: look for TOTAL / Total Amount lines (ignore WIN lines)
        val totalKeywords = listOf("total amount", "amount due", "balance due", "total")
        for (ln in lines) {
            val lower = ln.lowercase()
            if (totalKeywords.any { lower.contains(it) } && !lower.contains("win")) {
                val m = money.matcher(ln)
                if (m.find()) {
                    val v = parseMoney(m.group(1) ?: "")
                    if (v != null) {
                        Log.d("PARSER", "Found total via keyword line: £$v from: $ln")
                        return v
                    }
                }
            }
        }

        // 2) Fallback: pick the LARGEST £ amount among non-noise lines
        val candidates = mutableListOf<Double>()
        for (ln in lines) {
            if (isNoiseLine(ln)) continue
            val m = money.matcher(ln)
            while (m.find()) {
                val v = parseMoney(m.group(1) ?: "")
                if (v != null && v in 0.01..9999.99) candidates.add(v)
            }
        }

        val best = candidates.maxOrNull()
        if (best != null) Log.d("PARSER", "Fallback total picked as max non-noise money: £$best")
        else Log.d("PARSER", "No total found")

        return best
    }


    /**
     * Extract subtotal (before tax)
     */
    private fun extractSubtotal(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1) Same-line format: "Subtotal £12.34"
        for (ln in lines) {
            val lower = ln.lowercase()
            if (lower.contains("subtotal") || lower.contains("sub total") || lower.contains("sub-total")) {
                val m = moneyRegex.find(ln)
                if (m != null) return m.groupValues[1].replace(",", ".").toDoubleOrNull()
            }
        }

        // 2) Generated format: last 3 money amounts are [subtotal, tax, total]
        val amounts = lines.mapNotNull { ln ->
            moneyRegex.find(ln)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
        }
        if (amounts.size >= 3) return amounts[amounts.size - 3]

        // 3) If we only have total & tax elsewhere, subtotal can be total - tax
        val total = extractTotal(text)
        val tax = extractTax(text)
        return if (total != null && tax != null) (total - tax).coerceAtLeast(0.0) else null
    }

    private fun extractTax(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1) Same-line format: "Tax £1.23" / "VAT £1.23"
        for (ln in lines) {
            val lower = ln.lowercase()
            if (lower.contains("tax") || lower.contains("vat")) {
                val m = moneyRegex.find(ln)
                if (m != null) return m.groupValues[1].replace(",", ".").toDoubleOrNull()
            }
        }

        // 2) Generated format: last 3 money amounts are [subtotal, tax, total]
        val amounts = lines.mapNotNull { ln ->
            moneyRegex.find(ln)?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
        }
        return if (amounts.size >= 2) amounts[amounts.size - 2] else null
    }
}