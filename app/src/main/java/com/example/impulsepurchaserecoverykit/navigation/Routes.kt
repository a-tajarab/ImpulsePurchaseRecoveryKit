package com.example.impulsepurchaserecoverykit.navigation

import com.example.impulsepurchaserecoverykit.ui.screens.OcrFailureReason

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Receipts : Screen("receipts")
    data object Stats : Screen("stats")
    data object Scan : Screen("scan")
    data object Bot : Screen("bot")
    data object ManualEntry : Screen("manual_entry")
    data object OcrFailure : Screen("ocr_failure/{reason}"){
        fun create(reason : OcrFailureReason) = "ocr_failure/${reason.name}"
    }
    data object ReceiptDetail : Screen("receipt/{receiptId}") {
        fun create(receiptId: Long) = "receipt/$receiptId"
    }

    data object Regret : Screen("regret/{receiptId}") {
        fun create(receiptId: Long) = "regret/$receiptId"
    }
}
