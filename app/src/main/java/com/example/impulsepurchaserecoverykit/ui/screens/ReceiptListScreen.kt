package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel

@Composable
fun ReceiptListScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onReceiptClick: (Long) -> Unit
) {
    val receipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())

    //Group the receipts into months
    val groupedReceipts = remember(receipts){
        receipts
            .groupBy { receipt ->
                val date = receipt.purchaseDate?.trim() ?:""
                val parts = date.replace(" ", "").split("/")
                if (parts.size >= 3) {
                    val month = parts[1].padStart(2, '0').toIntOrNull() ?: 0
                    val year = parts[2].take(4)
                    val monthName = when (month) {
                        1 -> "January"
                        2 -> "February"
                        3 -> "March"
                        4 -> "April"
                        5 -> "May"
                        6 -> "June"
                        7 -> "July"
                        8 -> "August"
                        9 -> "September"
                        10 -> "October"
                        11 -> "November"
                        12 -> "December"
                        else -> "Unknown"
                    }
                    "$year-${month.toString().padStart(2, '0')} $monthName $year"
                } else {
                    "99-99 Unknown Date"
                }
            }
            .toSortedMap(reverseOrder())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "All Receipts",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (receipts.isEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    )
                    {
                        Text("No receipts yet")
                        Text(
                            "Scan your first receipt using the Home screen to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    "${receipts.size} receipt${if (receipts.size == 1) "" else "s"} logged",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            groupedReceipts.forEach { (sortableKey, groupedReceipts) ->
                val displayHeader = sortableKey
                    .substringAfter(" ")
                    .let { if (it == "Unknown Date") "📅 Unknown Date" else "📅 $it" }
                item(key = sortableKey) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = displayHeader,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    HorizontalDivider()
                }
                items(groupedReceipts, key = { it.id }) { r ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReceiptClick(r.id) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.Top
                            )
                            {
                                Text(
                                    r.storeName ?: "Unknown store",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                val impulseBadgeColour = when (r.impulseLabel) {
                                    "HIGH" -> MaterialTheme.colorScheme.errorContainer
                                    "MEDIUM" -> MaterialTheme.colorScheme.tertiaryContainer
                                    "LOW" -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val impulseTextColour = when (r.impulseLabel) {
                                    "HIGH" -> MaterialTheme.colorScheme.onErrorContainer
                                    "MEDIUM" -> MaterialTheme.colorScheme.onTertiaryContainer
                                    "LOW" -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                //val badgeLabel = r.impulseLabel ?: "Not rated"
                                Surface(
                                    color = impulseBadgeColour,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "⚡ ${r.impulseLabel ?: "—"}",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = impulseTextColour
                                    )
                                }
                                    val regretScore = r.regretScore?.takeIf { it > 0 }
                                        val regretBadgeColour = when {
                                            regretScore == null -> MaterialTheme.colorScheme.surfaceVariant
                                            regretScore >= 8 -> MaterialTheme.colorScheme.errorContainer
                                            regretScore >= 5 -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        }
                                        val regretTextColour = when {
                                            regretScore == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                            regretScore >= 8 -> MaterialTheme.colorScheme.onErrorContainer
                                            regretScore >= 5 -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                        Surface(
                                            color = regretBadgeColour,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = if (regretScore != null) "😬 $regretScore/10" else "😬 Not rated",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = regretTextColour
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                "📅 ${r.purchaseDate ?: "Date unknown"}${r.purchaseTime?.let { " at $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "💰 £${r.totalAmount?.let { "%.2f".format(it) } ?: "—"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
}