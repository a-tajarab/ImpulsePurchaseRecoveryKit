package com.example.impulsepurchaserecoverykit.debug

data class ReceiptItem(
    val name: String,
    val price: Double
)

data class ReceiptData(
    val storeName: String,
    val addressLines: List<String>,
    val phone: String?,
    val dateTime: String,          // "15/10/2025 14:22"
    val items: List<ReceiptItem>,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val paymentMethod: String,     // "VISA DEBIT"
    val last4: String              // "1234"
)

