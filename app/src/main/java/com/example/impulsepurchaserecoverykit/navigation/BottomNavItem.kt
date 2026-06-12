package com.example.impulsepurchaserecoverykit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a single item in the app's bottom navigation bar.
 *
 * Each BottomNavItem holds the display label, icon, and navigation route
 * for one of the four main destinations in the app. The bottom navigation
 * bar is defined in [AppRoot] and uses this data class to render each tab
 * and handle navigation between top-level screens.
 *
 * @property label The text label displayed beneath the icon in the nav bar,
 *                 for example "Home" or "KIRA"
 * @property icon The Material icon displayed in the nav bar for this destination
 * @property route The navigation route string used by Jetpack Navigation to
 *                 identify and navigate to this screen
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

/**
 * The complete list of items displayed in the app's bottom navigation bar.
 *
 * Defines the four main destinations of the app in the order they appear
 * left to right. The centre slot in the navigation bar is reserved for the
 * scanner FAB and is not included here — it is rendered separately in [AppRoot]
 * as a raised [FloatingActionButton] above the bar.
 *
 * Destinations:
 * - **Home** — spend summary, recent receipts and budget donut
 * - **Receipts** — full purchase history grouped by month
 * - **Stats** — spending, regret and category charts
 * - **KIRA** — AI spending coach chat
 */
val bottomNavItems = listOf(
    BottomNavItem("Home", Icons.Filled.Home, Screen.Home.route),
    BottomNavItem("Receipts", Icons.Filled.ReceiptLong, Screen.Receipts.route),
    BottomNavItem("Stats", Icons.Filled.ShowChart, Screen.Stats.route),
    BottomNavItem("KIRA", Icons.Filled.SmartToy, Screen.Bot.route)
)