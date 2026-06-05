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
 * HomeScreen — the main landing screen of the application.
 * Displays the teal header, three stat cards, scan button,
 * insight card, and the five most recent receipts.
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
        // Teal header with app title
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

        // ── Insight card ── only shown when average regret data exists
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

        // ── Goal progress toggle card ──
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


        // ── Recent receipts header ──
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

        // ── Recent receipts list ──
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
 * Small summary card displaying a single statistic.
 * Used for This Month, Receipts, and Avg Regret on the Home screen.
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
 * Contextual insight card shown below the scan button.
 * Colour and message change based on the user's average regret score:
 * - Score 7+ = high regret warning (teal)
 * - Score 4-6 = medium regret caution (amber)
 * - Score below 4 = positive reinforcement (green)
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
 * Individual receipt card shown in the recent purchases list.
 * Displays store initial circle, store name, date, total amount,
 * impulse label badge, regret score badge, and an impulse score
 * progress bar at the bottom of the card.
 */
@Composable
private fun HomeReceiptCard(
    receipt: ReceiptEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store initial circle
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

            // Store name + date
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

            // Right side — amount + badges
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
                    receipt.impulseLabel?.let { label ->
                        ImpulseBadge(label = label)
                    }
                    receipt.regretScore?.let { score ->
                        RegretBadge(score = score)
                    }
                }
            }
        }

        // Impulse score bar at bottom of card
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
                                else -> Success700
                            }
                        )
                )
            }
        }
    }
}

/**
 * Small coloured badge showing the impulse risk label — HIGH, MEDIUM, or LOW.
 * Colour coded red for HIGH, amber for MEDIUM, and green for LOW.
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
 * Small coloured badge showing the user's regret score out of 10.
 * Colour coded teal for high regret, amber for medium, and green for low.
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
 * Small coloured badge showing the user's regret score out of 10.
 * Colour coded teal for high regret, amber for medium, and green for low.
 */
@Composable
private fun EmptyStateCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
            Text("Total: ${
                receipt.totalAmount?.let {"£%.2f".format(it) } ?: "-"
            }"
            )
            Text("Regret: ${receipt.regretScore?.toString() ?: "Not rated"}")
        }
    }
}

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
            // ── Empty state ────────────────────────────────────────────────
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
            // ── Goal set: centred donut + spend info + saving chips ────────
            val rawProgress = (monthlySpend / goal.monthlyLimit).coerceIn(0.0, 1.0).toFloat()
            val isOver = monthlySpend > goal.monthlyLimit
            val arcColor = when {
                isOver -> Error700
                rawProgress >= 0.8f -> Terra500
                else -> Teal700
            }
            val animatedProgress by animateFloatAsState(
                targetValue = rawProgress,
                animationSpec = tween(durationMillis = 900),
                label = "goalArcProgress"
            )

            // Top-priority goal (lowest priority number)
            val topGoal = savingGoals.minByOrNull { it.priority }
            val extraCount = (savingGoals.size - 1).coerceAtLeast(0)

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Donut arc ──────────────────────────────────────────────
                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 9.dp.toPx()
                        val inset = strokeWidth / 2f
                        val topLeft = Offset(inset, inset)
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        drawArc(
                            color = arcColor.copy(alpha = 0.12f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
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

                // ── Label + amount ─────────────────────────────────────────
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

                    // ── Saving goal chips ──────────────────────────────────
                    if (topGoal != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Top-priority goal chip (teal)
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
                            // "+N more" overflow chip (terracotta)
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