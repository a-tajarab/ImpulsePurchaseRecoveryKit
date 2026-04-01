package com.example.impulsepurchaserecoverykit.ui

import android.content.Context
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    //This checks if the user has seen onboarding before
    val prefs = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
    val ocrFailureReason by viewModel.ocrFailureReason.collectAsState()
    LaunchedEffect(ocrFailureReason) {
        ocrFailureReason?.let { reason ->
            navController.navigate(Screen.OcrFailure.create(reason))
            viewModel.setOcrFailureReason(null)
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // This hides the bottom nav on splash and onboarding
            val hideNav = currentRoute == Screen.Splash.route ||
                    currentRoute == Screen.Onboarding.route

            if (!hideNav){
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if(item.route == Screen.Home.route){
                                    navController.navigate(Screen.Home.route){
                                        popUpTo(navController.graph.findStartDestination().id){
                                            inclusive = true
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                } else {
                                    navController.navigate(item.route){
                                        popUpTo(navController.graph.findStartDestination().id){
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label)},
                            label = { Text(item.label)}
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            composable(Screen.Splash.route){
                SplashScreen(
                    onSplashComplete = {
                        val hasSeenOnboarding = prefs.getBoolean("onboarding_complete", false)
                        val destination = if (hasSeenOnboarding){
                            Screen.Home.route
                        } else {
                            Screen.Onboarding.route
                        }
                        navController.navigate(destination){
                            popUpTo(Screen.Splash.route){
                                inclusive = true
                            }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route){
                OnboardingScreen(
                    onOnboardingComplete = {
                        prefs.edit().putBoolean("onboarding_complete", true).apply()
                        navController.navigate(Screen.Home.route){
                            popUpTo(Screen.Onboarding.route){ inclusive = true}
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onScanClick = { navController.navigate(Screen.Scan.route) },
                    onReceiptClick = { id ->
                        navController.navigate(Screen.ReceiptDetail.create(id)) }
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
                    onReceiptClick = { id ->
                        navController.navigate(Screen.ReceiptDetail.create(id)) }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(paddingValues = padding, viewModel = viewModel)
            }

            composable(Screen.Bot.route){
                SuggestionBotScreen(paddingValues = padding)
            }

            composable(Screen.ReceiptDetail.route) { backStack ->
                val receiptId = backStack.arguments?.getString("receiptId")?.toLongOrNull()
                    ?: return@composable

                ReceiptDetailScreen(
                    paddingValues = padding,
                    receiptId = receiptId,
                    viewModel = viewModel,
                    onSetRegret = { navController.navigate(Screen.Regret.create(receiptId)) },
                    onEdit = { navController.navigate(Screen.EditReceipt.create(receiptId)) },
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
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                             },
                    onViewStats = {
                        navController.navigate(Screen.Stats.route) {
                            popUpTo(Screen.Home.route){
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("ocr_failure/{reason}") { backStack ->
                val reasonName = backStack.arguments?.getString("reason") ?: return@composable
                val reason = OcrFailureReason.valueOf(reasonName)

                OcrFailureScreen(
                    paddingValues = padding,
                    reason = reason,
                    onTryAgain = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onManualEntry = {
                        navController.navigate(Screen.ManualEntry.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onDismiss = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.ManualEntry.route){
                ManualEntryScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onSaved = { receiptId ->
                        navController.navigate(Screen.Regret.create(receiptId)){
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBack = { navController.popBackStack()}
                )
            }

        }
    }
}