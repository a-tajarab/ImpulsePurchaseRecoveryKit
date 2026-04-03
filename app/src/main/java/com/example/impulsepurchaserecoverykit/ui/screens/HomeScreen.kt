package com.example.impulsepurchaserecoverykit.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onScanClick: () -> Unit,
    onReceiptClick: (Long) -> Unit
) {
    val recentReceipts by viewModel.getRecentReceipts(5).collectAsState(initial = emptyList())
    val totalSpend by viewModel.getTotalSpend().collectAsState(initial = null)
    val avgRegret by viewModel.averageRegret.collectAsState(initial = null)
    val receiptCount by viewModel.receiptCount.collectAsState(initial = 0)


    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        )
    ) {
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
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                 StatCard(
                     modifier = Modifier.weight(1f),
                     label = "Total spent",
                     value = totalSpend?.let { "£${"%.2f".format(it)}" } ?: "£0.00",
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
    item {
        Button(
            onClick = onScanClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terra500,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Scan New Receipt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // ── Insight card ──
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
