package com.example.impulsepurchaserecoverykit.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.impulsepurchaserecoverykit.navigation.Screen
import com.example.impulsepurchaserecoverykit.navigation.bottomNavItems
import com.example.impulsepurchaserecoverykit.ui.screens.*
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.screens.ScanScreen
import com.example.impulsepurchaserecoverykit.ui.theme.Terra500

/**
 * Root composable that owns the [NavHost] and the [Scaffold] for the entire app.
 *
 * AppRoot is the single entry point for all navigation and is set as the content
 * in [MainActivity]. It wires together:
 *
 * - A [NavHost] with routes for every screen in the app
 * - A [ScannerBottomBar] that is hidden during [Screen.Splash] and [Screen.Onboarding]
 * - The OCR failure navigation side-effect via [LaunchedEffect]
 * - First-run detection via SharedPreferences to decide whether to show onboarding
 *
 * **Navigation flow:**
 * - App launches → [Screen.Splash]
 * - Splash completes → [Screen.Onboarding] (first run) or [Screen.Home] (returning user)
 * - Onboarding completes → [Screen.Home] (clears back stack, writes `onboarding_complete`)
 * - Scan FAB tapped → [Screen.Scan] → receipt picked → [onScanReceiptPicked] called,
 *   then [Screen.Scan] is popped from the back stack
 * - Receipt saved → [Screen.ReceiptDetail] → [Screen.Regret] (Rate Regret button)
 * - OCR fails → [Screen.OcrFailure] via [LaunchedEffect] on [ReceiptViewModel.ocrFailureReason]
 * - Manual entry → [Screen.ManualEntry] → [Screen.Regret] after saving
 * - Goal card tapped → [Screen.Goal]
 *
 * **SharedPreferences:**
 * The `onboarding_complete` boolean is stored in `app_prefs`. It is written to `true`
 * when the user completes or skips onboarding, and read during the splash → home
 * navigation decision on every subsequent launch.
 *
 * **OCR failure navigation:**
 * [ReceiptViewModel.ocrFailureReason] is set by [MainActivity] when the scan pipeline
 * fails. A [LaunchedEffect] observes this state and navigates to [Screen.OcrFailure]
 * automatically, then clears the reason so the navigation does not repeat on
 * recomposition.
 *
 * @param viewModel The shared [ReceiptViewModel] passed down to every screen that
 *                  needs database access
 * @param onScanReceiptPicked Callback invoked with the selected image [Uri] when the
 *                            user confirms a receipt in [ScanScreen]. Handed back to
 *                            [MainActivity] to begin the OCR and AI parsing pipeline.
 */
@Composable
fun AppRoot(
    viewModel: ReceiptViewModel,
    onScanReceiptPicked: (Uri) -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // SharedPreferences for first-run detection — persists onboarding completion
    val prefs = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    // Observe OCR failure reason set by MainActivity — navigate to failure screen when non-null
    val ocrFailureReason by viewModel.ocrFailureReason.collectAsState()
    LaunchedEffect(ocrFailureReason) {
        ocrFailureReason?.let { reason ->
            navController.navigate(Screen.OcrFailure.create(reason))
            // Clear immediately so this navigation does not trigger again on recomposition
            viewModel.setOcrFailureReason(null)
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Hide the bottom nav on Splash and Onboarding — these are full-screen experiences
            val hideNav = currentRoute == Screen.Splash.route ||
                    currentRoute == Screen.Onboarding.route

            if (!hideNav) {
                ScannerBottomBar(
                    currentRoute = currentRoute,
                    onNavClick = { route ->
                        if (route == Screen.Home.route) {
                            // Home clears the back stack entirely and resets to a fresh Home
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else {
                            // All other tabs save and restore their state on tab switch
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onScanClick = { navController.navigate(Screen.Scan.route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {
            // ── Splash ────────────────────────────────────────────────────
            // Navigates to Home or Onboarding after animation completes.
            // Removes itself from the back stack so Back does not return to Splash.
            composable(Screen.Splash.route) {
                SplashScreen(
                    onSplashComplete = {
                        val hasSeenOnboarding = prefs.getBoolean("onboarding_complete", false)
                        val destination = if (hasSeenOnboarding) Screen.Home.route else Screen.Onboarding.route
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Onboarding ────────────────────────────────────────────────
            // Writes onboarding_complete = true then navigates to Home.
            // Removes itself from the back stack so Back does not return to Onboarding.
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        prefs.edit().putBoolean("onboarding_complete", true).apply()
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Home ──────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetail.create(id)) },
                    onGoalClick = { navController.navigate(Screen.Goal.route) }
                )
            }

            // ── Scan ──────────────────────────────────────────────────────
            // After the user picks a receipt, onScanReceiptPicked is called and
            // ScanScreen is popped so the user returns to wherever they came from.
            composable(Screen.Scan.route) {
                ScanScreen(
                    onScanReceipt = { uri ->
                        onScanReceiptPicked(uri)
                        navController.popBackStack()
                    }
                )
            }

            // ── Receipts list ─────────────────────────────────────────────
            composable(Screen.Receipts.route) {
                ReceiptListScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onReceiptClick = { id -> navController.navigate(Screen.ReceiptDetail.create(id)) }
                )
            }

            // ── Stats ─────────────────────────────────────────────────────
            composable(Screen.Stats.route) {
                StatsScreen(paddingValues = padding, viewModel = viewModel)
            }

            // ── KIRA chat ─────────────────────────────────────────────────
            composable(Screen.Bot.route) {
                SuggestionBotScreen(paddingValues = padding)
            }

            // ── Receipt detail ────────────────────────────────────────────
            // receiptId is extracted from the route argument and passed directly —
            // returning@composable early if the argument is missing or unparseable.
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

            // ── Regret score ──────────────────────────────────────────────
            // After rating, navigate back to Home without creating a new Home instance.
            // onViewStats navigates to Stats while keeping Home in the back stack.
            composable(Screen.Regret.route) { backStack ->
                val receiptId = backStack.arguments?.getString("receiptId")?.toLongOrNull()
                    ?: return@composable

                RegretScoreScreen(
                    paddingValues = padding,
                    receiptId = receiptId,
                    viewModel = viewModel,
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onViewStats = {
                        navController.navigate(Screen.Stats.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ── OCR failure ───────────────────────────────────────────────
            // reason is decoded from the route argument using OcrFailureReason.valueOf.
            // Navigation options: retry scan, go to manual entry, or return to Home.
            composable("ocr_failure/{reason}") { backStack ->
                val reasonName = backStack.arguments?.getString("reason") ?: return@composable
                val reason = OcrFailureReason.valueOf(reasonName)

                OcrFailureScreen(
                    paddingValues = padding,
                    reason = reason,
                    onTryAgain = { navController.navigate(Screen.Scan.route) { popUpTo(Screen.Home.route) } },
                    onManualEntry = { navController.navigate(Screen.ManualEntry.route) { popUpTo(Screen.Home.route) } },
                    onDismiss = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = true } } }
                )
            }

            // ── Manual entry ──────────────────────────────────────────────
            // After saving, navigates to the Regret screen for the new receipt.
            composable(Screen.ManualEntry.route) {
                ManualEntryScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onSaved = { receiptId ->
                        navController.navigate(Screen.Regret.create(receiptId)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Spending goal ─────────────────────────────────────────────
            composable(Screen.Goal.route) {
                SpendingGoalScreen(
                    paddingValues = padding,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Custom bottom navigation bar with a raised scanner [FloatingActionButton] in the centre.
 *
 * The bar uses a 5-slot [NavigationBar] with an invisible disabled centre slot that
 * reserves horizontal space for the FAB. This keeps the two left tabs (Home, Receipts)
 * and two right tabs (Stats, KIRA) symmetrically spaced without the FAB overlapping any tab.
 *
 * The FAB is absolutely positioned in a [Box] wrapping the entire bar, aligned to
 * `Alignment.TopCenter` and offset upward by 18dp so it bleeds into the content area
 * above the nav bar. This matches the standard Material 3 raised-FAB-in-nav pattern
 * seen in the design guidelines.
 *
 * The centre slot uses transparent indicator and icon colours to be completely
 * invisible — it exists purely as a layout spacer.
 *
 * The bar is hidden on [Screen.Splash] and [Screen.Onboarding] — this is managed
 * by the calling code in [AppRoot] via the `hideNav` flag.
 *
 * @param currentRoute The current destination route from [NavController], used to
 *                     highlight the active [NavigationBarItem]
 * @param onNavClick Callback invoked with the route string when a tab is tapped.
 *                   The caller ([AppRoot]) handles the actual navigation.
 * @param onScanClick Callback invoked when the centre scanner FAB is tapped —
 *                    navigates to [Screen.Scan]
 */
@Composable
private fun ScannerBottomBar(
    currentRoute: String?,
    onNavClick: (String) -> Unit,
    onScanClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {

        NavigationBar {
            // Left two tabs: Home, Receipts
            bottomNavItems.take(2).forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onNavClick(item.route) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) }
                )
            }

            // Invisible centre spacer — reserves space for the raised FAB
            // The disabled item is fully transparent so nothing renders here
            NavigationBarItem(
                selected = false,
                onClick = {},
                enabled = false,
                icon = { /* transparent gap — FAB floats above this slot */ },
                label = {},
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    disabledIconColor = Color.Transparent
                )
            )

            // Right two tabs: Stats, KIRA
            bottomNavItems.drop(2).forEach { item ->
                NavigationBarItem(
                    selected = currentRoute == item.route,
                    onClick = { onNavClick(item.route) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) }
                )
            }
        }

        // Scanner FAB — warm stone circle raised 18dp above the nav bar
        // Positioned at TopCenter of the Box then offset upward so it bleeds
        // into the screen content above the navigation bar
        FloatingActionButton(
            onClick = onScanClick,
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-18).dp),
            shape = CircleShape,
            containerColor = Terra500,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Scan receipt", modifier = Modifier.size(26.dp))
        }
    }
}