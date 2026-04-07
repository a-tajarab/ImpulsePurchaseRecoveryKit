package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*


private fun monthNumberToName(month: Int) : String = when (month){
    1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
    5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
    9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
    else -> "Unknown"
}

private val writtenMonths = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
    "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
    "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)

// Returns "YYYY-MM MonthName YYYY" for sorting, or null if unparseable
private fun parseDateToGroupKey(date: String?): String? {
    if (date.isNullOrBlank()) return null
    val trimmed = date.trim()

    // Format A: DD/MM/YYYY or DD-MM-YYYY or DD/MM/ YYYY (numeric)
    val numericRegex = Regex("""(\d{1,2})[/\-]\s*(\d{1,2})[/\-]\s*(\d{2,4})""")
    numericRegex.find(trimmed)?.let { match ->
        val month = match.groupValues[2].toIntOrNull() ?: return@let
        var year = match.groupValues[3].toIntOrNull() ?: return@let
        if (year < 100) year += 2000
        if (month !in 1..12) return@let
        return "$year-${month.toString().padStart(2, '0')} ${monthNumberToName(month)} $year"
    }
    // Format B: DD MMM YYYY  e.g. "21 Mar 2024" or "21 Apr 2024"
    val writtenRegex = Regex("""(\d{1,2})\s+([A-Za-z]{3,9})\s+(\d{2,4})""")
    writtenRegex.find(trimmed)?.let { match ->
        val monthStr = match.groupValues[2].lowercase().take(3)
        val month = writtenMonths[monthStr] ?: return@let
        var year = match.groupValues[3].toIntOrNull() ?: return@let
        if (year < 100) year += 2000
        return "$year-${month.toString().padStart(2, '0')} ${monthNumberToName(month)} $year"
    }
    // Format C: MMM DD, YYYY  e.g. "Apr 09, 2024"
    val usRegex = Regex("""([A-Za-z]{3,9})\s+(\d{1,2}),?\s+(\d{2,4})""")
    usRegex.find(trimmed)?.let { match ->
        val monthStr = match.groupValues[1].lowercase().take(3)
        val month = writtenMonths[monthStr] ?: return@let
        var year = match.groupValues[3].toIntOrNull() ?: return@let
        if (year < 100) year += 2000
        return "$year-${month.toString().padStart(2, '0')} ${monthNumberToName(month)} $year"
    }

    return null
}

@Composable
fun ReceiptListScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onReceiptClick: (Long) -> Unit
) {
    val receipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())

    //Extract all the available years from receipts
    val availableYears = remember(receipts) {
        receipts.mapNotNull { receipt ->
            parseDateToGroupKey(receipt.purchaseDate)
                ?.substringBefore("-")
                ?.toIntOrNull()
        }.distinct().sortedDescending()
    }
    var selectedYear by remember { mutableStateOf<Int?>(null) }

    // Filter receipts by selected year
    val filteredReceipts = remember(receipts, selectedYear) {
        if (selectedYear == null) receipts
        else receipts.filter { receipt ->
            parseDateToGroupKey(receipt.purchaseDate)
                ?.substringBefore("-")
                ?.toIntOrNull() == selectedYear
        }
    }

    //Group the receipts into months
    val groupedReceipts = remember(filteredReceipts) {
        filteredReceipts
            .sortedByDescending { it.createdAt }
            .groupBy { receipt ->
                parseDateToGroupKey(receipt.purchaseDate) ?: "0000-00 Unknown Date"
            }
            .toSortedMap(reverseOrder())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = 20.dp,
                    start = 20.dp,
                    end = 20.dp
                )
        ) {
            Column {
                Text(
                    text = "My Receipts",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${receipts.size} purchase${if (receipts.size != 1) "s" else ""} tracked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                val listTotal = filteredReceipts.sumOf { it.totalAmount ?: 0.0 }
                if (listTotal > 0) {
                    Text(
                        text = "Total: £${"%.2f".format(listTotal)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // ── Year filter chips ──
        if (availableYears.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All" chip
                FilterChip(
                    selected = selectedYear == null,
                    onClick = { selectedYear = null },
                    label = {
                        Text(
                            text = "All",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedYear == null,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Year chips
                availableYears.forEach { year ->
                    FilterChip(
                        selected = selectedYear == year,
                        onClick = {
                            selectedYear = if (selectedYear == year) null else year
                        },
                        label = {
                            Text(
                                text = year.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedYear == year,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )
        }

        if (filteredReceipts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = if (selectedYear != null)
                            "No receipts for $selectedYear"
                        else "No receipt yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (selectedYear != null)
                            "Try selecting a different year or tap All to see everything"
                        else "Scan your first receipt to start tracking your impulse purchases",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                groupedReceipts.forEach { (sortableKey, monthReceipts) ->
                    val displayHeader = sortableKey
                        .substringAfter(" ")
                        .let { if (it == "Unknown Date") "Unknown Date" else it }

                    item(key = "header_$sortableKey") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = displayHeader,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )
                            Text(
                                text = "${monthReceipts.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(monthReceipts, key = { it.id }) { receipt ->
                        ReceiptListCard(
                            receipt = receipt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onReceiptClick(receipt.id) }
                        )
                    }
                    item(key = "spacer_$sortableKey") {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptListCard(
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
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = receipt.storeName?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receipt.storeName ?: "Unknown store",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = buildString {
                            append(receipt.purchaseDate ?: "No date")
                            receipt.purchaseTime?.let { append("  ·  $it") }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = receipt.totalAmount
                            ?.let { "£${"%.2f".format(it)}" } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        receipt.impulseLabel?.let { ImpulseListBadge(it) }
                        receipt.regretScore?.let { RegretListBadge(it) }
                    }
                }
            }
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
                                    score >= 70 -> Terra500
                                    score >= 40 -> Warning700
                                    else -> Teal500
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ImpulseListBadge(label: String) {
    val (bg, fg) = when (label.uppercase()) {
        "HIGH"   -> Error100 to Error700
        "MEDIUM" -> Warning100 to Warning700
        else     -> Success100 to Success700
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
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
private fun RegretListBadge(score: Int) {
    val (bg, fg) = when {
        score >= 7 -> Error100 to Error700
        score >= 4 -> Warning100 to Warning700
        else       -> Success100 to Success700
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
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