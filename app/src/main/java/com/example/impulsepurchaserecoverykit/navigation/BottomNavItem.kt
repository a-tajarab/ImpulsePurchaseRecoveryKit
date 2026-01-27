package com.example.impulsepurchaserecoverykit.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector


data class BottomNavItem (
    val label: String,
    val icon: ImageVector,
    val route: String
    )

    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Screen.Home.route),
        BottomNavItem("Receipts", Icons.Filled.ReceiptLong, Screen.Receipts.route),
        BottomNavItem("Stats", Icons.Filled.ShowChart, Screen.Stats.route),
    )