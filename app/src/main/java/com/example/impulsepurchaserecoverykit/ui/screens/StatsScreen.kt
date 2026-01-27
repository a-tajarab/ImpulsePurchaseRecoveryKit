package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
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
    var showFact by remember { mutableStateOf((0..4).random() == 0) }
    val fact = remember { Facts.list.random() }

    val receiptCount by viewModel.receiptCount.collectAsState()
    val avgRegret by viewModel.averageRegret.collectAsState()

    val totalSpend by viewModel.getTotalSpend().collectAsState(initial = 0.0)
    val topRegretReceipts by viewModel.getTopRegretReceipts(3).collectAsState(initial = emptyList())

    val spendByCategory by viewModel.getSpendByCategory().collectAsState(initial = emptyList())
    val weeklySpend by viewModel.getWeeklySpend().collectAsState(initial = emptyList())

    val weeklyAvgRegret by viewModel.getWeeklyAverageRegret().collectAsState(initial = emptyList())


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

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Stats", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Receipts",
                value = receiptCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total spend",
                value = "£${String.format("%.2f", totalSpend ?: 0.0)}",
                modifier = Modifier.weight(1f)
            )
        }

        StatCard(
            title = "Average regret",
            value = "${avgRegret?.let { String.format("%.1f", it) } ?: "—"} / 10",
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // ===== Top regret receipts =====
        Text("Top regret purchases", style = MaterialTheme.typography.titleMedium)

        if (topRegretReceipts.isEmpty()) {
            Text("No regret scores yet. Rate a few purchases to see insights.")
        } else {
            topRegretReceipts.forEach { receipt ->
                TopRegretRow(receipt)
            }
        }

        HorizontalDivider()

        Text("Impulse Tracker (weekly spend)", style = MaterialTheme.typography.titleMedium)

        if (weeklySpend.size < 2) {
            Text("Scan receipts over time to see a weekly trend.")
        } else {
            WeeklySpendLineChart(data = weeklySpend)

            // Optional: show readable week labels under the chart
            val fmt = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
            Spacer(Modifier.height(8.dp))
            weeklySpend.takeLast(4).forEach { w ->
                Text("${fmt.format(Date(w.weekStart))}: £${String.format("%.2f", w.total)}")
            }
        }

        Text("Weekly regret trend", style = MaterialTheme.typography.titleMedium)

        if (weeklyAvgRegret.size < 2) {
            Text("Rate a few receipts over time to see your regret trend.")
        } else {
            WeeklyRegretLineChart(data = weeklyAvgRegret)

            val last = weeklyAvgRegret.last().avgRegret
            val prev = weeklyAvgRegret.dropLast(1).lastOrNull()?.avgRegret

            Spacer(Modifier.height(8.dp))
            Text("Latest weekly avg regret: ${String.format("%.1f", last)} / 10")

            if (prev != null) {
                val diff = last - prev
                val direction = if (diff >= 0) "up" else "down"
                Text("Compared to previous week: ${direction} ${String.format("%.1f", kotlin.math.abs(diff))}")
            }
        }

        HorizontalDivider()

        // ===== Spend by category (text for now) =====
        Text("Spend by category", style = MaterialTheme.typography.titleMedium)

        if (spendByCategory.isEmpty()) {
            Text("No category data yet. Scan a receipt first.")
        } else {
            val top = spendByCategory.take(5)

            CategorySpendBarChart(data = top)

            // Optional: keep the list underneath for exact numbers
            Spacer(Modifier.height(8.dp))
            top.forEach { row ->
                Text("${row.category}: £${String.format("%.2f", row.total)}")
            }
        }
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
            Text(receipt.storeName ?: "Unknown store", style = MaterialTheme.typography.titleMedium)
            Text("Regret: ${receipt.regretScore ?: "—"} / 10")
            Text("Total: £${receipt.totalAmount ?: "—"}")
            Text("Date: ${receipt.purchaseDate ?: "—"}")
        }
    }
}