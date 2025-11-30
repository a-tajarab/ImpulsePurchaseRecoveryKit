package com.example.impulsepurchaserecoverykit

data class ParsedItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val category: String = "other"
)


/**
 * Represents a complete parsed receipt
 */
data class ParsedReceipt(
    val storeName: String?,
    val purchaseDate: String?,
    val items: List<ParsedItem>,
    val subtotal: Double?,
    val tax: Double?,
    val total: Double?,
    val rawText: String // Keep original for debugging
) {
    /**
     * Checks if the receipt was parsed successfully
     */
    fun isValid(): Boolean {
        return total != null && total > 0.0 && items.isNotEmpty()
    }

    /**
     * Get a summary of the receipt for logging
     */
    fun getSummary(): String {
        return """
            Store: ${storeName ?: "Unknown"}
            Date: ${purchaseDate ?: "Unknown"}
            Items: ${items.size}
            Total: $${total ?: "Unknown"}
        """.trimIndent()
    }
}