package com.example.impulsepurchaserecoverykit.navigation

import com.example.impulsepurchaserecoverykit.ui.screens.OcrFailureReason

/**
 * Defines all navigation routes in the app as a type-safe sealed class hierarchy.
 *
 * Each object inside Screen represents a unique destination in the Jetpack
 * Navigation graph. Simple screens use a plain string route, while screens
 * that require arguments define a route template with a placeholder and
 * provide a [create] helper function to build the final route string safely
 * at the call site — avoiding raw string concatenation throughout the codebase.
 *
 * Navigation flow:
 * - App launches → [Splash] → [Onboarding] (first run only) → [Home]
 * - Scan FAB → [Scan] → [ReceiptDetail] or [OcrFailure]
 * - Receipt card tap → [ReceiptDetail] → [Regret]
 * - Bottom nav → [Home], [Receipts], [Stats], [Bot]
 *
 * @property route The raw route string registered with the Jetpack NavHost.
 *                 For argument-bearing screens this includes placeholder
 *                 segments such as `{receiptId}` or `{reason}`.
 */
sealed class Screen(val route: String) {

    /** Initial splash screen shown briefly while the app initialises. */
    data object Splash : Screen("splash")

    /** Onboarding screen shown to first-time users explaining the app's purpose. */
    data object Onboarding : Screen("onboarding")

    /** Home screen — spend summary, budget donut and recent receipts. */
    data object Home : Screen("home")

    /** Receipts screen — full purchase history grouped by month. */
    data object Receipts : Screen("receipts")

    /** Stats screen — spending, regret and category analytics charts. */
    data object Stats : Screen("stats")

    /** Scan screen — camera viewfinder for scanning a physical receipt. */
    data object Scan : Screen("scan")

    /** KIRA screen — AI spending coach conversational chat interface. */
    data object Bot : Screen("bot")

    /** Manual entry screen — allows the user to log a receipt without scanning. */
    data object ManualEntry : Screen("manual_entry")

    /**
     * Edit receipt screen — allows the user to correct or update a scanned receipt.
     *
     * @see create
     */
    data object EditReceipt : Screen("edit_receipt/{receiptId}") {
        /**
         * Builds the fully resolved route string for navigating to this screen.
         * @param receiptId The ID of the receipt to edit
         * @return The resolved route string with the receipt ID substituted in
         */
        fun create(receiptId: Long) = "edit_receipt/$receiptId"
    }

    /**
     * OCR failure screen — shown when text recognition fails or produces unusable output.
     * Offers the user options to retry the scan or switch to manual entry.
     *
     * @see create
     */
    data object OcrFailure : Screen("ocr_failure/{reason}") {
        /**
         * Builds the fully resolved route string for navigating to this screen.
         * @param reason The [OcrFailureReason] describing why OCR failed
         * @return The resolved route string with the failure reason substituted in
         */
        fun create(reason: OcrFailureReason) = "ocr_failure/${reason.name}"
    }

    /**
     * Receipt detail screen — shows the full parsed receipt with items,
     * impulse score, regret rating, and per-item sentiment reactions.
     *
     * @see create
     */
    data object ReceiptDetail : Screen("receipt/{receiptId}") {
        /**
         * Builds the fully resolved route string for navigating to this screen.
         * @param receiptId The ID of the receipt to display
         * @return The resolved route string with the receipt ID substituted in
         */
        fun create(receiptId: Long) = "receipt/$receiptId"
    }

    /**
     * Regret rating screen — allows the user to score their overall shopping
     * experience from 1–10 and select the emotion that best describes their
     * mood during the purchase.
     *
     * @see create
     */
    data object Regret : Screen("regret/{receiptId}") {
        /**
         * Builds the fully resolved route string for navigating to this screen.
         * @param receiptId The ID of the receipt being rated
         * @return The resolved route string with the receipt ID substituted in
         */
        fun create(receiptId: Long) = "regret/$receiptId"
    }

    /** Monthly goal screen — budget donut arc and saving goals management. */
    data object Goal : Screen("goal")
}