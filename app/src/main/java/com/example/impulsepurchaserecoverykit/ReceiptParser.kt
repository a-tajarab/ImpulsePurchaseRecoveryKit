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
     * Extract store name (usually in first few lines)
     */
    private fun extractStoreName(text: String): String? {
        val lines = text.lines().filter { it.isNotBlank() }

        // Common store names to look for
        val storePatterns = listOf(
            "walmart", "target", "costco", "kroger", "safeway",
            "whole foods", "trader joe", "albertsons", "publix",
            "cvs", "walgreens", "rite aid", "dollar", "aldi",
            "tesco", "sainsbury", "asda", "morrisons"
        )

        // Check first 5 lines for store names
        for (line in lines.take(5)) {
            val lowerLine = line.lowercase()
            for (pattern in storePatterns) {
                if (pattern in lowerLine) {
                    Log.d("PARSER", "Found store name: $line")
                    return line.trim()
                }
            }
        }

        // If no known store found, return first non-empty line
        val firstLine = lines.firstOrNull()?.trim()
        Log.d("PARSER", "Using first line as store name: $firstLine")
        return firstLine
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
     * Extract individual items and their prices
     */
    private fun extractItems(text: String, storeName: String?): List<ParsedItem> {
        val items = mutableListOf<ParsedItem>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // inline: "Coffee £2.50" or "Bread 0.89"
        val itemLinePattern = Pattern.compile(
            "(.{2,}?)\\s+(?:£|\\$|GBP\\s*)?([0-9]+(?:[\\.,][0-9]{1,2})?)\\s*$",
            Pattern.CASE_INSENSITIVE
        )

        val stopKeywords = listOf(
            "subtotal", "sub total", "sub-total",
            "tax", "vat", "total",
            "clubcard", "savings", "saving",
            "visa", "mastercard", "amex", "debit", "credit",
            "payment", "paid", "change", "balance",
            "join", "today", "thank", "visit", "www",
            "cashier", "operator", "terminal", "auth", "ref"
        )

        val categories = mapOf(
            "produce" to listOf(
                "banana",
                "apple",
                "orange",
                "lettuce",
                "tomato",
                "potato",
                "onion"
            ),
            "dairy" to listOf("milk", "cheese", "yogurt", "butter", "cream", "egg"),
            "meat" to listOf("chicken", "beef", "pork", "fish", "turkey", "bacon"),
            "bakery" to listOf("bread", "bagel", "donut", "cake", "cookie"),
            "beverage" to listOf("soda", "juice", "water", "coffee", "tea", "latte",
                "cappuccino", "espresso", "mocha", "americano", "macchiato"),
            "tops" to listOf(
                "shirt",
                "tshirt",
                "t-shirt",
                "tee",
                "blouse",
                "tank",
                "hoodie",
                "sweater",
                "jumper",
                "crop top",
                "top",
                "polo",
                "cardigan"
            ),
            "bottoms" to listOf(
                "jeans",
                "pants",
                "trousers",
                "shorts",
                "leggings",
                "skirt",
                "cargo",
                "joggers"
            ),
            "outerwear" to listOf("jacket", "coat", "parka", "blazer", "windbreaker", "puffer"),
            "shoes" to listOf(
                "shoes",
                "sneakers",
                "trainers",
                "boots",
                "heels",
                "sandals",
                "flip flops",
                "slides"
            ),
            "accessories" to listOf(
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
            ),
            "bags" to listOf("bag", "handbag", "purse", "backpack", "tote", "wallet", "duffel")
        )

        var startedItems = false

        // 1) Inline parsing pass
        for (line in lines) {
            val lower = line.lowercase()
            // ALWAYS skip summary/payment/footer lines (even before startedItems)
            if (stopKeywords.any { it in lower }) continue

            // Skip masked card lines like "****7912" or lines that are basically card digits
            if (lower.contains("****")) continue
            if (lower.matches(Regex("""^\*{2,}\d{2,}.*$"""))) continue

            // Skip pure time lines
            if (lower.matches(Regex("""^\d{1,2}:\d{2}$"""))) continue

            if (startedItems && stopKeywords.any { it in lower }) break

            if (!startedItems) {
                val headerJunk = listOf("tesco", "dunkin", "every", "clubcard")
                if (headerJunk.any { it in lower }) continue
            }


            val matcher = itemLinePattern.matcher(line)
            if (!matcher.find()) continue

            var name = matcher.group(1)?.trim().orEmpty()
            val priceStr = matcher.group(2)?.trim().orEmpty()

            val price = priceStr.replace(",", ".").toDoubleOrNull() ?: continue
            if (price !in 0.01..999.99) continue

            name = cleanItemName(name)
            if (!name.any { it.isLetter() }) continue
            if (name.length <= 5 && name.uppercase() == name) continue // TESCO, IKEA, etc

            startedItems = true

            val category = categorizeItem(name, categories)
            items.add(ParsedItem(name, price, 1, category))
            Log.d("PARSER", "Found item (inline): $name [$category] -> £$price")
        }

        // 2) Fallback: Tesco/Dunkin block pairing (names block + prices block)
        // 2) Fallback: names block + prices-only block (your generated receipts)
        if (items.isEmpty()) {
            Log.d("PARSER", "No inline items found. Trying block pairing (names + prices)...")

            val lowerStore = storeName?.lowercase()?.trim()

            val isPriceOnly = Regex("""^\s*(?:£|\$)\s*([0-9]+(?:[\\.,][0-9]{2})?)\s*$""")
            //val stopAt = setOf("subtotal", "sub total", "tax", "vat", "total")

            val cleanLines = lines.map { it.trim() }.filter { it.isNotEmpty() }

            fun isSummaryLabel(line: String):Boolean {
                val norm = line.lowercase()
                    .replace(" ", "")
                    .replace("-", "")
                    .trim()
                return norm == "subtotal" || norm == "tax" || norm == "vat" || norm == "total"
            }
            // Find where items section ends (Subtotal/Tax/Total label lines)
            val stopIndex = cleanLines.indexOfFirst { isSummaryLabel(it)}
                .let { if (it == -1) cleanLines.size else it }

            // Find date line index (so we start collecting item names AFTER the date)
            val dateIndex = cleanLines.indexOfFirst { ln ->
                Regex("""\d{1,2}\s*[/-]\s*\d{1,2}\s*[/-]\s*\d{2,4}""").containsMatchIn(ln)
            }.let { if (it == -1) 0 else it }

            var startIndex = (dateIndex + 1).coerceAtLeast(0)
            // if next line is time like 20:40, skip it too
            if (startIndex < stopIndex && cleanLines[startIndex].matches(Regex("""\d{1,2}:\d{2}"""))) {
                startIndex++
            }


            // Collect item names between dateIndex..stopIndex (excluding store/header/address/phone)
            val itemNames = mutableListOf<String>()
            for (i in startIndex until stopIndex) {
                val ln = cleanLines[i]
                val l = ln.lowercase()

                // skip obvious header/footer junk
                if (l.contains("high street") || l.contains("london") || l.startsWith("020") || l.contains(
                        "tel"
                    )
                ) continue
                if (l.matches(Regex("""\d{1,2}:\d{2}"""))) continue // time like 11:55

                val cleaned = cleanItemName(ln)

                // skip store line if it appears
                if (lowerStore != null && cleaned.lowercase() == lowerStore) continue
                if (!cleaned.any { it.isLetter() }) continue

                itemNames.add(cleaned)

            }

            // Collect ALL prices-only lines (in order)
            val prices = mutableListOf<Double>()
            for (ln in cleanLines) {
                val m = isPriceOnly.find(ln)
                if (m != null) {
                    val p = m.groupValues[1].replace(",", ".").toDoubleOrNull()
                    if (p != null && p in 0.01..999.99) prices.add(p)
                }
            }

            // Your generator outputs: [item prices...] then [subtotal, tax, total] at the very end.
            // Remove last 3 if possible so totals don't get paired as items.
            val itemPrices = prices.toMutableList()
            if (itemPrices.size >= 3) {
                itemPrices.removeAt(itemPrices.lastIndex) // total
                itemPrices.removeAt(itemPrices.lastIndex) // tax
                itemPrices.removeAt(itemPrices.lastIndex) // subtotal
            }

            val pairCount = minOf(itemNames.size, itemPrices.size)
            for (i in 0 until pairCount) {
                val name = itemNames[i]
                val price = itemPrices[i]
                val category = categorizeItem(name, categories)
                items.add(ParsedItem(name, price, 1, category))
                Log.d("PARSER", "Paired item: $name [$category] -> £$price")
            }

        }
        Log.d("PARSER", "Found ${items.size} items total")
        return items
    }






    private fun cleanItemName(name: String): String {
        return name
            .replace(Regex("[*@#]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }
    }

    private fun categorizeItem(itemName: String, categories: Map<String, List<String>>): String {
        val lowerName = itemName.lowercase()

        for ((category, keywords) in categories) {
            for (kw in keywords){
                val pattern = Regex("""\b${Regex.escape(kw.lowercase())}\b""")
                if (pattern.containsMatchIn(lowerName)) return category
            }
        }
        return "other"
    }

    /**
     * Extract total amount
     */

    private fun extractTotal(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1) If there is a "TOTAL ... £xx.xx" on the same line, use it.
        val totalSameLine = Pattern.compile(
            """\btotal\b.*?(?:£|\$)\s*([0-9]+(?:[.,][0-9]{2})?)""",
            Pattern.CASE_INSENSITIVE
        )
        for (ln in lines) {
            val m = totalSameLine.matcher(ln)
            if (m.find()) {
                val v = m.group(1)?.replace(",", ".")?.toDoubleOrNull()
                if (v != null) {
                    Log.d("PARSER", "Found total on TOTAL line: £$v")
                    return v
                }
            }
        }

        // 2) Otherwise collect ALL currency amounts in order and take the last one.
        // This matches your generated receipts where subtotal/tax/total are at the very end.
        val money = Pattern.compile("""(?:£|\$)\s*([0-9]+(?:[.,][0-9]{2})?)""")
        val amounts = mutableListOf<Double>()

        for (ln in lines) {
            val m = money.matcher(ln)
            while (m.find()) {
                val v = m.group(1)?.replace(",", ".")?.toDoubleOrNull()
                if (v != null) amounts.add(v)
            }
        }

        if (amounts.isNotEmpty()) {
            val total = amounts.last()
            Log.d("PARSER", "Using last currency amount as total: £$total")
            return total
        }

        Log.d("PARSER", "No total found")
        return null
    }


    /**
     * Extract subtotal (before tax)
     */
    private val moneyRegex = Regex("""(?:£|\$)\s*([0-9]+(?:[.,][0-9]{2})?)""")

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