package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Data model representing a single page of the onboarding flow.
 *
 * Each page has a large emoji, a two-part heading (title + subtitle), a body
 * description explaining the feature, and an optional tip shown in a teal
 * card at the bottom of the page. The [accentColor] is reserved for future
 * per-page accent theming.
 *
 * @property emoji The large emoji displayed in the circular header icon
 * @property title The main heading text, may contain newlines for line breaks
 * @property subtitle A short supporting line shown below the title in the header
 * @property description The body text explaining the feature shown on this page
 * @property tip An optional quick-tip string shown in a highlighted card below
 *               the description, or null if no tip is needed for this page
 * @property accentColor The accent colour associated with this page — defaults
 *                       to [Teal700]. Currently used to style the Next button
 *                       on the final page.
 */
data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val tip: String? = null,
    val accentColor: androidx.compose.ui.graphics.Color = Teal700
)

/**
 * The four onboarding pages shown to first-time users.
 *
 * Each page introduces one of the app's core features in a warm,
 * non-judgemental tone:
 * 1. Welcome — sets the emotional tone of the app
 * 2. Receipt scanning — explains how OCR works
 * 3. Regret scoring — introduces the emotional tracking system
 * 4. KIRA — introduces the AI spending coach
 */
private val pages = listOf(
    OnboardingPage(
        emoji = "🧾",
        title = "Welcome to Your\nRecovery Kit",
        subtitle = "You're not alone.",
        description = "We all make purchases we later regret. The Impulse Purchase Recovery Kit is a kind, " +
                "judgement-free space to track your spending, understand your emotions, and build healthier habits, one receipt at a time.",
        tip = "It's not about guilt. It's about growth. 🌱",
        accentColor = Teal700
    ),
    OnboardingPage(
        emoji = "📸✨",
        title = "Scan Your Receipts",
        subtitle = "No manual entry needed.",
        description = "Simply point your camera at any receipt and our smart scanner reads it automatically. " +
                "Store name, items, prices and all — so you can focus on reflecting, not typing. ",
        tip = "Works with paper receipts, screenshots, and gallery photos!",
        accentColor = Teal500
    ),
    OnboardingPage(
        emoji = "😬❤️",
        title = "Rate How You Feel",
        subtitle = "Your emotions matter here.",
        description = "After logging a purchase, give it a Regret Score from 1 to 10. Over time, the app learns your emotional patterns," +
                " helping you spot the stores, times, and situations that trigger your impulse spending.",
        tip = "Even a score of 1 is useful data. Rate everything!",
        accentColor = Terra500
    ),
    OnboardingPage(
        emoji = "🤖💡",
        title = "Meet KIRA",
        subtitle = "Your personal spending coach.",
        description = "KIRA is your Kind Impulse Recovery Advisor. She already knows your spending history with the receipts you have " +
                "scanned and she is always ready to give warm, personalised advice whenever you need it. Ask her anything! she won't judge, only help.",
        tip = "Try asking: \"Should I buy this?\" and watch KIRA work her magic ✨",
        accentColor = Teal700
    )
)

/**
 * Onboarding flow shown to first-time users when they launch the app.
 *
 * Presents four pages in a [HorizontalPager] introducing the app's core
 * features. The screen is split into two visual zones:
 *
 * - **Header** — a fixed midnight navy panel showing the current page's emoji
 *   in a circular container, its title, and its subtitle. The header content
 *   updates immediately as the user swipes between pages.
 *
 * - **Content area** — the scrollable pager body below the header, containing
 *   the page description and an optional tip card. Users can swipe horizontally
 *   or use the navigation buttons to move between pages.
 *
 * Navigation controls at the bottom:
 * - **Skip** — visible on all pages except the last, dismisses onboarding immediately
 * - **Back** — visible from page 2 onwards, animates to the previous page
 * - **Next / Let's Go** — advances to the next page, or completes onboarding on
 *   the last page. The button colour changes from teal to warm stone on the
 *   final page to signal the transition into the main app.
 *
 * Page progress is indicated by an animated dot row below the pager. The
 * active dot expands from 8dp to 24dp width using [animateDpAsState] to
 * create a pill-shaped indicator that clearly shows the current position.
 *
 * Onboarding completion is persisted by the caller — [onOnboardingComplete]
 * is expected to write a flag to SharedPreferences so this screen is never
 * shown again on subsequent launches.
 *
 * @param onOnboardingComplete Callback invoked when the user either completes
 *                             all four pages or taps Skip — navigates to the
 *                             Home screen and marks onboarding as done
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1
    val currentPage = pages[pagerState.currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Fixed header — emoji, title, subtitle update as the pager page changes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp)
                    .padding(top = 52.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Circular emoji container — semi-transparent white tint on primary
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentPage.emoji,
                            fontSize = 42.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = currentPage.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = currentPage.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Swipeable content pager — description and optional tip card per page
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }

            // Animated dot indicators — active dot expands to a pill shape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    // Dot width animates between 8dp (inactive) and 24dp (active)
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        label = "dot_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Teal700
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            // Navigation buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Skip — shown on all pages except the last where it would be redundant
                if (!isLastPage) {
                    TextButton(
                        onClick = onOnboardingComplete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Skip",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Back — hidden on the first page since there is nowhere to go back to
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Next / Let's Go — colour shifts to warm stone on the final page
                // to visually signal the transition from onboarding into the main app
                Button(
                    onClick = {
                        if (isLastPage) {
                            onOnboardingComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLastPage) Terra500 else Teal700,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (isLastPage) "Let's Go! 🚀" else "Next →",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Content composable for a single onboarding page, rendered inside the [HorizontalPager].
 *
 * Displays the page's description text and, if present, an optional tip card
 * shown below the description in a teal [Card]. The tip card uses a lightbulb
 * emoji prefix and is styled with [Teal500] as the container colour so it
 * stands out visually from the plain body text above it.
 *
 * @param page The [OnboardingPage] whose content should be displayed
 */
@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .padding(top = 28.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Main description — centred body text explaining the feature
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 26.sp
        )

        // Optional tip card — only shown if the page provides a tip
        page.tip?.let { tip ->
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Teal500),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("💡", fontSize = 16.sp)
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Teal700,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}