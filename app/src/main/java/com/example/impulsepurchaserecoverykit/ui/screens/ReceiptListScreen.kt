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

/**
 * Converts a numeric month number to its full English name.
 *
 * @param month A month number from 1 (January) to 12 (December)
 * @return The full month name, or "Unknown" for any value outside 1–12
 */
private fun monthNumberToName(month: Int): String = when (month) {
    1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
    5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
    9 -> "September"; 10 -> "October"; 11 -> "November"; 12 -> "December"
    else -> "Unknown"
}

/**
 * Lookup map for converting three-letter month abbreviations to their
 * numeric equivalent. Used by [parseDateToGroupKey] to parse written
 * date formats such as "21 Mar 2024" and "Apr 09, 2024".
 */
private val writtenMonths = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
    "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
    "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)

/**
 * Parses a receipt date string into a sortable group key for the month-grouped list.
 *
 * The returned key has the format `YYYY-MM MonthName YYYY` — for example
 * `2026-06 June 2026`. The `YYYY-MM` prefix ensures lexicographic sorting
 * produces correct chronological order, while the human-readable suffix
 * is used as the displayed month header after stripping the prefix.
 *
 * Supports three common date formats found on UK and US receipts:
 * - **Format A**: `DD/MM/YYYY`, `DD-MM-YYYY`, or `DD/MM/ YYYY` (numeric separators)
 * - **Format B**: `DD MMM YYYY` — for example "21 Mar 2024"
 * - **Format C**: `MMM DD, YYYY` — for example "Apr 09, 2024"
 *
 * Two-digit years (e.g. "24") are treated as 2000+ and expanded to four digits.
 * Returns null if the date string is blank or does not match any supported format.
 *
 * @param date The raw date string from [ReceiptEntity.purchaseDate]
 * @return A sortable key string of the form `YYYY-MM MonthName YYYY`,
 *         or null if the date cannot be parsed
 */
private fun parseDateToGroupKey(date: String?): String? {
    if (date.isNullOrBlank()) return null
    val trimmed = date.trim()

    // Format A: DD/MM/YYYY or DD-MM-YYYY or DD/MM/ YYYY (numeric separators)
    val numericRegex = Regex("""(\d{1,2})[/\-]\s*(\d{1,2})[/\-]\s*(\d{2,4})""")
    numericRegex.find(trimmed)?.let { match ->
        val month = match.groupValues[2].toIntOrNull() ?: return@let
        var year = match.groupValues[3].toIntOrNull() ?: return@let
        if (year < 100) year += 2000
        if (month !in 1..12) return@let
        return "$year-${month.toString().padStart(2, '0')} ${monthNumberToName(month)} $year"
    }

    // Format B: DD MMM YYYY — e.g. "21 Mar 2024"
    val writtenRegex = Regex("""(\d{1,2})\s+([A-Za-z]{3,9})\s+(\d{2,4})""")
    writtenRegex.find(trimmed)?.let { match ->
        val monthStr = match.groupValues[2].lowercase().take(3)
        val month = writtenMonths[monthStr] ?: return@let
        var year = match.groupValues[3].toIntOrNull() ?: return@let
        if (year < 100) year += 2000
        return "$year-${month.toString().padStart(2, '0')} ${monthNumberToName(month)} $year"
    }

    // Format C: MMM DD, YYYY — e.g. "Apr 09, 2024"
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

/**
 * Full purchase history screen displaying all scanned receipts grouped by month.
 *
 * The screen has three main sections:
 *
 * **Header** — midnight navy bar showing the total number of receipts and the
 * combined total of all receipts currently visible (filtered or unfiltered).
 *
 * **Year filter chips** — shown when receipts span more than one year.
 * An "All" chip shows every receipt; individual year chips filter the list
 * to only receipts from that year. Tapping the selected year chip deselects it
 * and returns to the unfiltered view.
 *
 * **Grouped receipt list** — receipts are grouped by month using [parseDateToGroupKey]
 * and sorted in reverse chronological order (most recent month first). Each group
 * has a month/year header with a divider and receipt count. Receipts within each
 * group are sorted by [ReceiptEntity.createdAt] descending.
 *
 * Receipts that cannot be parsed to a valid date are grouped under "Unknown Date"
 * which sorts to the bottom of the list.
 *
 * An empty state is shown when no receipts match the current filter.
 *
 * @param paddingValues Padding applied by the parent [Scaffold]
 * @param viewModel The shared [ReceiptViewModel] providing all receipt data
 * @param onReceiptClick Callback invoked with a receipt ID when the user taps
 *                       a receipt card — navigates to the Receipt Detail screen
 */
@Composable
fun ReceiptListScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onReceiptClick: (Long) -> Unit
) {
    val receipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())

    // Extract distinct years from parsed receipt dates for the year filter chips
    val availableYears = remember(receipts) {
        receipts.mapNotNull { receipt ->
            parseDateToGroupKey(receipt.purchaseDate)
                ?.substringBefore("-")
                ?.toIntOrNull()
        }.distinct().sortedDescending()
    }

    // null = show all years; an Int value = filter to that year only
    var selectedYear by remember { mutableStateOf<Int?>(null) }

    // Filter receipts to the selected year — re-calculated only when receipts or year changes
    val filteredReceipts = remember(receipts, selectedYear) {
        if (selectedYear == null) receipts
        else receipts.filter { receipt ->
            parseDateToGroupKey(receipt.purchaseDate)
                ?.substringBefore("-")
                ?.toIntOrNull() == selectedYear
        }
    }

    // Group filtered receipts by month key and sort in reverse chronological order.
    // The "0000-00 Unknown Date" fallback sorts to the bottom via reverseOrder().
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
        // ── Midnight navy header ──────────────────────────────────────────
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
                // Total receipt count — always shows all receipts, not just filtered
                Text(
                    text = "${receipts.size} purchase${if (receipts.size != 1) "s" else ""} tracked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                // Filtered total — only shown when the visible list has a non-zero sum
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

        // ── Year filter chips — only shown when multiple years exist ──────
        if (availableYears.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All" chip — clears the year filter
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

                // One chip per year — tapping the selected year deselects it
                availableYears.forEach { year ->
                    FilterChip(
                        selected = selectedYear == year,
                        onClick = { selectedYear = if (selectedYear == year) null else year },
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        }

        // ── Empty state ───────────────────────────────────────────────────
        if (filteredReceipts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
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
                    // Message adapts based on whether a year filter is active
                    Text(
                        text = if (selectedYear != null) "No receipts for $selectedYear" else "No receipt yet",
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
            // ── Month-grouped receipt list ────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
            ) {
                groupedReceipts.forEach { (sortableKey, monthReceipts) ->
                    // Strip the YYYY-MM sort prefix to get the display label
                    val displayHeader = sortableKey
                        .substringAfter(" ")
                        .let { if (it == "Unknown Date") "Unknown Date" else it }

                    // Month header — displays "Month YYYY" with a divider and receipt count
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

                    // Receipt cards for this month
                    items(monthReceipts, key = { it.id }) { receipt ->
                        ReceiptListCard(
                            receipt = receipt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onReceiptClick(receipt.id) }
                        )
                    }

                    // Spacer between month groups
                    item(key = "spacer_$sortableKey") {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Receipt card used in the month-grouped list on the Receipt List screen.
 *
 * Displays a compact summary of a single [ReceiptEntity] including:
 * - A store initial circle in the primary container colour
 * - Store name (truncated with ellipsis on long names) and purchase date/time
 * - Total amount, impulse label badge, and regret score badge (when rated)
 * - A thin impulse score progress bar along the bottom edge of the card,
 *   colour-coded warm stone for HIGH (≥70), amber for MEDIUM (≥40), and
 *   teal for LOW
 *
 * Tapping the card triggers [onReceiptClick] via the [Modifier.clickable]
 * applied by the parent [ReceiptListScreen].
 *
 * @param receipt The [ReceiptEntity] to display
 * @param modifier Optional [Modifier] applied to the root [Card], including
 *                 the clickable modifier attached by the parent
 */
@Composable
private fun ReceiptListCard(
    receipt: ReceiptEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Store initial circle — falls back to "?" for receipts with no store name
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

                // Store name and date — time appended with a centred dot separator if present
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

                // Right column — total amount, impulse badge, regret badge
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = receipt.totalAmount?.let { "£${"%.2f".format(it)}" } ?: "—",
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

            // Impulse score progress bar — proportional fill with colour coding
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
                                    score >= 70 -> Terra500  // warm stone for high impulse
                                    score >= 40 -> Warning700
                                    else        -> Teal500
                                }
                            )
                    )
                }
            }
        }
    }
}

/**
 * Small coloured badge displaying the impulse risk label in the receipt list.
 *
 * Colour coded based on the label:
 * - HIGH — error red background and text
 * - MEDIUM — amber background and text
 * - LOW / other — green background and text
 *
 * @param label The impulse label string — "HIGH", "MEDIUM", or "LOW"
 */
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

/**
 * Small coloured badge displaying the user's regret score in the receipt list.
 *
 * Colour coded based on the score:
 * - Score ≥ 7 — error red (high regret)
 * - Score ≥ 4 — amber (medium regret)
 * - Score < 4 — green (low regret)
 *
 * @param score The regret score from 1 to 10
 */
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