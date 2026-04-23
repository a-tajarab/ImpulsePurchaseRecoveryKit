package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*
import org.json.JSONArray

/**
 * ItemDraft holds the editable state for a single item during edit mode.
 * Uses mutableStateOf delegation so Compose tracks changes and triggers
 * recomposition when the user types in the text fields.
 * This fixes the bug where plain var fields caused typing to not register.
 */
class ItemDraft(
    val id: Long,
    initialName: String,      // ← renamed to "initial..."
    initialPrice: String,
    initialQuantity: String
) {
    var name by mutableStateOf(initialName)
    var price by mutableStateOf(initialPrice)
    var quantity by mutableStateOf(initialQuantity)
}

/**
 * ReceiptDetailScreen displays the full details of a single scanned receipt.
 *
 * Two modes:
 * - View mode: shows purchase summary, regret/sentiment cards, items list, and analysis button
 * - Edit mode: allows the user to correct store name, date, time, total, and individual items
 *
 * Also contains a delete confirmation dialog and a modal bottom sheet
 * for the purchase analysis and item reaction features.
 */
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
    var editSubtotal by remember { mutableStateOf("") }
    var editTax by remember { mutableStateOf("") }

    val itemDrafts = remember { mutableStateListOf<ItemDraft>() }

    if (receipt == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ){
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
            editSubtotal = r.subtotal?.toString() ?: ""
            editTax = r.tax?.toString() ?: ""
            itemDrafts.clear()
            itemDrafts.addAll(items.map { item ->
                ItemDraft(
                    id = item.id,
                    initialName = item.name,
                    initialPrice = item.price.toString(),
                    initialQuantity = item.quantity.toString()
                )
            })
        }
    }
    // Delete confirmation dialog
    if (showDeleteDialog){
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Delete receipt?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                    },
            text = {
                Text(
                    "This will permanently delete this receipt and all its data. This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                   },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReceipt(r)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Error700,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    //Purchase analaysis bottom sheet
    if (showAnalysisSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAnalysisSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { // Teal header with store name, date, and action buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(
                    top = paddingValues.calculateTopPadding() + 12.dp,
                    bottom = 16.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back / Cancel Button
                    OutlinedButton(
                        onClick = {
                            if (isEditing) isEditing = false else onBack()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (isEditing) "Cancel" else "Back",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!isEditing) {
                        //View mode action buttons
                        Button(
                            onClick = onSetRegret,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Terra500,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Rate Regret", fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { isEditing = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        // Save button - persists all edits to the database
                        Button(
                            onClick = {
                                viewModel.updateReceiptDetails(
                                    receiptId = receiptId,
                                    storeName = editStoreName.ifBlank { null },
                                    purchaseDate = editDate.ifBlank { null },
                                    purchaseTime = editTime.ifBlank { null },
                                    totalAmount = editTotal.toDoubleOrNull(),
                                    subtotal = editSubtotal.toDoubleOrNull(),
                                    tax = editTax.toDoubleOrNull()
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
                                    } else if (draft.name.isNotBlank()) {
                                        viewModel.addItemToReceipt(
                                            receiptId = receiptId,
                                            name = draft.name,
                                            price = draft.price.toDoubleOrNull() ?: 0.0,
                                            quantity = draft.quantity.toIntOrNull() ?: 1
                                        )
                                    }
                                }
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Terra500,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Store name and date shown in view mode only
                if (!isEditing) {
                    Text(
                        r.storeName ?: "Unknown store",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${r.purchaseDate ?: "—"}${r.purchaseTime?.let { "  ·  $it" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isEditing) {
                // Purchase summary card - subtotal, tax, shipping, total
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Purchase summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryRow(
                                "Subtotal",
                                r.subtotal?.let { "£${"%.2f".format(it)}" } ?: "—")
                            SummaryRow("Tax", r.tax?.let { "£${"%.2f".format(it)}" } ?: "—")
                            r.shipping?.let {
                                SummaryRow("Shipping", "£${"%.2f".format(it)}")
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    r.totalAmount?.let { "£${"%.2f".format(it)}" } ?: "—",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                // ── Regret + sentiment row ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Regret card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    r.regretScore == null -> MaterialTheme.colorScheme.surface
                                    r.regretScore!! >= 7 -> Error100
                                    r.regretScore!! >= 4 -> Warning100
                                    else -> Success100
                                }
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Regret",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    r.regretScore?.let { "$it/10" } ?: "Not rated",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = when {
                                        r.regretScore == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                        r.regretScore!! >= 7 -> Error700
                                        r.regretScore!! >= 4 -> Warning700
                                        else -> Success700
                                    }
                                )
                            }
                        }
                        // Sentiment card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Sentiment",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    r.userSentimentLabel ?: "Not rated",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = when (r.userSentimentLabel) {
                                        "GOOD" -> Success700
                                        "BAD" -> Error700
                                        "MIXED" -> Warning700
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                // ── Items header ──
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${items.size} item${if (items.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // ── Items list ──
                if (items.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No items parsed for this receipt",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(items, key = { it.id }) { item ->
                        ItemViewCard(item = item)
                    }
                }
                // ── Analysis button ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Teal50
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Purchase analysis",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Teal700
                            )
                            Text(
                                "See your impulse signals and react to each item when you're ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { showAnalysisSheet = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Teal700,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Open analysis", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

            } else {
                // ── EDIT MODE ──
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Edit receipt details",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = editStoreName,
                                onValueChange = { editStoreName = it },
                                label = { Text("Store name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editDate,
                                onValueChange = { editDate = it },
                                label = { Text("Purchase date") },
                                placeholder = { Text("e.g. 28/11/2025") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editTime,
                                onValueChange = { input ->
                                    if (input.length <= 5 && input.all { c ->
                                            c.isDigit() || c == ':'
                                        }) editTime = input
                                },
                                label = { Text("Purchase time") },
                                placeholder = { Text("e.g. 14:30") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editTotal,
                                onValueChange = { editTotal = it },
                                label = { Text("Total (£)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editSubtotal,
                                onValueChange = { editSubtotal = it },
                                label = { Text("Subtotal (£)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = editTax,
                                onValueChange = { editTax = it },
                                label = { Text("Tax (£)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
                item {
                    Text(
                        "Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(itemDrafts, key = { it.id }) { draft ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = draft.name,
                                onValueChange = { draft.name = it },
                                label = { Text("Item name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = draft.price,
                                    onValueChange = { draft.price = it },
                                    label = { Text("Price (£)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                OutlinedTextField(
                                    value = draft.quantity,
                                    onValueChange = { draft.quantity = it },
                                    label = { Text("Qty") },
                                    modifier = Modifier.width(80.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
                //add missing item button
                item {
                    OutlinedButton(
                        onClick = {
                            itemDrafts.add(
                                ItemDraft(
                                    id = -System.currentTimeMillis(),
                                    initialName = "",
                                    initialPrice = "",
                                    initialQuantity = "1"
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Add missing item", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Single row showing a label and value side by side.
 * Used in the purchase summary card for subtotal, tax, and shipping.
 */
@Composable
private fun SummaryRow(label: String, value: String){
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Read-only item card showing item name, category, price, and quantity.
 * Displayed in view mode only — replaced by editable drafts in edit mode.
 */
@Composable
private fun ItemViewCard(item: ItemEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "£${"%.2f".format(item.price)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "x${item.quantity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bottom sheet content showing the purchase analysis for a receipt.
 * Displays the impulse signal score and reasons, overall mood summary,
 * and item-level reaction buttons (happy, neutral, regret).
 * Users can save or discard their item reactions from this sheet.
 */
@Composable
private fun AnalysisSheetContent(
    receiptId: Long,
    viewModel: ReceiptViewModel,
    receiptImpulseLabel: String?,
    receiptImpulseScore: Int?,
    impulseReasonsJson: String?,
    items: List<ItemEntity>,
    onClose: () -> Unit
) {
    val reasons = remember(impulseReasonsJson) {
        parseReasons(impulseReasonsJson)
    }

    val reactions by viewModel.getItemReactionsForReceipt(receiptId)
        .collectAsState(initial = emptyList())
    //savedMap holds the persisted reactions from the databse
    val savedMap = remember(reactions) {
        reactions.associate { it.itemId to it.reaction }
    }
    // draftMap holds unsaved changes made in this session
    val draftMap = remember(receiptId) {
        mutableStateMapOf<Long, Int>()
    }

    LaunchedEffect(savedMap) {
        draftMap.clear()
        draftMap.putAll(savedMap)
    }

    val hasUnsavedChanges by remember(savedMap) {
        derivedStateOf { savedMap != draftMap.toMap() }
    }
    //Calculate mood summary from current draft reactions
    val positives = draftMap.values.count { it == 1 }
    val neutrals = draftMap.values.count { it == 0 }
    val negatives = draftMap.values.count { it == -1 }

    val totalRated = positives + neutrals + negatives
    val moodLabel = when {
        totalRated == 0 -> "Not rated yet"
        positives >= negatives + 2 -> "Good shop 🙂"
        negatives >= positives + 2 -> "Regretful shop 🙁"
        else -> "Mixed feelings 😐"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Purchase analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Impulse signal card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (receiptImpulseLabel?.uppercase()) {
                        "HIGH" -> Error100
                        "MEDIUM" -> Warning100
                        else -> Success100
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Impulse signal",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${receiptImpulseLabel ?: "—"}  ${receiptImpulseScore ?: 0}/100",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (receiptImpulseLabel?.uppercase()) {
                                "HIGH" -> Error700
                                "MEDIUM" -> Warning700
                                else -> Success700
                            }
                        )
                    }
                    // Reasons list from impulse scoring engine
                    if (reasons.isNotEmpty()) {
                        reasons.forEach { reason ->
                            Text(
                                "• $reason",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Mood summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        moodLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Happy 🙂$positives",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success700
                        )
                        Text(
                            "Neutral 😐$neutrals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Regret 🙁$negatives",
                            style = MaterialTheme.typography.bodySmall,
                            color = Error700
                        )
                    }
                }
            }
        }
        //Save and discard buttons
        item {
            Text(
                "How did each item feel?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (items.isEmpty()) {
            item { Text("No items found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(items, key = { it.id }) { item ->
                ItemReactionRow(
                    itemName = item.name,
                    selectedReaction = draftMap[item.id],
                    onReact = { draftMap[item.id] = it }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { draftMap.clear(); draftMap.putAll(savedMap) },
                    modifier = Modifier.weight(1f),
                    enabled = hasUnsavedChanges,
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Discard") }

                Button(
                    onClick = { viewModel.saveItemReactions(receiptId, draftMap.toMap()) },
                    modifier = Modifier.weight(1f),
                    enabled = hasUnsavedChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Teal700,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Save", fontWeight = FontWeight.Bold) }
            }
        }

        item {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Close") }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Single item row with three emoji reaction buttons — happy, neutral, regret.
 * The selected reaction is highlighted with a filled background.
 * Unselected reactions appear at reduced opacity.
 */
@Composable
private fun ItemReactionRow(
        itemName: String,
        selectedReaction: Int?,
        onReact: (Int) -> Unit
    ) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Tap a face to react",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1 to Icons.Filled.SentimentSatisfied,
                    0 to Icons.Filled.SentimentNeutral,
                    -1 to Icons.Filled.SentimentDissatisfied
                ).forEach { (value, icon) ->
                    if (selectedReaction == value) {
                        FilledIconButton(
                            onClick = { onReact(value) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = when (value) {
                                    1 -> Success100
                                    -1 -> Error100
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = when (value) {
                                    1 -> Success700
                                    -1 -> Error700
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    } else {
                        IconButton(onClick = { onReact(value) }) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Parses the impulse reasons JSON array string into a list of reason strings.
 * Returns an empty list if the JSON is null, blank, or malformed.
 */
private fun parseReasons(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i -> arr.getString(i) }
    } catch (e: Exception) {
        emptyList()
    }
}
