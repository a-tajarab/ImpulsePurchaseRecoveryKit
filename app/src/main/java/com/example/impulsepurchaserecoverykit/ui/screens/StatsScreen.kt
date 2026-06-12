package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.database.models.CategorySpend
import com.example.impulsepurchaserecoverykit.ui.Facts
import com.example.impulsepurchaserecoverykit.ui.charts.CategorySpendBarChart
import com.example.impulsepurchaserecoverykit.ui.charts.WeeklyRegretLineChart
import com.example.impulsepurchaserecoverykit.ui.charts.WeeklySpendLineChart
import com.example.impulsepurchaserecoverykit.ui.theme.*
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full month name lookup by numeric index (1 = January, 12 = December).
 * Index 0 is an empty string placeholder so 1-based indexing works directly.
 */
private val statsMonthNames = listOf(
    "", "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

/**
 * Lookup map for converting three-letter month abbreviations to their numeric value.
 * Used by [parseReceiptDateParts] to parse written date formats such as "21 Mar 2024".
 */
private val statsShortMonths = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
    "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
    "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)

/**
 * Parses a receipt date string into a (year, month, day) Triple for filtering
 * receipts by specific calendar periods on the Stats screen.
 *
 * Supports two date formats:
 * - **Numeric**: `DD/MM/YYYY`, `DD-MM-YYYY`, or variants with spaces around separators
 * - **Written**: `DD MMM YYYY` — for example "21 Mar 2024"
 *
 * Returns null if the string is blank, null, or does not match either format.
 * Day and month values are validated to be within realistic ranges before returning.
 *
 * @param date The raw date string from [ReceiptEntity.purchaseDate]
 * @return A [Triple] of (year, month, day) as integers, or null if unparseable
 */
private fun parseReceiptDateParts(date: String?): Triple<Int, Int, Int>? {
    if (date.isNullOrBlank()) return null
    val t = date.trim()

    // Format A: DD/MM/YYYY or DD-MM-YYYY with optional spaces around separators
    Regex("""(\d{1,2})[/\-]\s*(\d{1,2})[/\-]\s*(\d{4})""").find(t)?.let {
        val d = it.groupValues[1].toIntOrNull() ?: return@let
        val m = it.groupValues[2].toIntOrNull() ?: return@let
        val y = it.groupValues[3].toIntOrNull() ?: return@let
        if (m in 1..12 && d in 1..31) return Triple(y, m, d)
    }

    // Format B: DD MMM YYYY — e.g. "21 Mar 2024"
    Regex("""(\d{1,2})\s+([A-Za-z]{3,9})\s+(\d{4})""").find(t)?.let {
        val d = it.groupValues[1].toIntOrNull() ?: return@let
        val m = statsShortMonths[it.groupValues[2].lowercase().take(3)] ?: return@let
        val y = it.groupValues[3].toIntOrNull() ?: return@let
        return Triple(y, m, d)
    }

    return null
}

/**
 * Returns the number of days in a given month, accounting for leap years.
 *
 * Used by the Stats screen to generate a complete list of days for the monthly
 * spend chart, including the correct February length for the selected year.
 *
 * @param year The four-digit year, used to determine leap year status
 * @param month A month number from 1 (January) to 12 (December)
 * @return The number of days in the specified month
 */
private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 30
}

/**
 * Analytics dashboard screen showing spending, regret, and category statistics.
 *
 * The screen opens with a 1-in-5 chance of showing a "Did you know?" fact from
 * [Facts.list] as an [AlertDialog] — a light-touch educational feature that surfaces
 * spending psychology insights without being intrusive.
 *
 * The main content is a [LazyColumn] with three sections:
 *
 * **Header** — midnight navy bar with "Your Stats" title and tagline.
 *
 * **Summary cards** — three [StatsCard] composables showing total receipts,
 * total spend, and average regret score. Regret score is colour-coded green,
 * amber, or red based on its value.
 *
 * **Tabbed content** — a [TabRow] with three tabs:
 *
 * 1. **Spending** — toggleable between Monthly and Weekly view via [FilterChip]:
 *    - Monthly: a [SpendPeriodChart] bar chart with daily breakdown and
 *      ‹ / › month navigation buttons
 *    - Weekly: a 7-bar Mon–Sun chart with week navigation. The forward
 *      button is disabled when `weekOffset == 0` (current week)
 *
 * 2. **Regret** — month-filtered receipt regret bars (one per rated receipt),
 *    plus an all-time top regret list ([TopRegretCard]) and a weekly regret
 *    trend line chart ([WeeklyRegretLineChart]) with trend direction indicator
 *
 * 3. **Categories** — month-filtered [CategorySpendBarChart] showing the top 6
 *    spending categories by item total for the selected month
 *
 * The selected month and year are shared state across all three tabs so navigating
 * to a different month on the Regret tab also affects the Categories tab.
 *
 * @param paddingValues Padding applied by the parent [Scaffold]
 * @param viewModel The shared [ReceiptViewModel] providing all spending and regret data
 */
@Composable
fun StatsScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel
) {
    // Show a random spending psychology fact on 1 in 5 app opens
    var showFact by remember { mutableStateOf((0..4).random() == 0) }
    val fact = remember { Facts.list.random() }

    val receiptCount by viewModel.receiptCount.collectAsState()
    val avgRegret by viewModel.averageRegret.collectAsState(initial = null)
    val totalSpend by viewModel.getTotalSpend().collectAsState(initial = 0.0)
    val topRegretReceipts by viewModel.getTopRegretReceipts(3).collectAsState(initial = emptyList())
    val spendByCategory by viewModel.getSpendByCategory().collectAsState(initial = emptyList())
    val weeklySpend by viewModel.getWeeklySpend().collectAsState(initial = emptyList())
    val weeklyAvgRegret by viewModel.getWeeklyAverageRegret().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Spending", "Regret", "Categories")

    // Weekly view filter — not currently wired to the chart; reserved for future use
    var selectedWeeks by remember { mutableStateOf(4) }
    var selectedWeekFilter by remember { mutableStateOf("4 weeks") }
    val filteredWeeklySpend = remember(weeklySpend, selectedWeekFilter) {
        when (selectedWeekFilter) {
            "4 weeks" -> weeklySpend.takeLast(4)
            "8 weeks" -> weeklySpend.takeLast(8)
            else      -> weeklySpend
        }
    }

    val allReceipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())

    // Toggle between Monthly and Weekly bar chart views on the Spending tab
    var spendViewMode by remember { mutableStateOf("Monthly") }

    val nowCal = remember { java.util.Calendar.getInstance() }

    // selectedYear and selectedMonth are shared across all three tabs via shared state
    var selectedYear by remember { mutableStateOf(nowCal.get(java.util.Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(nowCal.get(java.util.Calendar.MONTH) + 1) }

    // weekOffset: 0 = current week, -1 = last week, -2 = two weeks ago, etc.
    var weekOffset by remember { mutableStateOf(0) }

    // "Did you know?" fact dialog — dismissed via "Got it" button
    if (showFact) {
        AlertDialog(
            onDismissRequest = { showFact = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Did you know?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = { Text(fact, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(onClick = { showFact = false }, colors = ButtonDefaults.buttonColors(containerColor = Teal700, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 16.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(top = paddingValues.calculateTopPadding() + 16.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your Stats", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                    Text("Insights from your spending patterns", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                }
            }
        }

        // ── Summary cards — Receipts, Total spent, Avg regret ────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatsCard(modifier = Modifier.weight(1f), label = "Receipts", value = receiptCount.toString(), valueColor = MaterialTheme.colorScheme.primary)
                StatsCard(modifier = Modifier.weight(1f), label = "Total spent", value = "£${String.format("%.2f", totalSpend ?: 0.0)}", valueColor = MaterialTheme.colorScheme.primary)
                StatsCard(
                    modifier = Modifier.weight(1f),
                    label = "Avg regret",
                    value = "${avgRegret?.let { String.format("%.1f", it) } ?: "-"}/10",
                    valueColor = when {
                        avgRegret == null  -> MaterialTheme.colorScheme.onSurfaceVariant
                        avgRegret!! >= 7.0 -> Error700
                        avgRegret!! >= 4.0 -> Warning700
                        else               -> Success700
                    }
                )
            }
        }

        // ── Tab row — Spending / Regret / Categories ──────────────────────
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Teal700
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.labelLarge)
                        },
                        selectedContentColor = Teal700,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        // ══ TAB 0: SPENDING ═══════════════════════════════════════════════
        if (selectedTab == 0) {

            // Weekly / Monthly toggle chips
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Weekly", "Monthly").forEach { mode ->
                        FilterChip(
                            selected = spendViewMode == mode,
                            onClick = { spendViewMode = mode },
                            label = { Text(mode, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Teal700, selectedLabelColor = Color.White, containerColor = MaterialTheme.colorScheme.surfaceVariant, labelColor = MaterialTheme.colorScheme.onSurfaceVariant),
                            border = FilterChipDefaults.filterChipBorder(enabled = true, selected = spendViewMode == mode, borderColor = MaterialTheme.colorScheme.outlineVariant, selectedBorderColor = Teal700),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            if (spendViewMode == "Monthly") {
                // ── Monthly bar chart with ‹ / › month navigation ────────
                item {
                    val monthLabel = "${statsMonthNames[selectedMonth]} $selectedYear"
                    val days = daysInMonth(selectedYear, selectedMonth)

                    // Build a list of (day label, total spend) for every day in the month
                    val monthData = (1..days).mapNotNull { day ->
                        val total = allReceipts.filter { r ->
                            val c = parseReceiptDateParts(r.purchaseDate)
                            c != null && c.first == selectedYear && c.second == selectedMonth && c.third == day
                        }.sumOf { it.totalAmount ?: 0.0 }
                        "$day" to total
                    }

                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Month navigator row — wraps at year boundaries
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else selectedMonth-- }) {
                                    Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                IconButton(onClick = { if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++ }) {
                                    Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (monthData.none { it.second > 0 }) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    Text("No spending recorded for ${statsMonthNames[selectedMonth]}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            } else {
                                SpendPeriodChart(data = monthData)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(4.dp))
                                Text("Total: £${String.format("%.2f", monthData.sumOf { it.second })}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                // Daily breakdown — only days with spend > 0
                                monthData.filter { it.second > 0 }.forEach { (day, total) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${statsMonthNames[selectedMonth].take(3)} $day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("£${String.format("%.2f", total)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // ── Weekly bar chart with ‹ / › week navigation ──────────
                // weekOffset 0 = current week, negative = past weeks
                item {
                    val weekCal = remember(weekOffset) {
                        java.util.Calendar.getInstance().apply {
                            firstDayOfWeek = java.util.Calendar.MONDAY
                            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                            add(java.util.Calendar.WEEK_OF_YEAR, weekOffset)
                        }
                    }

                    // Build a (year, month, day) triple for each day of the displayed week
                    val weekDays = (0..6).map { offset ->
                        val dayCal = weekCal.clone() as java.util.Calendar
                        dayCal.add(java.util.Calendar.DAY_OF_YEAR, offset)
                        Triple(dayCal.get(java.util.Calendar.YEAR), dayCal.get(java.util.Calendar.MONTH) + 1, dayCal.get(java.util.Calendar.DAY_OF_MONTH))
                    }

                    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

                    val weekData = weekDays.mapIndexed { i, (y, m, d) ->
                        val total = allReceipts.filter { r ->
                            val c = parseReceiptDateParts(r.purchaseDate)
                            c != null && c.first == y && c.second == m && c.third == d
                        }.sumOf { it.totalAmount ?: 0.0 }
                        "${dayLabels[i]}\n$d" to total
                    }

                    val startDay = weekDays.first()
                    val endDay = weekDays.last()
                    val weekLabel = "${startDay.third} ${statsMonthNames[startDay.second].take(3)} – ${endDay.third} ${statsMonthNames[endDay.second].take(3)} ${endDay.first}"

                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { weekOffset-- }) {
                                    Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Text(weekLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
                                // Forward button disabled at current week — future weeks have no data
                                IconButton(onClick = { if (weekOffset < 0) weekOffset++ }, enabled = weekOffset < 0) {
                                    Text("›", fontSize = 24.sp, color = if (weekOffset < 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                            }
                            SpendPeriodChart(data = weekData)
                            val weekTotal = weekData.sumOf { it.second }
                            if (weekTotal > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Week total: £${String.format("%.2f", weekTotal)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                weekData.filter { it.second > 0 }.forEach { (label, total) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(label.replace("\n", " "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("£${String.format("%.2f", total)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text("No spending recorded this week", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ══ TAB 1: REGRET ═════════════════════════════════════════════════
        if (selectedTab == 1) {

            // Month navigator — shared selectedMonth/selectedYear with other tabs
            item(key = "regret_month_nav") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else selectedMonth-- }) {
                        Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text("${statsMonthNames[selectedMonth]} $selectedYear", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    IconButton(onClick = { if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++ }) {
                        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Per-receipt regret bars for the selected month — one bar per rated receipt
            item(key = "regret_bars_${selectedYear}_${selectedMonth}") {
                val monthReceipts = allReceipts.filter { r ->
                    val c = parseReceiptDateParts(r.purchaseDate)
                    c != null && c.first == selectedYear && c.second == selectedMonth && r.regretScore != null
                }.sortedByDescending { it.regretScore }

                if (monthReceipts.isEmpty()) {
                    EmptyStateCard(message = "No rated receipts for ${statsMonthNames[selectedMonth]} $selectedYear.\nRate some purchases to see your regret breakdown.", modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                } else {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val avgMonthRegret = monthReceipts.mapNotNull { it.regretScore }.average()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${monthReceipts.size} rated purchase${if (monthReceipts.size != 1) "s" else ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Avg ${String.format("%.1f", avgMonthRegret)}/10", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = when { avgMonthRegret >= 7 -> Error700; avgMonthRegret >= 4 -> Warning700; else -> Success700 })
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // One horizontal bar per rated receipt, width proportional to score
                            monthReceipts.forEach { receipt ->
                                val score = receipt.regretScore ?: 0
                                val fraction = (score / 10f).coerceIn(0.03f, 1f)
                                val barColor = when { score >= 8 -> Error700; score >= 5 -> Warning700; else -> Success700 }
                                val storeName = receipt.storeName ?: "Unknown"

                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.weight(1f).height(34.dp)) {
                                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(barColor), contentAlignment = Alignment.CenterStart) {
                                                // Store name inside bar when bar is wide enough
                                                if (fraction > 0.3f) {
                                                    Text(text = storeName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp))
                                                }
                                            }
                                            if (fraction <= 0.3f) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(text = storeName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                    // Score badge to the right of the bar
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(barColor).padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                                        Text("$score/10", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // All-time top regret list
            item(key = "regret_top_header") {
                Spacer(Modifier.height(8.dp))
                SectionHeader(title = "All-time top regret", modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (topRegretReceipts.isEmpty()) {
                item(key = "regret_empty") {
                    EmptyStateCard(message = "No regret scores yet. Rate a few purchases to see insights.", modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }
            } else {
                items(topRegretReceipts, key = { "top_regret_${it.id}" }) { receipt ->
                    TopRegretCard(receipt = receipt, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            // Weekly regret trend line chart — requires at least 2 data points
            item(key = "regret_trend_header") {
                SectionHeader(title = "Weekly regret trend", modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (weeklyAvgRegret.size < 2) {
                item(key = "regret_trend_empty") {
                    EmptyStateCard(message = "Rate a few receipts over time to see your regret trend", modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                }
            } else {
                item(key = "regret_trend_chart") {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            WeeklyRegretLineChart(data = weeklyAvgRegret)
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(8.dp))

                            val last = weeklyAvgRegret.last().avgRegret
                            val prev = weeklyAvgRegret.dropLast(1).lastOrNull()?.avgRegret

                            // Latest weekly average with colour coding
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Latest weekly avg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format("%.1f", last)} /10", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = when { last >= 7 -> Error700; last >= 4 -> Warning700; else -> Success700 })
                            }

                            // Week-on-week trend arrow — ↑ is bad (higher regret), ↓ is good
                            if (prev != null) {
                                val diff = last - prev
                                val arrow = if (diff >= 0) "↑" else "↓"
                                val colour = if (diff >= 0) Error700 else Success700
                                Text("$arrow ${String.format("%.1f", kotlin.math.abs(diff))} vs previous week", style = MaterialTheme.typography.bodySmall, color = colour, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ══ TAB 2: CATEGORIES ═════════════════════════════════════════════
        if (selectedTab == 2) {
            item {
                SectionHeader(title = "Spend by category", modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Month navigator — shared with other tabs
            item(key = "spend_monthly_${selectedYear}_${selectedMonth}") {
                val catMonthLabel = "${statsMonthNames[selectedMonth]} $selectedYear"
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else selectedMonth-- }) {
                        Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(catMonthLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    IconButton(onClick = { if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++ }) {
                        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Category bar chart — built from item-level data for the selected month
            item(key = "cat_header") {
                val filteredByMonth = allReceipts.filter { r ->
                    val c = parseReceiptDateParts(r.purchaseDate)
                    c != null && c.first == selectedYear && c.second == selectedMonth
                }

                val itemsForMonth by viewModel.getItemsForMonth(selectedYear, selectedMonth)
                    .collectAsState(initial = emptyList())

                // Aggregate item spend by category — top 6 categories shown
                val monthCategories = itemsForMonth
                    .groupBy { it.category }
                    .map { (cat, items) -> CategorySpend(category = cat, total = items.sumOf { it.price * it.quantity }) }
                    .sortedByDescending { it.total }
                    .take(6)

                if (monthCategories.isEmpty()) {
                    EmptyStateCard(message = "No spending recorded for ${statsMonthNames[selectedMonth]} $selectedYear", modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                } else {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            CategorySpendBarChart(data = monthCategories)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small summary card used in the three-card row at the top of the Stats screen.
 *
 * Displays a single statistic with a muted label above and a bold value below.
 * [valueColor] allows each card to colour-code its value independently — for
 * example, the average regret card shifts from green to amber to red as the
 * score rises.
 *
 * @param modifier Optional [Modifier] applied to the root [Card]
 * @param label The small muted label shown above the value
 * @param value The formatted value string displayed prominently
 * @param valueColor The colour applied to the value text
 */
@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

/**
 * Bold section title used to label content groups on the Stats screen.
 *
 * @param title The section title text
 * @param modifier Optional [Modifier] — typically includes horizontal padding
 *                 and a vertical padding via [Modifier.padding]
 */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = modifier.padding(vertical = 8.dp))
}

/**
 * Centred empty state card shown when a Stats section has no data to display.
 *
 * Used throughout the Stats screen when: no receipts have been rated for the
 * selected month, no regret scores exist yet, not enough weeks of data exist for
 * the trend chart, or no items have been logged for a category month.
 *
 * @param message The explanatory message to display — may contain newlines for
 *                multi-line text
 * @param modifier Optional [Modifier] applied to the root [Card], typically
 *                 including horizontal padding from the parent list
 */
@Composable
private fun EmptyStateCard(message: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/**
 * Card displaying a single receipt from the all-time top regret list.
 *
 * Shows the store name, date, and total amount on the left and the regret
 * score badge on the right. The card background colour adapts to the score:
 * - Error red background for scores 8 and above
 * - Amber background for scores 5–7
 * - Plain surface for scores below 5
 *
 * @param receipt The [ReceiptEntity] to display
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
@Composable
private fun TopRegretCard(receipt: ReceiptEntity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                (receipt.regretScore ?: 0) >= 8 -> Error100
                (receipt.regretScore ?: 0) >= 5 -> Warning100
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(receipt.storeName ?: "Unknown store", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(receipt.purchaseDate ?: "—", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("£${receipt.totalAmount?.let { String.format("%.2f", it) } ?: "—"}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(when { (receipt.regretScore ?: 0) >= 8 -> Error700; (receipt.regretScore ?: 0) >= 5 -> Warning700; else -> Success700 }).padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("${receipt.regretScore ?: "—"}/10", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}

/**
 * Custom vertical bar chart for displaying spending per time period.
 *
 * Used on the Spending tab for both monthly (one bar per day) and weekly
 * (one bar per day of the week) views. Each bar is a [Box] sized proportionally
 * to the period's spend relative to a rounded y-axis maximum.
 *
 * The y-axis maximum is rounded up to the nearest clean value (50, 100, 250,
 * 500, 1000, 2000, or nearest 1000 above) to give the chart breathing room
 * and avoid the highest bar always touching the chart ceiling.
 *
 * The highest-spend bar is coloured [Terra500] (warm stone) to make it stand
 * out visually; all other bars use [Teal500]. Bars for zero-spend periods are
 * omitted entirely rather than rendering a zero-height bar.
 *
 * X-axis labels are rotated –70° to prevent overlap on monthly views with
 * up to 31 bars. Only the first three characters of each label are shown.
 *
 * @param data A list of (label, value) pairs — one entry per bar. The label
 *             may contain a newline character (used by weekly view to show
 *             "Mon\n5" for Monday the 5th)
 * @param modifier Optional [Modifier] applied to the root [Row]
 */
@Composable
private fun SpendPeriodChart(data: List<Pair<String, Double>>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val maxValue = data.maxOf { it.second }.takeIf { it > 0 } ?: 1.0

    // Round y-axis max up to a clean value for better visual readability
    val yMax = when {
        maxValue <= 50   -> 50.0
        maxValue <= 100  -> 100.0
        maxValue <= 250  -> 250.0
        maxValue <= 500  -> 500.0
        maxValue <= 1000 -> 1000.0
        maxValue <= 2000 -> 2000.0
        else             -> (Math.ceil(maxValue / 1000) * 1000)
    }
    val ySteps = listOf(yMax, yMax * 0.75, yMax * 0.5, yMax * 0.25, 0.0)

    Row(modifier = modifier.fillMaxWidth()) {
        // Y-axis labels column — five evenly spaced values from yMax to 0
        Column(modifier = Modifier.width(44.dp).height(140.dp), verticalArrangement = Arrangement.SpaceBetween) {
            ySteps.forEach { value ->
                Text(
                    text = if (value >= 1000) "£${(value / 1000).toInt()}k" else "£${value.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        Column(modifier = modifier.fillMaxWidth(1f)) {
            Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                data.forEach { (_, value) ->
                    val fraction = (value / yMax).toFloat().coerceIn(0f, 1f)
                    // Highlight the highest-spend bar in warm stone; others in teal
                    val isHighest = value > 0 && value == data.maxOf { it.second }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        if (value > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (isHighest) Terra500 else Teal500)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // X-axis labels — rotated –70° to prevent overlap on monthly charts with 31 bars
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                data.forEachIndexed { _, (label, _) ->
                    Box(modifier = Modifier.weight(1f).height(28.dp), contentAlignment = Alignment.TopCenter) {
                        Text(
                            text = label.lines().first().take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            softWrap = false,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                            modifier = Modifier.graphicsLayer { rotationZ = -70f }
                        )
                    }
                }
            }
        }
    }
}