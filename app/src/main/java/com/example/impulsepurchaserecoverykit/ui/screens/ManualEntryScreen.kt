package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.ParsedItem
import com.example.impulsepurchaserecoverykit.ParsedReceipt
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel

@Composable
fun ManualEntryScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Manual Entry",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Fill in what you can — even partial details are useful.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = { Text("Store or website name") },
            placeholder = { Text("e.g. ASOS, Tesco, Amazon") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = totalAmount,
            onValueChange = { totalAmount = it },
            label = { Text("Total amount spent (£)") },
            placeholder = { Text("e.g. 24.99") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        HorizontalDivider()

        Text(
            "Main item purchased (optional)",
            style = MaterialTheme.typography.titleSmall
        )

        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item name") },
            placeholder = { Text("e.g. Blue hoodie, Trainers") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = itemPrice,
            onValueChange = { itemPrice = it },
            label = { Text("Item price (£)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        HorizontalDivider()

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Why did you buy this? (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                // Validate
                val total = totalAmount.toDoubleOrNull()
                if (storeName.isBlank() && total == null) {
                    errorMessage = "Please enter at least a store name or total amount."
                    return@Button
                }

                // Build a ParsedReceipt from manual input
                val items = if (itemName.isNotBlank()) {
                    listOf(
                        ParsedItem(
                            name = itemName,
                            price = itemPrice.toDoubleOrNull() ?: 0.0,
                            quantity = 1,
                            category = "other"
                        )
                    )
                } else emptyList()

                val parsedReceipt = ParsedReceipt(
                    storeName = storeName.ifBlank { null },
                    purchaseDate = null,
                    items = items,
                    subtotal = total,
                    tax = null,
                    total = total,
                    rawText = "Manual entry: $storeName $totalAmount ${note}"
                )

                viewModel.saveReceipt(parsedReceipt, null) { receiptId ->
                    onSaved(receiptId)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Purchase")
        }

        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}