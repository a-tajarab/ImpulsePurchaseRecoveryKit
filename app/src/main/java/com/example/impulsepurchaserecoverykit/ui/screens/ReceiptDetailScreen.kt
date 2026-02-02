package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import org.json.JSONArray


@OptIn(ExperimentalMaterial3Api::class)
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

    // Bottom sheet state
    var showAnalysisSheet by remember { mutableStateOf(false) }

    if (showAnalysisSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnalysisSheet = false },
            // optional: skipPartiallyExpanded = true, // if you want
        ) {
            AnalysisSheetContent(
                receiptId = receiptId,
                viewModel = viewModel,
                receiptImpulseLabel = receipt?.impulseLabel,
                receiptImpulseScore = receipt?.impulseScore,
                impulseReasonsJson = receipt?.impulseReasonsJson,
                items = items,
                onClose = { showAnalysisSheet = false }
            )
        }
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onSetRegret) { Text("Rate Regret") }
        }

        if (receipt == null) {
            Text("Receipt not found.")
            return@Column
        }

        // ===== Facts Section (no judgement) =====
        Text(receipt.storeName ?: "Unknown store", style = MaterialTheme.typography.headlineSmall)
        Text("Date: ${receipt.purchaseDate ?: "—"}")
        Text("Subtotal: £${receipt.subtotal ?: "—"}")
        Text("Tax: £${receipt.tax ?: "—"}")
        Text("Total: £${receipt.totalAmount ?: "—"}")
        Text("Regret: ${receipt.regretScore?.toString() ?: "Not rated"}")

        Divider()

        // ===== Items =====
        Text("Items", style = MaterialTheme.typography.titleMedium)

        if (items.isEmpty()) {
            Text("No items parsed for this receipt.")
        } else {
            items.forEach { item ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        Text("Category: ${item.category}")
                        Text("Price: £${item.price}  x${item.quantity}")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Divider()

        // ===== Analysis Preview (collapsed by default) =====
        Text("Analysis of this purchase", style = MaterialTheme.typography.titleMedium)
        Text(
            "Open when you’re ready — this includes impulse signals and lets you react to each item.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = { showAnalysisSheet = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open analysis")
        }

        Spacer(Modifier.height(24.dp))
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
        val positives = reactions.count {it.reaction == 1}
        val neutrals = reactions.count {it.reaction == 0}
        val negatives = reactions.count {it.reaction == -1}

        val totalRated = positives + neutrals + negatives
        val moodLabel = when {
            totalRated == 0 -> "Not rated yet"
            positives >= positives + 2 -> "Good shop 🙂"
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
                    selectedReaction = reactionMap[item.id]?.reaction,
                    onReact = { newReaction ->
                        viewModel.setItemReaction(receiptId, item.id, newReaction)
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