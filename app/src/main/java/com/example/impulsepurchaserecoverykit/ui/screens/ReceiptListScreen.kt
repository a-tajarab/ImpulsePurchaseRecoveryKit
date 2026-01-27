package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

    Column(
        Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text("All Receipts", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        if (receipts.isEmpty()) {
            Text("Nothing here yet — scan a receipt to get started.")
        } else {
            receipts.forEach { r ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onReceiptClick(r.id) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.storeName ?: "Unknown store", style = MaterialTheme.typography.titleMedium)
                        Text("Date: ${r.purchaseDate ?: "—"}")
                        Text("Total: £${r.totalAmount ?: "—"}")
                        Text("Regret: ${r.regretScore?.toString() ?: "Not rated"}")
                    }
                }
            }
        }
    }
}