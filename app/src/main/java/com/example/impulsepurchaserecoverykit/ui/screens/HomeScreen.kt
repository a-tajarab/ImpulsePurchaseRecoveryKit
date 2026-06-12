package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import com.example.impulsepurchaserecoverykit.database.entities.GoalEntity
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.entities.SavingGoalEntity
import com.example.impulsepurchaserecoverykit.navigation.Screen
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*
import kotlin.collections.minByOrNull

/**
 * The main landing screen of the application.
 *
 * HomeScreen is the first screen users see after onboarding and serves as
 * the central hub for understanding their spending at a glance. It is built
 * as a single [LazyColumn] containing the following sections in order:
 *
 * 1. **Primary header** — app title and tagline on a midnight navy background
 * 2. **Stat cards** — three cards showing This Month spend, total receipt count,
 *    and average regret score across all receipts
 * 3. **Insight card** — a contextual message and icon that changes colour and
 *    tone based on the user's average regret level
 * 4. **Goal toggle card** — a compact budget donut showing monthly spend vs limit,
 *    saving goal chips, and a prompt to set a budget if none exists
 * 5. **Recent purchases** — the five most recently added receipts, each showing
 *    store name, date, total, impulse label, and an impulse score progress bar
 *
 * All data is observed reactively from [ReceiptViewModel] via [collectAsState],
 * so the screen updates automatically as receipts are added or edited.
 *
 * @param paddingValues Padding applied by the parent [Scaffold] to avoid
 *                      overlap with system bars and the bottom navigation bar
 * @param viewModel The shared [ReceiptViewModel] providing all spending data
 * @param onReceiptClick Callback invoked with a receipt ID when the user taps
 *                       a receipt card — navigates to the Receipt Detail screen
 * @param onGoalClick Callback invoked when the user taps the goal toggle card —
 *                    navigates to the Spending Goal screen
 */
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onReceiptClick: (Long) -> Unit,
    onGoalClick: () -> Unit
) {
    val recentReceipts by viewModel.getRecentReceipts(5).collectAsState(initial = emptyList())
    val totalSpend by viewModel.getTotalSpend().collectAsState(initial = null)
    val avgRegret by viewModel.averageRegret.collectAsState(initial = null)
    val receiptCount by viewModel.receiptCount.collectAsState(initial = 0)

    // Current month and year used to calculate This Month spending
    val now = remember { java.util.Calendar.getInstance() }
    val currentMonth = now.get(java.util.Calendar.MONTH) + 1
    val currentYear = now.get(java.util.Calendar.YEAR)
    val monthlySpend by viewModel.getMonthlySpend(currentYear, currentMonth)
        .collectAsState(initial = null)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        )
    ) {
        // Primary header — app title on midnight navy background
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Column {
                    Text(
                        "Impulse Kit",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Your spending, honestly tracked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Three stat cards — This Month, Receipts, Avg Regret
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "This month",
                    value = monthlySpend?.let { "£${"%.2f".format(it)}" } ?: "£0.00",
                    valueColor = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Receipts",
                    value = receiptCount.toString(),
                    valueColor = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Avg regret",
                    value = avgRegret?.let { "${"%.1f".format(it)}/10" } ?: "-",
                    valueColor = when {
                        avgRegret == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        avgRegret!! >= 7.0 -> Teal700
                        avgRegret!! >= 4.0 -> Warning700
                        else -> Success700
                    }
                )
            }
        }

        // Insight card — only shown once average regret data is available
        item {
            avgRegret?.let { regret ->
                InsightCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    avgRegret = regret,
                    receiptCount = receiptCount
                )
            }
        }

        // Goal toggle card — budget donut and saving goal chips
        item {
            val goal by viewModel.getGoal().collectAsState(initial = null)
            val now = remember { java.util.Calendar.getInstance() }
            val currentMonth = now.get(java.util.Calendar.MONTH) + 1
            val currentYear = now.get(java.util.Calendar.YEAR)
            val monthlySpend by viewModel.getMonthlySpend(currentYear, currentMonth)
                .collectAsState(initial = 0.0)
            val savingGoals by viewModel.getSavingGoals()
                .collectAsState(initial = emptyList())

            GoalToggleCard(
                goal = goal,
                monthlySpend = monthlySpend ?: 0.0,
                savingGoals = savingGoals,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                onClick = onGoalClick
            )
        }

        // Recent receipts section header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent purchases",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Recent receipts list — empty state shown when no receipts exist
        if (recentReceipts.isEmpty()) {
            item {
                EmptyStateCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(recentReceipts, key = { it.id }) { receipt ->
                HomeReceiptCard(
                    receipt = receipt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onReceiptClick(receipt.id) }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

/**
 * Small summary card displaying a single spending statistic.
 *
 * Used in a three-card row on the Home screen to show This Month spend,
 * total receipt count, and average regret score. The [valueColor] parameter
 * allows each card to colour-code its value independently — for example,
 * the regret card shifts from green to amber to teal as the score rises.
 *
 * @param modifier Optional [Modifier] applied to the root [Card]
 * @param label The small label shown above the value, for example "This month"
 * @param value The formatted value string to display prominently
 * @param valueColor The colour applied to the value text
 */
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Contextual insight card displayed below the stat row on the Home screen.
 *
 * Shows a short message and icon that adapts its colour, icon, and tone based
 * on the user's average regret score across all rated receipts:
 * - Score ≥ 7 — high regret warning in teal with a Warning icon
 * - Score ≥ 4 — medium caution in amber, suggesting KIRA for reflection
 * - Score < 4 — positive reinforcement in green with a CheckCircle icon
 *
 * The card is only rendered when [avgRegret] is non-null, so it does not
 * appear until the user has rated at least one receipt.
 *
 * @param modifier Optional [Modifier] applied to the root [Card]
 * @param avgRegret The user's average regret score across all rated receipts
 * @param receiptCount The total number of receipts logged — reserved for future
 *                     use in more nuanced insight messaging
 */
@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    avgRegret: Double,
    receiptCount: Int
) {
    val isHighRegret = avgRegret >= 7.0
    val isMedRegret = avgRegret >= 4.0

    val bgColor = when {
        isHighRegret -> Teal100
        isMedRegret  -> Warning100
        else         -> Success100
    }
    val textColor = when {
        isHighRegret -> Teal900
        isMedRegret  -> Warning700
        else         -> Success700
    }
    val icon = when {
        isHighRegret -> Icons.Default.Warning
        isMedRegret  -> Icons.Default.Warning
        else         -> Icons.Default.CheckCircle
    }
    val message = when {
        isHighRegret -> "Your regret score is high. Consider pausing before your next purchase."
        isMedRegret  -> "Mixed feelings about your spending. KIRA can help you reflect."
        else         -> "You're making considered purchases. Keep it up!"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Receipt card used in the Recent Purchases list on the Home screen.
 *
 * Displays a compact summary of a single [ReceiptEntity] including:
 * - A store initial circle in the app's primary container colour
 * - Store name and purchase date
 * - Total amount, impulse label badge, and regret score badge (if rated)
 * - A thin impulse score progress bar along the bottom edge of the card,
 *   colour-coded teal for HIGH (≥70), amber for MEDIUM (≥40), and green for LOW
 *
 * Tapping the card triggers [onReceiptClick] via the [Modifier.clickable]
 * applied by the parent [HomeScreen].
 *
 * @param receipt The [ReceiptEntity] to display
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
@Composable
private fun HomeReceiptCard(
    receipt: ReceiptEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store initial circle — falls back to "?" for receipts with no store name
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = receipt.storeName?.take(1)?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.width(12.dp))

            // Store name and date — store name truncates with ellipsis on long names
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = receipt.storeName ?: "Unknown store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = receipt.purchaseDate ?: "No date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right column — total amount, impulse label badge, regret score badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = receipt.totalAmount?.let { "£${"%.2f".format(it)}" } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    receipt.impulseLabel?.let { label -> ImpulseBadge(label = label) }
                    receipt.regretScore?.let { score -> RegretBadge(score = score) }
                }
            }
        }

        // Impulse score bar — thin strip along the bottom of the card showing
        // the raw 0-100 score as a proportional fill with colour coding
        receipt.impulseScore?.let { score ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(score / 100f)
                        .fillMaxHeight()
                        .background(
                            when {
                                score >= 70 -> Teal700
                                score >= 40 -> Warning700
                                else        -> Success700
                            }
                        )
                )
            }
        }
    }
}

/**
 * Small coloured badge displaying the impulse risk label for a receipt.
 *
 * Colour coded based on the label value:
 * - HIGH — error red background and text
 * - MEDIUM — amber background and text
 * - LOW / other — green background and text
 *
 * @param label The impulse label string — "HIGH", "MEDIUM", or "LOW"
 */
@Composable
private fun ImpulseBadge(label: String) {
    val (bg, fg) = when (label.uppercase()) {
        "HIGH"   -> Error100 to Error700
        "MEDIUM" -> Warning100 to Warning700
        else     -> Success100 to Success700
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

/**
 * Small coloured badge displaying the user's regret score for a receipt.
 *
 * Colour coded based on the score value:
 * - Score ≥ 7 — teal background (high regret)
 * - Score ≥ 4 — amber background (medium regret)
 * - Score < 4 — green background (low regret)
 *
 * @param score The regret score from 1 to 10
 */
@Composable
private fun RegretBadge(score: Int) {
    val bg = when {
        score >= 7 -> Teal100
        score >= 4 -> Warning100
        else       -> Success100
    }
    val fg = when {
        score >= 7 -> Teal900
        score >= 4 -> Warning700
        else       -> Success700
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$score/10",
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

/**
 * Empty state card shown in the Recent Purchases section when the user has
 * not yet scanned any receipts.
 *
 * Prompts the user to scan their first receipt using the central scanner FAB
 * in the bottom navigation bar.
 *
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
@Composable
private fun EmptyStateCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No receipts yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Scan your first receipt to start tracking your impulse purchases",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * Legacy receipt row composable — superseded by [HomeReceiptCard].
 *
 * This simpler row layout was the original receipt list item before the
 * card-based redesign. Retained in the codebase but no longer used in
 * the production UI. May be removed in a future cleanup.
 *
 * @param receipt The [ReceiptEntity] to display
 * @param onClick Callback invoked when the row is tapped
 */
@Composable
private fun ReceiptRow(receipt: ReceiptEntity, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(receipt.storeName ?: "Unknown store", style = MaterialTheme.typography.titleMedium)
            Text("Date: ${receipt.purchaseDate ?: "—"}")
            Text("Total: ${receipt.totalAmount?.let { "£%.2f".format(it) } ?: "-"}")
            Text("Regret: ${receipt.regretScore?.toString() ?: "Not rated"}")
        }
    }
}

/**
 * Compact budget card shown on the Home screen that summarises the user's
 * monthly spending goal and saving goals.
 *
 * The card has two distinct states:
 *
 * **No goal set** — shows a star icon with a prompt to set a monthly budget,
 * encouraging the user to navigate to the Spending Goal screen.
 *
 * **Goal set** — shows a 100dp animated donut arc displaying the percentage
 * of the monthly budget used, with the raw spend vs limit below it. The arc
 * colour shifts from midnight navy to warm stone at 80% and to error red when
 * the budget is exceeded. Below the donut, the highest-priority saving goal is
 * shown as a teal chip, with an overflow chip showing how many additional goals
 * exist if there are more than one.
 *
 * The entire card is tappable and navigates to the full Spending Goal screen
 * via [onClick].
 *
 * @param goal The user's current [GoalEntity] containing the monthly limit,
 *             or null if no budget has been set
 * @param monthlySpend The total amount spent so far in the current month in GBP
 * @param savingGoals The list of [SavingGoalEntity] records ordered by priority,
 *                    used to populate the saving goal chips
 * @param modifier Optional [Modifier] applied to the root [Card]
 * @param onClick Callback invoked when the card is tapped — navigates to the
 *                Spending Goal screen
 */
@Composable
fun GoalToggleCard(
    goal: GoalEntity?,
    monthlySpend: Double,
    savingGoals: List<SavingGoalEntity> = emptyList(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (goal == null) {
            // Empty state — prompt the user to set a monthly budget
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Teal700.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Teal700,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Set a monthly budget",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Track spending towards your goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Teal700.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            // Goal set — animated donut arc + spend info + saving goal chips
            val rawProgress = (monthlySpend / goal.monthlyLimit).coerceIn(0.0, 1.0).toFloat()
            val isOver = monthlySpend > goal.monthlyLimit

            // Arc colour shifts at 80% and again at 100% to signal urgency
            val arcColor = when {
                isOver           -> Error700
                rawProgress >= 0.8f -> Terra500
                else             -> Teal700
            }

            // Animated progress — smoothly transitions when the spend value updates
            val animatedProgress by animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = tween(durationMillis = 900),
                label = "goalArcProgress"
            )

            // Only show the highest-priority goal chip; count the rest as overflow
            val topGoal = savingGoals.minByOrNull { it.priority }
            val extraCount = (savingGoals.size - 1).coerceAtLeast(0)

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 100dp donut arc drawn on Canvas — track + filled arc
                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 9.dp.toPx()
                        val inset = strokeWidth / 2f
                        val topLeft = Offset(inset, inset)
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                        // Background track — full circle at low opacity
                        drawArc(
                            color = arcColor.copy(alpha = 0.12f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Filled arc — sweeps clockwise from the top
                        if (animatedProgress > 0f) {
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    // Percentage label centred inside the donut
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(rawProgress * 100).toInt()}%",
                            fontWeight = FontWeight.Black,
                            color = arcColor,
                            fontSize = 18.sp
                        )
                        Text(
                            "used",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                // Spend label — turns error red when over budget
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Monthly Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "£${"%.0f".format(monthlySpend)} of £${"%.0f".format(goal.monthlyLimit)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isOver) Error700 else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Saving goal chips — top priority in teal, overflow count in warm stone
                    if (topGoal != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Teal700.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "✦ ${topGoal.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Teal700
                                )
                            }
                            if (extraCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Terra500.copy(alpha = 0.13f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "+$extraCount more",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Terra700
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}