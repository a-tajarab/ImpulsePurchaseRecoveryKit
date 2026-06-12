package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.ParsedItem
import com.example.impulsepurchaserecoverykit.ParsedReceipt
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel

/**
 * Manual receipt entry screen for logging a purchase without scanning a physical receipt.
 *
 * Shown when the user selects manual entry from the OCR failure screen, or
 * navigates to it directly when they do not have a physical receipt to scan
 * — for example, for online purchases or digital receipts.
 *
 * The screen collects the minimum information needed to create a meaningful
 * receipt record: store name, total amount, time of purchase, an optional
 * single item, and a free-text reflection note. The app is intentionally
 * forgiving — partial submissions are accepted as long as at least a store
 * name or a total amount is provided.
 *
 * On save, the form data is assembled into a [ParsedReceipt] and passed to
 * [ReceiptViewModel.saveReceipt], which writes it to the Room database and
 * returns the new receipt ID via the [onSaved] callback. Navigation to the
 * receipt detail screen is then handled by the caller.
 *
 * @param paddingValues Padding applied by the parent [Scaffold] to avoid
 *                      overlap with system bars and the bottom navigation bar
 * @param viewModel The shared [ReceiptViewModel] used to save the receipt
 *                  to the Room database
 * @param onSaved Callback invoked with the new receipt ID once the receipt
 *                has been successfully saved — used to navigate to the
 *                receipt detail screen
 * @param onBack Callback invoked when the user taps Cancel — navigates back
 *               to the previous screen without saving
 */
@Composable
fun ManualEntryScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    // Form field state — all optional except at least one of storeName or totalAmount
    var storeName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var itemPrice by remember { mutableStateOf("") }
    var purchaseTime by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Validation error message — shown below the form fields when submission fails
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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Fill in what you can — even partial details are useful.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Store name field — used as the receipt title throughout the app
        OutlinedTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = { Text("Store or website name") },
            placeholder = { Text("e.g. ASOS, Tesco, Amazon") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Total amount field — stored as the receipt total and used in all spend calculations
        OutlinedTextField(
            value = totalAmount,
            onValueChange = { totalAmount = it },
            label = { Text("Total amount spent (£)") },
            placeholder = { Text("e.g. 24.99") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        // Time of purchase field — used by the impulse scorer to factor in time of day
        OutlinedTextField(
            value = purchaseTime,
            onValueChange = { input ->
                // Only allow digits and colons, capped at HH:MM format (5 characters)
                if (input.length <= 5 && input.all { c -> c.isDigit() || c == ':' })
                    purchaseTime = input
            },
            label = { Text("Time of purchase (optional)") },
            placeholder = { Text("e.g. 14:30") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            supportingText = { Text("24-hour format — e.g. 09:00 or 21:45") }
        )

        HorizontalDivider()

        Text("Main item purchased (optional)", style = MaterialTheme.typography.titleSmall)

        // Item name — if provided, creates a single ParsedItem linked to this receipt
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Item name") },
            placeholder = { Text("e.g. Blue hoodie, Trainers") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Item price — defaults to 0.0 if left blank or unparseable
        OutlinedTextField(
            value = itemPrice,
            onValueChange = { itemPrice = it },
            label = { Text("Item price (£)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        HorizontalDivider()

        // Optional reflection note — stored in the receipt's rawText field
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Why did you buy this? (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        // Validation error message — only shown when the form fails the minimum check
        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            onClick = {
                // Validate — require at least a store name or a valid total amount
                val total = totalAmount.toDoubleOrNull()
                if (storeName.isBlank() && total == null) {
                    errorMessage = "Please enter at least a store name or total amount."
                    return@Button
                }

                // Build a single-item list if the user provided an item name
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

                // Assemble a ParsedReceipt from the form fields and save to Room DB
                val parsedReceipt = ParsedReceipt(
                    storeName = storeName.ifBlank { null },
                    purchaseDate = null,
                    purchaseTime = purchaseTime.ifBlank { null },
                    items = items,
                    subtotal = total,
                    tax = null,
                    total = total,
                    rawText = "Manual entry: $storeName $totalAmount $note"
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

        Spacer(Modifier.height(16.dp))
    }
}