package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.entities.ReceiptEntity
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onScanClick: () -> Unit,
    onReceiptClick: (Long) -> Unit
) {
    val receiptCount by viewModel.receiptCount.collectAsState()
    val avgRegret by viewModel.averageRegret.collectAsState()

    val recentReceipts by viewModel.getRecentReceipts(5).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Impulse Purchase Recovery Kit", style = MaterialTheme.typography.headlineMedium)

        ElevatedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dashboard", style = MaterialTheme.typography.titleLarge)
                Text("Receipts logged: $receiptCount")
                Text("Average regret: ${avgRegret?.let { String.format("%.1f", it) } ?: "—"} / 10")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan New Receipt") }
            }
        }

        Text("Recent Receipts", style = MaterialTheme.typography.titleMedium)

        if (recentReceipts.isEmpty()) {
            Text("No receipts yet. Scan your first one!")
        } else {
            recentReceipts.forEach { receipt ->
                ReceiptRow(receipt = receipt, onClick = { onReceiptClick(receipt.id) })
            }
        }
    }
}

@Composable
private fun ReceiptRow(receipt: ReceiptEntity, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(receipt.storeName ?: "Unknown store", style = MaterialTheme.typography.titleMedium)
            Text("Date: ${receipt.purchaseDate ?: "—"}")
            Text("Total: £${receipt.totalAmount ?: "—"}")
            Text("Regret: ${receipt.regretScore?.toString() ?: "Not rated"}")
        }
    }
}
