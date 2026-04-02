package com.example.impulsepurchaserecoverykit

import android.util.Log
import java.util.regex.Pattern

class ReceiptParser {

    fun parseReceipt(rawText: String): ParsedReceipt {
        Log.d("PARSER", "Starting to parse receipt...")
        val storeName = extractStoreName(rawText)
        val date = extractDate(rawText)
        val time = extractTime(rawText)
        val items = extractItems(rawText, storeName)
        val total = extractTotal(rawText)
        val subtotal = extractSubtotal(rawText)
        val tax = extractTax(rawText)
        val shipping = extractShipping(rawText, items, total, tax)

        val receipt = ParsedReceipt(
            storeName = storeName,
            purchaseDate = date,
            purchaseTime = time,
            items = items,
            subtotal = subtotal,
            tax = tax,
            shipping = shipping,
            total = total,
            rawText = rawText
        )
        Log.d("PARSER_RESULT", "Parsed receipt:\n${receipt.getSummary()}")
        return receipt
    }

    // ─────────────────────────────────────────────────────────────
    // STORE NAME
    // ─────────────────────────────────────────────────────────────

    private fun extractStoreName(text: String): String? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1) Search ALL lines for a known store name
        for (line in lines) {
            val lower = line.lowercase()
            val match = knownStores.firstOrNull { it in lower }
            if (match != null && !looksLikeAddress(line) && !isNoiseLine(line)) {
                Log.d("PARSER", "Found known store: $line")
                return cleanStoreName(line.trim())
            }
        }

        // 2) Fallback — first short line that doesn't look like noise/address/product
        val productWords = listOf(
            "jacket", "hoodie", "jeans", "shirt", "shoes", "trainers",
            "dress", "top", "bag", "pack", "set", "shorts", "coat"
        )
        val skipWords = listOf(
            "thank you", "order", "invoice", "receipt", "welcome",
            "hello", "dear", "shipping", "delivery", "total", "tax"
        )

        val fallback = lines.firstOrNull { line ->
            val lower = line.lowercase()
            line.length in 2..35 &&
                    !line.contains("£") && !line.contains("$") &&
                    !looksLikeAddress(line) &&
                    !looksLikePhoneOrRef(line) &&
                    !looksLikeDateOrTime(line) &&
                    !isNoiseLine(line) &&
                    skipWords.none { it in lower } &&
                    productWords.none { it in lower } &&
                    line.any { it.isLetter() } &&
                    !looksLikePersonName(line)
        }?.trim()

        Log.d("PARSER", "Fallback store name: $fallback")
        return fallback?.let { cleanStoreName(it) }
    }

    private fun cleanStoreName(name: String): String {
        var cleaned = name.lowercase().trim()
        cleaned = cleaned.removePrefix("www.")
        listOf(".co.uk", ".com", ".net", ".org", ".io", ".co", ".uk", ".store", ".shop")
            .forEach { suffix ->
                if (cleaned.endsWith(suffix)) {
                    cleaned = cleaned.removeSuffix(suffix)
                    return@forEach
                }
            }
        cleaned = cleaned.replace("/", "")
            .replace(Regex("""^[.\s]+|[.\s]+$"""), "").trim()
        return cleaned.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    // ─────────────────────────────────────────────────────────────
    // ITEMS — detects format automatically
    // ─────────────────────────────────────────────────────────────

    private fun extractItems(text: String, storeName: String?): List<ParsedItem> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val total = extractTotal(text)
        val subtotal = extractSubtotal(text)
        val tax = extractTax(text)
        // Detect format: does the text have prices on the same lines as item names?
        val inlineCount = lines.count { line ->
            val hasName = line.length > 3 && line.any { it.isLetter() }
            val hasPrice = moneyRegex.containsMatchIn(line) ||
                    Regex("""[A-Za-z].{2,30}\s{2,}[0-9]+\.[0-9]{2}$""").containsMatchIn(line)
            hasName && hasPrice && !isNoiseLine(line)
        }

        val totalMoneyLines = lines.count { moneyRegex.containsMatchIn(it) }
        val isInlineFormat = inlineCount >= (totalMoneyLines / 2)

        Log.d("PARSER", "Format detect — inline=$inlineCount, moneyLines=$totalMoneyLines, isInline=$isInlineFormat")

        return if (isInlineFormat) {
            extractItemsInline(lines, storeName)
        } else {
            extractItemsSplitColumn(lines, storeName, text, total, subtotal, tax)
        }
    }

    // Format 1 & 2: item and price on the same line
    private fun extractItemsInline(
        lines: List<String>,
        storeName: String?
    ): List<ParsedItem> {
        val items = mutableListOf<ParsedItem>()
        val lowerStore = storeName?.lowercase()?.trim()

        val patternWithPound = Regex("""^(.{2,40}?)\s{1,}[£$]\s*([0-9OlI]+[.,][0-9]{2})\s*$""")
        val patternNoSymbol = Regex("""^([A-Za-z][A-Za-z ,&\-']{1,38}?)\s{2,}([0-9]+\.[0-9]{2})\s*$""")

        val summaryStarts = listOf("total", "subtotal", "sub total", "tax", "vat",
            "paid", "visa", "mastercard", "amex", "change", "balance")

        for (line in lines) {
            val lower = line.lowercase()
            if (isNoiseLine(line)) continue
            if (lowerStore != null && lower == lowerStore) continue
            if (looksLikeAddress(line)) continue
            if (looksLikePhoneOrRef(line)) continue
            if (summaryStarts.any { lower.startsWith(it) }) continue

            val matchA = patternWithPound.find(line)
            if (matchA != null) {
                val name = cleanItemName(matchA.groupValues[1])
                val price = parseMoneyNumber(matchA.groupValues[2])
                if (isValidItem(name, price)) {
                    items.add(ParsedItem(name, price!!, 1, categorizeItem(name)))
                    Log.d("PARSER", "Inline item: $name -> £$price")
                }
                continue
            }

            val matchB = patternNoSymbol.find(line)
            if (matchB != null) {
                val name = cleanItemName(matchB.groupValues[1])
                val price = parseMoneyNumber(matchB.groupValues[2])
                if (isValidItem(name, price)) {
                    items.add(ParsedItem(name, price!!, 1, categorizeItem(name)))
                    Log.d("PARSER", "Inline item (no symbol): $name -> £$price")
                }
            }
        }

        Log.d("PARSER", "Inline found ${items.size} items")
        return items
    }

    // Format 3: names block then prices block (split column online receipts)
    private fun extractItemsSplitColumn(
        lines: List<String>,
        storeName: String?,
        fullText: String,
        precomputedTotal: Double?,
        precomputedSubtotal: Double?,
        precomputedTax: Double?
    ): List<ParsedItem> {
        val items = mutableListOf<ParsedItem>()
        val lowerStore = storeName?.lowercase()?.trim()

        val allPricesInOrder = mutableListOf<Double>()
        for (line in lines) {
            if (isNoiseLine(line)) continue
            val price = extractMoneyFromLine(line) ?: continue
            if (price in 0.01..9999.99) allPricesInOrder.add(price)
        }

        // ── Collect name candidates in order ──
        val hardSkip = listOf(
            "thank you", "order date", "order number", "order ref", "order no",
            "order confirmation", "order summary",
            "shipping to", "shipping", "delivery", "dispatch",
            "total", "subtotal", "sub total", "tax", "vat",
            "paid", "payment", "change", "balance",
            "visa", "mastercard", "amex", "debit", "credit",
            "est.", "since 18", "since 19", "since 20",
            "tell us", "visit us", "www.", "http",
            "enter for", "win a", "win £", "gift card",
            "cashier", "operator", "terminal", "auth",
            "nectar", "clubcard", "points", "savings",
            "return", "refund", "exchange",
            "sample", "example", "test"
        )

        val nameCandidates = mutableListOf<String>()
        for (line in lines) {
            val lower = line.lowercase()
            if (hardSkip.any { it in lower }) continue
            if (isNoiseLine(line)) continue
            if (looksLikeAddress(line)) continue
            if (looksLikePhoneOrRef(line)) continue
            if (looksLikeDateOrTime(line)) continue
            if (lowerStore != null && lower.contains(lowerStore)) continue
            if (extractMoneyFromLine(line) != null) continue
            if (line.replace(" ", "").all { it.isDigit() }) continue
            val cleaned = cleanItemName(line)
            if (cleaned.length < 2) continue
            if (!cleaned.any { it.isLetter() }) continue
            if (categorizeItem(cleaned) == "stopKeywords") continue
            val idx = lines.indexOf(line)
            val beforeLines = lines.take(idx).map { it.lowercase() }
            val afterShippingTo = beforeLines.any {
                it.startsWith("shipping to") || it == "shipping to:"
            }
            if (afterShippingTo) {
                val isProduct = productKeywords.any { it in lower }
                if (!isProduct) continue
            }
            nameCandidates.add(cleaned)
            Log.d("PARSER", "Name candidate: $cleaned")
        }

        // ── KEY INSIGHT: prices are ordered as [item1, item2, ..., subtotal, tax, total]
        // We know how many items there are = nameCandidates.size
        // So item prices = first N prices from allPricesInOrder
        // ──
        val itemCount = nameCandidates.size
        val itemPrices = allPricesInOrder.take(itemCount)

        Log.d("PARSER", "Split column — names: ${nameCandidates.size}, " +
                "all prices: ${allPricesInOrder.size}, taking first: $itemCount")

        val pairCount = minOf(nameCandidates.size, itemPrices.size)
        for (i in 0 until pairCount) {
            val name = nameCandidates[i]
            val price = itemPrices[i]
            items.add(ParsedItem(name, price, 1, categorizeItem(name)))
            Log.d("PARSER", "Split paired: $name -> £$price")
        }

        return items
    }

    private val productKeywords = listOf(
        "hoodie", "shirt", "t-shirt", "tee", "blouse", "jumper", "sweater",
        "jacket", "coat", "parka", "blazer", "cardigan",
        "jeans", "trousers", "pants", "shorts", "leggings", "skirt", "dress",
        "shoes", "trainers", "sneakers", "boots", "heels", "sandals",
        "hat", "cap", "scarf", "gloves", "belt", "sunglasses",
        "bag", "backpack", "wallet", "purse",
        "milk", "bread", "eggs", "butter", "cheese", "yogurt",
        "apple", "banana", "tomato", "potato", "chicken", "beef",
        "coffee", "tea", "juice", "water", "cereal", "pasta", "rice",
        "shampoo", "soap", "detergent", "batteries", "mop",
        "shelf", "lamp", "rug", "plant", "pot", "light",
        "sandwich", "salad", "croissant", "muffin", "donut",
        "pack", "set", "kit", "bundle"
    )

    private fun isValidItem(name: String, price: Double?): Boolean {
        if (price == null || price !in 0.01..9999.99) return false
        if (!name.any { it.isLetter() }) return false
        if (looksLikePhoneOrRef(name)) return false
        val lower = name.lowercase()
        if (lower.contains("total") || lower.contains("subtotal")) return false
        if (categorizeItem(name) == "stopKeywords") return false
        return true
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private fun cleanItemName(name: String): String =
        name.replace(Regex("[*@#]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { it.uppercase() }

    private fun parseMoneyNumber(s: String): Double? {
        val fixed = s
            .replace("l", "1", ignoreCase = false)
            .replace("I", "1", ignoreCase = false)
            .replace("O", "0", ignoreCase = false)
            .replace(",", ".")
        return fixed.toDoubleOrNull()
    }

    private val moneyRegex = Regex("""(?:£|\$)\s*([0-9lIOo]+(?:[.,][0-9]{2})?)(?![0-9!])""")

    private fun extractMoneyFromLine(line: String): Double? {
        val m = moneyRegex.find(line) ?: return null
        return parseMoneyNumber(m.groupValues[1])
    }

    private fun looksLikePhoneOrRef(line: String): Boolean {
        val l = line.lowercase().trim()
        if (l.startsWith("tel") || l.startsWith("phone") || l.contains("tel:")) return true
        if (l.matches(Regex("""^0\d{2,}.*"""))) return true
        if (l.replace(" ", "").matches(Regex("""^\d{8,}$"""))) return true
        return false
    }

    private fun looksLikeAddress(line: String): Boolean {
        val l = line.lowercase()
        if (l.contains("road") || l.contains("street") || l.contains("lane") ||
            l.contains("avenue") || l.contains("close") || l.contains("drive") ||
            l.contains("place") || l.contains("court")) return true
        if (l.contains("london") || l.contains("manchester") || l.contains("birmingham") ||
            l.contains("city") || l.contains("example city")) return true
        if (l.contains("sample") || l.contains("example")) return true
        // Has a number followed by a comma — likely "123 Sample St,"
        if (Regex("""^\d+\s+\S+.*,""").containsMatchIn(l)) return true
        // UK postcode
        if (Regex("""\b[A-Z]{1,2}\d[A-Z0-9]?\s*\d[A-Z]{2}\b""",
                RegexOption.IGNORE_CASE).containsMatchIn(l)) return true
        return false
    }

    private fun looksLikeDateOrTime(line: String): Boolean {
        val l = line.trim()
        if (l.matches(Regex("""^\d{1,2}:\d{2}.*$"""))) return true
        if (Regex("""\d{1,2}\s*[/-]\s*\d{1,2}\s*[/-]\s*\d{2,4}""").containsMatchIn(l)) return true
        return false
    }

    private fun looksLikePersonName(line: String): Boolean {
        val words = line.trim().split(" ")
        if (words.size != 2) return false
        // Two words, both starting with uppercase, both all letters = likely a name
        return words.all { word ->
            word.isNotEmpty() &&
                    word[0].isUpperCase() &&
                    word.all { it.isLetter() }
        }
    }

    private fun isNoiseLine(line: String): Boolean {
        val l = line.lowercase().trim()
        if (l.contains("www.") || l.contains("http")) return true
        if (l.contains("tell us") || l.contains("enter for a chance")) return true
        if (l.contains("gift card") || l.startsWith("win ")) return true
        if (l.contains("nectar") || l.contains("points")) return true
        if (l.contains("visa") || l.contains("mastercard") ||
            l.contains("debit") || l.contains("credit")) return true
        if (l.contains("auth") || l.contains("terminal") || l.contains("operator")) return true
        if (l.contains("****") || l.matches(Regex("""^\*{2,}\d+.*$"""))) return true
        if (l.replace(" ", "").matches(Regex("""^\d{10,}$"""))) return true
        if (l.matches(Regex("""^[e\s]+k?\s*\d+.*$"""))) return true
        if (l.matches(Regex("""^[a-z]\s+[a-z]\s+\d+.*$"""))) return true
        return false
    }

    // ─────────────────────────────────────────────────────────────
    // DATE / TIME
    // ─────────────────────────────────────────────────────────────

    private fun extractDate(text: String): String? {
        val patterns = listOf(
            Regex("""(\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4})\s+\d{1,2}:\d{2}"""),
            Regex("""\d{1,2}\s*[/\-]\s*\d{1,2}\s*[/\-]\s*\d{2,4}"""),
            Regex("""\d{4}\s*[/\-]\s*\d{1,2}\s*[/\-]\s*\d{1,2}"""),
            Regex("""[A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4}"""),
            Regex("""\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{4}""",
                RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val date = try {
                match.groups[1]?.value ?: match.value
            } catch (e: IndexOutOfBoundsException) {
                match.value
            }.trim()
            if (date.length >= 6 && date.any { it.isDigit() }) {
                Log.d("PARSER", "Found date: $date")
                return date
            }
        }
        Log.d("PARSER", "No date found")
        return null
    }

    private fun extractTime(text: String): String? {
        val lines = text.lines()
        for (line in lines) {
            if (line.trim().matches(Regex("""^\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4}$"""))) continue
            val match = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(line) ?: continue
            val hour = match.groupValues[1].toIntOrNull() ?: continue
            val minute = match.groupValues[2].toIntOrNull() ?: continue
            if (hour > 23 || minute > 59) continue
            val before = line.substring(0, match.range.first)
            if (before.trimEnd().endsWith("/")) continue
            return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────
    // TOTALS
    // ─────────────────────────────────────────────────────────────

    private fun extractTotal(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val totalKeywords = listOf("total amount", "amount due", "balance due")

        for (ln in lines) {
            val lower = ln.lowercase()
            if (totalKeywords.any { lower.contains(it) } && !lower.contains("win")) {
                val m = moneyRegex.find(ln)
                if (m != null) {
                    val v = parseMoneyNumber(m.groupValues[1])
                    if (v != null && v > 0) {
                        Log.d("PARSER", "Total from 'total amount': £$v")
                        return v
                    }
                }
            }
        }
        for (ln in lines) {
            val lower = ln.lowercase()
            if (lower.startsWith("total") && !lower.contains("win") &&
                !lower.contains("chance") && !lower.contains("amount")) {
                val m = moneyRegex.find(ln)
                if (m != null) {
                    val v = parseMoneyNumber(m.groupValues[1])
                    if (v != null && v > 0) {
                        Log.d("PARSER", "Total from 'total' line: £$v")
                        return v
                    }
                }
            }
        }


        // Use findAll to get all matches
        val allCandidates = mutableListOf<Double>()
        for (ln in lines) {
            if (isNoiseLine(ln)) continue
            val lower = ln.lowercase()
            if (lower.contains("win") || lower.contains("chance") ||
                lower.contains("prize") || lower.contains("gift")) continue
            moneyRegex.findAll(ln).forEach { match ->
                val v = parseMoneyNumber(match.groupValues[1])
                if (v != null && v in 0.01..9999.99) allCandidates.add(v)
            }
        }

        return allCandidates.maxOrNull()
            .also { Log.d("PARSER", "Total fallback: £$it") }
    }

    private fun extractTax(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Strategy 1: keyword and price on same line
        for (ln in lines) {
            val lower = ln.lowercase()
            if ((lower.contains("tax") || lower.contains("vat")) &&
                !lower.contains("total") && !lower.contains("win")) {
                val m = moneyRegex.find(ln)
                if (m != null) {
                    val value = parseMoneyNumber(m.groupValues[1])
                    if (value != null && value > 0) return value
                }
            }
        }

        // Strategy 2: keyword on separate line from prices
        // Find all money values, then use position relative to total
        val total = extractTotal(text)
        val allMoney = lines
            .filter { !isNoiseLine(it) }
            .filter { ln ->
                val lower = ln.lowercase()
                !lower.contains("win") && !lower.contains("chance") &&
                        !lower.contains("prize") && !lower.contains("gift") &&
                        !lower.startsWith("total")
            }
            .mapNotNull { extractMoneyFromLine(it) }
            .filter { it in 0.01..9999.99}

        // Check if receipt has a "Tax" keyword line — if yes, use position logic
        val hasTaxLabel = lines.any { ln ->
            val lower = ln.lowercase().trim()
            lower == "tax" || lower == "vat"
        }

        if (!hasTaxLabel) return null

        // Structure is always: [items..., subtotal, tax, total]
        // Tax = second to last value (before total)
        val withoutTotal = if (total != null && allMoney.lastOrNull() == total)
            allMoney.dropLast(1) else allMoney

        return if (withoutTotal.size >= 2) {
            val taxValue = withoutTotal.last()
            Log.d("PARSER", "Found tax (position after label): £$taxValue")
            taxValue
        } else null
    }

    private fun extractSubtotal(text: String): Double? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Strategy 1: keyword and price on same line
        for (ln in lines) {
            val lower = ln.lowercase()
            if (lower.contains("subtotal") || lower.contains("sub total") ||
                lower.contains("sub-total")) {
                val m = moneyRegex.find(ln)
                if (m != null) {
                    val value = parseMoneyNumber(m.groupValues[1])
                    if (value != null && value > 0) return value
                }
            }
        }

        // Strategy 2: has subtotal label but price is in separate block
        val hasSubtotalLabel = lines.any { ln ->
            val lower = ln.lowercase().trim()
            lower == "subtotal" || lower == "sub total" || lower == "sub-total"
        }

        if (!hasSubtotalLabel) {
            // Try deriving from total - tax
            val tax = extractTax(text)
            if (tax != null && tax > 0) {
                val total = extractTotal(text)
                if (total != null) return (total - tax).coerceAtLeast(0.0)
            }
            return null  // No label and no tax = show —
        }

        // Has label — structure: [items..., subtotal, tax, total]
        // Subtotal = third to last value
        val total = extractTotal(text)
        val allMoney = lines
            .filter { !isNoiseLine(it) }
            .mapNotNull { extractMoneyFromLine(it) }
            .filter { it in 0.01..9999.99 }

        val withoutTotal = if (total != null && allMoney.lastOrNull() == total)
            allMoney.dropLast(1) else allMoney

        return if (withoutTotal.size >= 2) {
            val subtotalValue = withoutTotal[withoutTotal.size - 2]
            Log.d("PARSER", "Found subtotal (position): £$subtotalValue")
            subtotalValue
        } else null
    }

    // ─────────────────────────────────────────────────────────────
    // CATEGORISATION
    // ─────────────────────────────────────────────────────────────

    internal fun categorizeItem(itemName: String): String {
        val lower = itemName.lowercase()
        return when {
            listOf("milk","cheese","yogurt","butter","cream","egg").any { it in lower } -> "dairy"
            listOf("banana","apple","orange","lettuce","tomato","potato","onion",
                "salad","fruit","veg").any { it in lower } -> "produce"
            listOf("water","juice","coffee","tea","soda","latte","cappuccino",
                "espresso","mocha","americano","macchiato").any { it in lower } -> "beverage"
            listOf("chicken","beef","pork","fish","turkey","bacon","ham","meat",
                "sandwich").any { it in lower } -> "meat"
            listOf("bread","bagel","donut","cake","cookie","biscuit","croissant",
                "muffin","pastry","cereal").any { it in lower } -> "bakery"
            listOf("shirt","tshirt","t-shirt","tee","blouse","tank","hoodie","sweater",
                "jumper","crop top","top","polo","cardigan").any { it in lower } -> "tops"
            listOf("jeans","pants","trousers","shorts","leggings","skirt","cargo",
                "joggers","dress").any { it in lower } -> "bottoms"
            listOf("jacket","coat","parka","blazer","windbreaker","puffer").any { it in lower } -> "outerwear"
            listOf("shoes","sneakers","trainers","boots","heels","sandals",
                "flip flops","slides").any { it in lower } -> "shoes"
            listOf("hat","cap","scarf","gloves","belt","sunglasses","watch",
                "jewelry","earrings","necklace","bracelet","ring").any { it in lower } -> "accessories"
            listOf("bag","handbag","purse","backpack","tote","wallet","duffel").any { it in lower } -> "bags"
            listOf("shampoo","conditioner","soap","detergent","toothpaste","deodorant",
                "moisturiser","lotion").any { it in lower } -> "toiletries"
            listOf("shelf","lamp","rug","plant","pot","candle","frame","cushion",
                "mop","batteries","lightbulb").any { it in lower } -> "homeware"
            listOf("subtotal","sub total","sub-total","tax","vat","total",
                "clubcard","savings","saving","visa","mastercard","amex","debit","credit",
                "payment","paid","change","balance","join","today","thank","visit","www",
                "cashier","operator","terminal","auth","ref").any { it in lower } -> "stopKeywords"
            else -> "other"
        }
    }

    // ─────────────────────────────────────────────────────────────
    // KNOWN STORES
    // ─────────────────────────────────────────────────────────────

    private val knownStores = listOf(
        "tesco", "sainsbury", "asda", "morrisons", "waitrose", "aldi", "lidl",
        "co-op", "coop", "co op", "iceland", "marks", "m&s", "marks and spencer",
        "pret", "costa", "starbucks", "greggs", "dunkin", "caffe nero", "nero",
        "primark", "wilko", "boots", "superdrug", "ikea", "argos", "next", "matalan",
        "h&m", "hm", "zara", "topshop", "jd sports", "sports direct", "nike", "adidas",
        "amazon", "asos", "shein", "ebay", "very", "mcdonalds", "kfc", "subway",
        "pizza", "nando", "wagamama", "itsu", "leon"
    )

    private fun extractShipping(
        text: String,
        items: List<ParsedItem>,
        total: Double?,
        tax: Double?
    ): Double? {

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Strategy 1: keyword on same line as price
        for (ln in lines) {
            val lower = ln.lowercase()
            if ((lower.contains("shipping") || lower.contains("delivery") ||
                        lower.contains("postage")) &&
                !lower.contains("shipping to") &&
                !lower.contains("address")) {
                val m = moneyRegex.find(ln)
                if (m != null) {
                    val value = parseMoneyNumber(m.groupValues[1])
                    if (value != null && value in 0.01..99.99) {
                        Log.d("PARSER", "Found shipping (keyword): £$value")
                        return value
                    }
                }
            }
        }

        // Strategy 2: remainder calculation
        // BUT only if this receipt has NO subtotal/tax/total labels at all
        // If it has those labels, the remainder IS the tax — not shipping
        val hasFinancialLabels = lines.any { ln ->
            val lower = ln.lowercase().trim()
            lower == "tax" || lower == "vat" ||
                    lower == "subtotal" || lower == "sub total" ||
                    lower.startsWith("subtotal") || lower.startsWith("sub total") ||
                    lower == "total" || lower.startsWith("total")
        }

        if (hasFinancialLabels) {
            Log.d("PARSER", "Receipt has financial labels — skipping shipping remainder calc")
            return null
        }

        // Only calculate remainder for receipts WITHOUT financial labels
        // (pure online order format: items + shipping + total, no tax/subtotal)
        if (total != null && items.isNotEmpty()) {
            val itemsSum = items.sumOf { it.price * it.quantity }
            val taxAmount = tax ?: 0.0
            val remainder = total - itemsSum - taxAmount
            val rounded = Math.round(remainder * 100.0) / 100.0

            if (rounded in 0.01..99.99) {
                Log.d("PARSER", "Calculated shipping from remainder: £$rounded")
                return rounded
            }
        }

        return null
    }
}
