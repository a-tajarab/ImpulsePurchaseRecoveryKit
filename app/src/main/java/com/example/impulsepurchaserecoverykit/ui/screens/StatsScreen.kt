package com.example.impulsepurchaserecoverykit.ui.screens

import android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.ui.Facts
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.charts.CategorySpendBarChart
import com.example.impulsepurchaserecoverykit.ui.charts.WeeklySpendLineChart
import com.example.impulsepurchaserecoverykit.ui.charts.WeeklyRegretLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun StatsScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel
) {
    // "Did you know? - show randomly on first open
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


    if (showFact) {
        AlertDialog(
            onDismissRequest = { showFact = false },
            confirmButton = {
                TextButton(onClick = { showFact = false }) { Text("Got it") }
            },
            title = { Text("Did you know?") },
            text = { Text(fact) }
        )
    }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Stats", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Receipts logged",
                    value = receiptCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total spend",
                    value = "£${String.format("%.2f", totalSpend ?: 0.0)}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatCard(
                title = "Average regret score",
                value = "${avgRegret?.let { String.format("%.1f", it) } ?: "—"} / 10",
                modifier = Modifier.fillMaxWidth()
            )
        }
        item{
            TabRow(selectedTabIndex = selectedTab){
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }

        if (selectedTab == 0){
            item {
                Text(
                    "Weekly Spend",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (weeklySpend.size < 2){
                item {
                    Text(
                        "Scan receipts over multiple days to see a weekly trend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else{
                item{
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)){
                            WeeklySpendLineChart(data = weeklySpend)
                            Spacer(Modifier.height(8.dp))
                            val fmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
                            weeklySpend.takeLast(4).forEach { w ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ){
                                    Text(
                                        fmt.format(Date(w.weekStart)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "£${String.format("%.2f", w.total)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // - Tab: Regret -
        if (selectedTab == 1){
            item{
                Text("Weekly regret trend", style = MaterialTheme.typography.titleMedium)
            }
            if (weeklyAvgRegret.size < 2) {
                item {
                    Text(
                        "Rate a few receipts over time to see your regret trend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)){
                                WeeklyRegretLineChart(data = weeklyAvgRegret)
                                Spacer(Modifier.height(8.dp))

                                val last = weeklyAvgRegret.last().avgRegret
                                val prev = weeklyAvgRegret.dropLast(1).lastOrNull()?.avgRegret

                                Text(
                                    "Latest weekly avg: ${String.format("%.1f", last)} / 10",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (prev != null){
                                    val diff = last - prev
                                    val arrow = if (diff >= 0) "↑" else "↓"
                                    val colour = if (diff >= 0)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                    Text(
                                        "$arrow ${String.format("%.1f", kotlin.math.abs(diff))} vs previous week",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colour
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    // ===== Top regret receipts =====
                    Text("Top regret purchases", style = MaterialTheme.typography.titleMedium)
                }
                if (topRegretReceipts.isEmpty()){
                    item{
                        Text(
                            "No regret score yet. Rate a few purchases to see insights.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(topRegretReceipts, key = {it.id}) { receipt ->
                        TopRegretRow(receipt)
                    }
                }
            }
            if (selectedTab == 2){
                item {
                    Text("Spend by category", style = MaterialTheme.typography.titleMedium)
                }
                if (spendByCategory.isEmpty()){
                    item {
                        Text(
                            "No category data yet. Scan a receipt first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val top = spendByCategory.take(6)
                    item{
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                CategorySpendBarChart(data = top)
                                Spacer(Modifier.height(8.dp))
                                top.forEach { row ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ){
                                        Text(
                                            row.category.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            "£${String.format("%.2f", row.total)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun TopRegretRow(receipt: ReceiptEntity) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                receipt.storeName ?: "Unknown store",
                style = MaterialTheme.typography.titleMedium
            )
            Text("Regret: ${receipt.regretScore ?: "—"} / 10")
            Text("Total: £${receipt.totalAmount ?: "—"}")
            Text("Date: ${receipt.purchaseDate ?: "—"}")
        }
    }
}