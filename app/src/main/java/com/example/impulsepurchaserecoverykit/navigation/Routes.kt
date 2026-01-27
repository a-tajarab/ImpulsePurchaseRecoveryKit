package com.example.impulsepurchaserecoverykit.navigation
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Receipts : Screen("receipts")
    data object Stats : Screen("stats")
    data object Scan : Screen("scan")

    data object ReceiptDetail : Screen("receipt/{receiptId}") {
        fun create(receiptId: Long) = "receipt/$receiptId"
    }

    data object Regret : Screen("regret/{receiptId}") {
        fun create(receiptId: Long) = "regret/$receiptId"
    }
}
