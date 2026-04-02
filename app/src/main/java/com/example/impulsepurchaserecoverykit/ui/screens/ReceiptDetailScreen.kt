package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import org.json.JSONArray


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    paddingValues: PaddingValues,
    receiptId: Long,
    viewModel: ReceiptViewModel,
    onSetRegret: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    //val receipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())
    //val receipt = receipts.firstOrNull { it.id == receiptId }
    val receipt by viewModel.getReceiptByIdFlow(receiptId).collectAsState(initial = null)
    val items by viewModel.getItemsForReceipt(receiptId).collectAsState(initial = emptyList())

    // Bottom sheet state
    var showAnalysisSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember {mutableStateOf(false)}
    var isEditing by remember { mutableStateOf(false) }

    // Edit state for receipt fields
    var editStoreName by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editTotal by remember { mutableStateOf("") }

    // Edit state for items — map of itemId to mutable draft
    data class ItemDraft(
        val id: Long,
        var name: String,
        var price: String,
        var quantity: String
    )
    val itemDrafts = remember { mutableStateListOf<ItemDraft>() }

    if (receipt == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ){
            CircularProgressIndicator()
        }
        return
    }
    val r = receipt!!

    // When entering edit mode, pre-fill all draft fields
    LaunchedEffect(isEditing) {
        if (isEditing) {
            editStoreName = r.storeName ?: ""
            editDate = r.purchaseDate ?: ""
            editTime = r.purchaseTime ?: ""
            editTotal = r.totalAmount?.toString() ?: ""
            itemDrafts.clear()
            itemDrafts.addAll(items.map { item ->
                ItemDraft(
                    id = item.id,
                    name = item.name,
                    price = item.price.toString(),
                    quantity = item.quantity.toString()
                )
            })
        }
    }

    if (showDeleteDialog){
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete receipt?") },
            text = { Text("This will permanently delete this receipt and all its data. This cannot be undone.")},
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReceipt(r)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    if (showAnalysisSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnalysisSheet = false }
        ) {
            AnalysisSheetContent(
                receiptId = receiptId,
                viewModel = viewModel,
                receiptImpulseLabel = r.impulseLabel,
                receiptImpulseScore = r.impulseScore,
                impulseReasonsJson = r.impulseReasonsJson,
                items = items,
                onClose = { showAnalysisSheet = false }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item{
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = {
                if (isEditing) isEditing = false else onBack()
            }) {
                Text(if (isEditing) "Cancel" else "Back")
            }

            if (!isEditing) {
                Button(
                    onClick = onSetRegret,
                    modifier = Modifier.weight(1f)
                ) { Text("Rate Regret") }

                OutlinedButton(onClick = { isEditing = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            } else {
                Button(
                    onClick = {
                        viewModel.updateReceiptDetails(
                            receiptId = receiptId,
                            storeName = editStoreName.ifBlank { null },
                            purchaseDate = editDate.ifBlank { null },
                            purchaseTime = editTime.ifBlank { null },
                            totalAmount = editTotal.toDoubleOrNull()
                        )
                        // Save item edits
                        itemDrafts.forEach { draft ->
                            if (draft.id > 0) {
                                viewModel.updateItem(
                                    itemId = draft.id,
                                    name = draft.name,
                                    price = draft.price.toDoubleOrNull() ?: 0.0,
                                    quantity = draft.quantity.toIntOrNull() ?: 1
                                )
                            } else {
                                if (draft.name.isNotBlank()) {
                                    viewModel.addItemToReceipt(
                                        receiptId = receiptId,
                                        name = draft.name,
                                        price = draft.price.toDoubleOrNull() ?: 0.0,
                                        quantity = draft.quantity.toIntOrNull() ?: 1
                                    )
                                }
                            }
                        }
                        isEditing = false
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save changes", fontWeight = FontWeight.Bold) }
            }
        }
        }
        item {
            if (!isEditing) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            r.storeName ?: "Unknown store",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        // ===== Facts Section (no judgement) =====
                        Text("Date: ${r.purchaseDate ?: "—"}")
                        Text("Time: ${r.purchaseTime ?: "Not recorded"}")
                        Text("Subtotal: £${r.subtotal?.let { "%.2f".format(it) } ?: "—"}")
                        Text("Tax: £${r.tax?.let {"%.2f".format(it) } ?: "—"}")
                        r.shipping?.let { shipping ->
                            Text("🚚 Shipping: £${"%.2f".format(shipping)}")
                        }
                        Text("Total: £${r.totalAmount?.let {"%.2f".format(it) } ?: "—"}")
                        Text("Regret: ${r.regretScore?.toString() ?: "Not rated"}")

                        val sentimentText = if (r.userSentimentLabel != null)
                            "Item sentiment: ${r.userSentimentLabel} (${r.userSentimentScore}/100)"
                        else "Item sentiment: Rate items in Analysis to see this"
                        Text(
                            sentimentText, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // EDIT MODE — receipt fields
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Editing receipt details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = editStoreName,
                            onValueChange = { editStoreName = it },
                            label = { Text("Store name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editDate,
                            onValueChange = { editDate = it },
                            label = { Text("Purchase date") },
                            placeholder = { Text("e.g. 28/11/2025") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editTime,
                            onValueChange = { input ->
                                if (input.length <= 5 && input.all { c -> c.isDigit() || c == ':' })
                                    editTime = input
                            },
                            label = { Text("Purchase time") },
                            placeholder = { Text("e.g. 14:30") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = editTotal,
                            onValueChange = { editTotal = it },
                            label = { Text("Total (£)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }
        }
                item {
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (items.isEmpty()) {
                    item { Text ("No items parsed for this receipt.")}
                } else {
                    if (!isEditing) {
                        items(items, key = { it.id }) { item ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Category: ${item.category}")
                                    Text("Price: £${"%.2f".format(item.price)}  x ${item.quantity}")
                                }
                            }
                        }
                    } else {
                        // EDIT MODE items
                        items(itemDrafts, key = { it.id }) { draft ->
                            ElevatedCard(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = draft.name,
                                        onValueChange = { draft.name = it },
                                        label = { Text("Item name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = draft.price,
                                            onValueChange = { draft.price = it },
                                            label = { Text("Price (£)") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal
                                            ),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = draft.quantity,
                                            onValueChange = { draft.quantity = it },
                                            label = { Text("Qty") },
                                            modifier = Modifier.width(80.dp),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                        // Add this right after the items(itemDrafts...) block, still inside the else (isEditing) branch:
                        item {
                            OutlinedButton(
                                onClick = {
                                    itemDrafts.add(
                                        ItemDraft(
                                            id = -System.currentTimeMillis(), // temp negative ID for new items
                                            name = "",
                                            price = "",
                                            quantity = "1"
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Add item")
                            }
                        }
                    }
                }
                if (!isEditing) {
                    item {
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Analysis of this purchase",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Open when your're ready - includes impulse signal and items reactions.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showAnalysisSheet = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                        {  Text("Open analysis") }

                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnalysisSheetContent(
    receiptId: Long,
    viewModel: ReceiptViewModel,
    receiptImpulseLabel: String?,
    receiptImpulseScore: Int?,
    impulseReasonsJson: String?,
    items: List<com.example.impulsepurchaserecoverykit.database.entities.ItemEntity>,
    onClose: () -> Unit
) {
    val reasons = remember(impulseReasonsJson) {
        parseReasons(impulseReasonsJson)
    }

    val reactions by viewModel.getItemReactionsForReceipt(receiptId)
        .collectAsState(initial = emptyList())
    val savedMap = remember(reactions){
        reactions.associate { it.itemId to it.reaction }
    }

    val draftMap = remember(receiptId) {
        mutableStateMapOf<Long, Int>()
    }

    LaunchedEffect(savedMap) {
        draftMap.clear()
        draftMap.putAll(savedMap)
    }

    val hasUnsavedChanges by remember(savedMap){
        derivedStateOf { savedMap != draftMap.toMap() }
    }

    val listState = rememberLazyListState()

    val reactionMap = remember(reactions) {
        reactions.associateBy { it.itemId }
    }


    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Purchase analysis", style = MaterialTheme.typography.titleLarge)
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Impulse signal: ${receiptImpulseLabel ?: "—"} (${receiptImpulseScore ?: 0}/100)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (reasons.isNotEmpty()) {
                        Text("Why:", style = MaterialTheme.typography.titleSmall)
                        reasons.forEach { r -> Text("• $r") }
                    } else {
                        Text("No reasons available.")
                    }
                }
            }
        }
        val positives = draftMap.values.count {it == 1}
        val neutrals = draftMap.values.count {it == 0}
        val negatives = draftMap.values.count {it == -1}

        val totalRated = positives + neutrals + negatives
        val moodLabel = when {
            totalRated == 0 -> "Not rated yet"
            positives >= negatives + 2 -> "Good shop 🙂"
            negatives >= positives + 2 -> "Regretful shop 🙁"
            else -> "Mixed feelings 😐"
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)){
                    Text("Your feelings so far", style = MaterialTheme.typography.titleMedium)
                    Text(moodLabel)
                    Text("🙂 $positives   😐 $neutrals   🙁 $negatives")
                }
            }
        }
        item {
            Text("How did each item feel?", style = MaterialTheme.typography.titleMedium)
        }
        if (items.isEmpty()) {
            item{Text("No items found.")}
        } else {
            items(items, key = {it.id}) { item ->
                ItemReactionRow(
                    itemName = item.name,
                    selectedReaction = draftMap[item.id],
                    onReact = { newReaction ->
                        draftMap[item.id] = newReaction
                    }
                )
            }
        }
        item {
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ){
                OutlinedButton(
                    onClick = {
                        draftMap.clear()
                        draftMap.putAll(savedMap)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasUnsavedChanges
                ) {
                    Text("Discard")
                }
                Button(
                    onClick = {
                        viewModel.saveItemReactions(receiptId, draftMap.toMap())
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasUnsavedChanges
                )
                {
                    Text("Save changes")
                }
            }
        }
    }
}

@Composable
private fun ItemReactionRow(
        itemName: String,
        selectedReaction: Int?,
        onReact: (Int) -> Unit
    ) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(itemName, style = MaterialTheme.typography.titleMedium)
                Text("Tap a face to react", style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // 🙂 positive = 1
                if (selectedReaction == 1) {
                    FilledIconButton(onClick = { onReact(1) }) {
                        Icon(Icons.Filled.SentimentSatisfied, contentDescription = "Positive")
                    }
                } else {
                    IconButton(onClick = { onReact(1) }) {
                        Icon(Icons.Filled.SentimentSatisfied, contentDescription = "Positive")
                    }
                }

                // 😐 neutral = 0
                if (selectedReaction == 0) {
                    FilledIconButton(onClick = { onReact(0) }) {
                        Icon(Icons.Filled.SentimentNeutral, contentDescription = "Neutral")
                    }
                } else {
                    IconButton(onClick = { onReact(0) }) {
                        Icon(Icons.Filled.SentimentNeutral, contentDescription = "Neutral")
                    }
                }

                // 🙁 negative = -1
                if (selectedReaction == -1) {
                    FilledIconButton(onClick = { onReact(-1) }) {
                        Icon(
                            Icons.Filled.SentimentDissatisfied,
                            contentDescription = "Negative"
                        )
                    }
                } else {
                    IconButton(onClick = { onReact(-1) }) {
                        Icon(
                            Icons.Filled.SentimentDissatisfied,
                            contentDescription = "Negative"
                        )
                    }
                }
            }
        }
    }
}

private fun parseReasons(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i -> arr.getString(i) }
    } catch (e: Exception) {
        emptyList()
    }
}
