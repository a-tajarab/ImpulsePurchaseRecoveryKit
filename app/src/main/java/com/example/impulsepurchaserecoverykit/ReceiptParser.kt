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
        val items = extractItems(rawText)
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
            // MM/DD/YYYY or MM/DD/YY
            "\\d{1,2}/\\d{1,2}/\\d{2,4}",
            // DD-MM-YYYY or DD-MM-YY
            "\\d{1,2}-\\d{1,2}-\\d{2,4}",
            // YYYY-MM-DD
            "\\d{4}-\\d{1,2}-\\d{1,2}",
            // Month DD, YYYY (e.g., "Jan 15, 2024")
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
    private fun extractItems(text: String): List<ParsedItem> {
        val items = mutableListOf<ParsedItem>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Pattern to match lines with prices
        // Matches: "Item Name    $12.99" or "Item Name 12.99"
        val standardPattern = Pattern.compile("(.*?)\\s+\\$?([0-9]+\\.[0-9]{2})\\s*$")

        // Keywords to skip (not actual items)
        val skipKeywords = listOf(
            "subtotal", "sub total", "sub-total",
            "tax", "total", "amount", "balance", "change",
            "cash", "credit", "debit", "visa", "mastercard",
            "discount", "coupon", "savings", "tender"
        )

        val categories = mapOf(
            "produce" to listOf(
                "banana", "apple", "orange", "lettuce", "tomato", "potato", "onion"
            ),
            "dairy" to listOf("milk", "cheese", "yogurt", "butter", "cream", "egg"),
            "meat" to listOf("chicken", "beef", "pork", "fish", "turkey", "bacon"),
            "bakery" to listOf("bread", "bagel", "donut", "cake", "cookie"),
            "beverage" to listOf("soda", "juice", "water", "coffee", "tea"),
            "tops" to listOf(
                "shirt", "tshirt", "t-shirt", "tee", "blouse", "tank", "hoodie", "sweater",
                "jumper", "crop top", "top", "polo", "cardigan"
            ),
            "bottoms" to listOf(
                "jeans", "pants", "trousers", "shorts",
                "leggings", "skirt", "cargo", "joggers"
            ),
            "outerwear" to listOf("jacket", "coat", "parka", "blazer", "windbreaker", "puffer"),
            "shoes" to listOf(
                "shoes", "sneakers", "trainers", "boots", "heels", "sandals", "flip flops", "slides"
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

        for (line in lines) {

            // Skip if line contains skip keywords
            val lowerLine = line.lowercase()
            if (skipKeywords.any { it in lowerLine }) continue

            // Try to match price pattern
            val matcher = standardPattern.matcher(line)
            if (matcher.find()) {
                val itemName = matcher.group(1)?.trim() ?: continue
                val priceStr = matcher.group(2) ?: continue

                // Skip if item name is too short or just numbers
                if (itemName.length >= 2 && !itemName.matches(Regex("^[0-9\\s]+$"))) {
                    try {
                        val price = priceStr.toDouble()
                        // Sanity check - items usually cost between $0.01 and $999.99
                        if (price in 0.01..999.99) {
                            val category = categorizeItem(itemName, categories)
                            items.add(ParsedItem(itemName, price, 1, category))
                            Log.d("PARSER", "Found item: $itemName [$category] -> $$price")
                        }
                    } catch (e: NumberFormatException) {
                        //Log.d("PARSER", "Failed to parse price: $priceStr")
                    }
                }
            }
        }
        if (items.isEmpty()) {
            Log.d("PARSER", "Standard pattern failed, trying alternative pattern ...")

            val itemNames = mutableListOf<String>()
            val prices = mutableListOf<Double>()

            for (line in lines) {

                val lowerLine = line.lowercase()

                if (skipKeywords.any { it in lowerLine }) continue

                val pricePattern = Pattern.compile("^\\$?([0-9]+\\.[0-9]{2})$")
                val priceMatcher = pricePattern.matcher(line.trim())

                if (priceMatcher.find()) {
                    val priceStr = priceMatcher.group(1)
                    try {
                        val price = priceStr.toDouble()
                        if (price in 0.01..999.99) {
                            prices.add(price)
                            Log.d("PARSER", "Found price: $$price")
                        }
                    } catch (e: NumberFormatException) {
                        //
                    }
                } else if (line.length >= 2 && !line.matches(Regex("^[0-9/\\-\\s]+$"))) {
                    itemNames.add(line)
                    Log.d("PARSER", "Found potential item: $line")
                }
            }
            val pairCount = minOf(itemNames.size, prices.size)
            for (i in 0 until pairCount) {
                val itemName = itemNames[i]
                val price = prices[i]
                val category = categorizeItem(itemName, categories)

                items.add(ParsedItem(itemName, price, 1, category))
                Log.d("PARSER", "Matched item: $itemName [$category] -> $$price")
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
            if (keywords.any { it in lowerName }) {
                return category
            }
        }
        return "other"
    }

    /**
     * Extract total amount
     */

    private fun extractTotal(text: String): Double? {
        // Look for patterns like "TOTAL $45.67" or "Amount Due: 45.67"
        val lines = text.lines().map { it.trim() }

        for (i in lines.indices) {
            val line = lines[i].lowercase()

            if ("total" in line) {
                val pricePattern = Pattern.compile("\\$?([0-9]+\\.[0-9]{2})")
                val matcher = pricePattern.matcher(lines[i])

                if (matcher.find()) {
                    try {
                        val total = matcher.group(1).toDouble()
                        Log.d("PARSER", "Found total on same line: $$total")
                        return total
                    } catch (e: NumberFormatException) {
                        //Log.d("PARSER", "Failed to parse total: $lastMatch")
                    }
                }
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    val nextMatcher = pricePattern.matcher(nextLine)

                    if (nextMatcher.find()) {
                        try {
                            val total = nextMatcher.group(1).toDouble()
                            Log.d("PARSER", "Found total on next line: $$total ")
                            return total
                        } catch (e: NumberFormatException) {
                            //Log.d("PARSER", "Failed to parse total: $lastMatch")
                        }
                    }
                }
            }
        }
        val allPrices = mutableListOf<Double>()
        val pricePattern = Pattern.compile("\\$?([0-9]+\\.[0-9]{2})")

        for (line in lines) {
            val matcher = pricePattern.matcher(line)
            while (matcher.find()) {
                try {
                    allPrices.add(matcher.group(1).toDouble())
                } catch (e: NumberFormatException) {

                }
            }
        }
        if (allPrices.isNotEmpty()) {
            val maxPrice = allPrices.maxOrNull()
            Log.d("PARSER", "Using largest price as total: $$maxPrice")
            return maxPrice
        }
        Log.d("PARSER", "No total found")
        return null
    }


    /**
     * Extract subtotal (before tax)
     */
    private fun extractSubtotal(text: String): Double? {
        val pattern =
            Pattern.compile("sub.*?total.*?\\$?([0-9]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text.lowercase())

        if (matcher.find()) {
            val subtotal = matcher.group(1)?.toDoubleOrNull()
            Log.d("PARSER", "Found subtotal: $$subtotal")
            return subtotal
        }

        return null
    }

    /**
     * Extract tax amount
     */
    private fun extractTax(text: String): Double? {
        val pattern = Pattern.compile("tax.*?\\$?([0-9]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text.lowercase())

        if (matcher.find()) {
            val tax = matcher.group(1)?.toDoubleOrNull()
            Log.d("PARSER", "Found tax: $$tax")
            return tax
        }

        return null
    }
}