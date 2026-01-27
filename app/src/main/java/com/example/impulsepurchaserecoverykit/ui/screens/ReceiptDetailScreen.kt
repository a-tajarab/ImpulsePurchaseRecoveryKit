package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel

@Composable
fun ReceiptDetailScreen(
    paddingValues: PaddingValues,
    receiptId: Long,
    viewModel: ReceiptViewModel,
    onSetRegret: () -> Unit,
    onBack: () -> Unit
) {
    val receipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())
    val receipt = receipts.firstOrNull { it.id == receiptId }

    val items by viewModel.getItemsForReceipt(receiptId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ){
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onSetRegret) { Text("Rate Regret") }
        }

        if (receipt == null) {
            Text("Receipt not found.")
            return@Column
        }

        Text(receipt.storeName ?: "Unknown store", style = MaterialTheme.typography.headlineSmall)
        Text("Date: ${receipt.purchaseDate ?: "—"}")
        Text("Subtotal: £${receipt.subtotal ?: "—"}")
        Text("Tax: £${receipt.tax ?: "—"}")
        Text("Total: £${receipt.totalAmount ?: "—"}")
        Text("Regret: ${receipt.regretScore?.toString() ?: "Not rated"}")

        receipt.emotionalNote?.let{
            Divider()
            Text("Note:", style = MaterialTheme.typography.titleMedium)
            Text(it)
        }

        Divider()
        Text("Items", style = MaterialTheme.typography.titleMedium)

        if (items.isEmpty()) {
            Text("No items parsed for this receipt.")
        } else {
            items.forEach { item ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("Category: ${item.category}")
                        Text("Price: £${item.price}  x${item.quantity}")
                    }
                }
            }
        }
    }
}