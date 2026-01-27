package com.example.impulsepurchaserecoverykit.ui

import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.impulsepurchaserecoverykit.navigation.Screen
import com.example.impulsepurchaserecoverykit.navigation.bottomNavItems
import com.example.impulsepurchaserecoverykit.ui.screens.*
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.screens.ScanScreen


@Composable
fun AppRoot(
    viewModel: ReceiptViewModel,
    onScanReceiptPicked: (Uri) -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onScanClick = { navController.navigate(Screen.Scan.route) },
                    onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetail.create(id)) }
                )
            }

            composable(Screen.Scan.route) {
                ScanScreen(
                    onScanReceipt = { uri ->
                        onScanReceiptPicked(uri)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Receipts.route) {
                ReceiptListScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetail.create(id)) }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(paddingValues = padding, viewModel = viewModel)
            }

            composable(Screen.ReceiptDetail.route) { backStack ->
                val receiptId = backStack.arguments?.getString("receiptId")?.toLongOrNull()
                    ?: return@composable

                ReceiptDetailScreen(
                    paddingValues = padding,
                    receiptId = receiptId,
                    viewModel = viewModel,
                    onSetRegret = { navController.navigate(Screen.Regret.create(receiptId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Regret.route) { backStack ->
                val receiptId = backStack.arguments?.getString("receiptId")?.toLongOrNull()
                    ?: return@composable

                RegretScoreScreen(
                    paddingValues = padding,
                    receiptId = receiptId,
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}