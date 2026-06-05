package com.example.impulsepurchaserecoverykit.ui.screens

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.impulsepurchaserecoverykit.database.entities.ItemEntity
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import com.example.impulsepurchaserecoverykit.ui.theme.*
import org.json.JSONArray

class ItemDraft(val id: Long, initialName: String, initialPrice: String, initialQuantity: String) {
    var name by mutableStateOf(initialName)
    var price by mutableStateOf(initialPrice)
    var quantity by mutableStateOf(initialQuantity)
}

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
    val reactions by viewModel.getItemReactionsForReceipt(receiptId).collectAsState(initial = emptyList())

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    var editStoreName by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editTime by remember { mutableStateOf("") }
    var editTotal by remember { mutableStateOf("") }
    var editSubtotal by remember { mutableStateOf("") }
    var editTax by remember { mutableStateOf("") }
    val itemDrafts = remember { mutableStateListOf<ItemDraft>() }

    // Reaction draft state — synced from DB, saved immediately on tap
    val savedMap = remember(reactions) { reactions.associate { it.itemId to it.reaction } }
    val draftMap = remember { mutableStateMapOf<Long, Int>() }
    LaunchedEffect(savedMap) { savedMap.forEach { (k, v) -> if (!draftMap.containsKey(k)) draftMap[k] = v } }

    val positives = draftMap.values.count { it == 1 }
    val neutrals  = draftMap.values.count { it == 0 }
    val negatives = draftMap.values.count { it == -1 }
    val totalRated = positives + neutrals + negatives

    if (receipt == null) { Box(Modifier.fillMaxSize().padding(paddingValues), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }; return }
    val r = receipt!!

    LaunchedEffect(isEditing) {
        if (isEditing) {
            editStoreName = r.storeName ?: ""; editDate = r.purchaseDate ?: ""; editTime = r.purchaseTime ?: ""
            editTotal = r.totalAmount?.toString() ?: ""; editSubtotal = r.subtotal?.toString() ?: ""; editTax = r.tax?.toString() ?: ""
            itemDrafts.clear(); itemDrafts.addAll(items.map { ItemDraft(it.id, it.name, it.price.toString(), it.quantity.toString()) })
        }
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, shape = RoundedCornerShape(16.dp), containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete receipt?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete this receipt and all its data.") },
            confirmButton = { Button(onClick = { viewModel.deleteReceipt(r); showDeleteDialog = false; onBack() }, colors = ButtonDefaults.buttonColors(containerColor = Error700, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) { Text("Delete", fontWeight = FontWeight.Bold) } },
            dismissButton = { OutlinedButton(onClick = { showDeleteDialog = false }, shape = RoundedCornerShape(10.dp)) { Text("Cancel") } }
        )
    }
    if (showImageViewer && r.imageUri != null) { ReceiptImageViewer(r.imageUri) { showImageViewer = false } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(top = paddingValues.calculateTopPadding() + 12.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { if (isEditing) isEditing = false else onBack() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)), shape = RoundedCornerShape(10.dp)) { Text(if (isEditing) "Cancel" else "Back", fontWeight = FontWeight.SemiBold) }
                    if (!isEditing) {
                        Button(onClick = onSetRegret, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Terra500, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) { Text("Rate Regret", fontWeight = FontWeight.Bold) }
                        IconButton(onClick = { isEditing = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onPrimary) }
                        IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)) }
                    } else {
                        Button(onClick = {
                            viewModel.updateReceiptDetails(receiptId, editStoreName.ifBlank { null }, editDate.ifBlank { null }, editTime.ifBlank { null }, editTotal.toDoubleOrNull(), editSubtotal.toDoubleOrNull(), editTax.toDoubleOrNull())
                            itemDrafts.forEach { d -> if (d.id > 0) viewModel.updateItem(d.id, d.name, d.price.toDoubleOrNull() ?: 0.0, d.quantity.toIntOrNull() ?: 1) else if (d.name.isNotBlank()) viewModel.addItemToReceipt(receiptId, d.name, d.price.toDoubleOrNull() ?: 0.0, d.quantity.toIntOrNull() ?: 1) }
                            isEditing = false
                        }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Terra500, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) { Text("Save changes", fontWeight = FontWeight.Bold) }
                    }
                }
                if (!isEditing) {
                    Text(r.storeName ?: "Unknown store", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                    Text("${r.purchaseDate ?: "—"}${r.purchaseTime?.let { "  ·  $it" } ?: ""}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, bottom = paddingValues.calculateBottomPadding() + 24.dp, start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isEditing) {
                // Image preview
                r.imageUri?.let { uri ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth().height(160.dp).clickable { showImageViewer = true }, shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                            Box {
                                AsyncImage(model = Uri.parse(uri), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)), startY = 60f)))
                                Row(Modifier.align(Alignment.BottomStart).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Receipt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text("Tap to view full receipt", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Purchase summary + impulse score
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Purchase summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryRow("Subtotal", r.subtotal?.let { "£${"%.2f".format(it)}" } ?: "—")
                            SummaryRow("Tax", r.tax?.let { "£${"%.2f".format(it)}" } ?: "—")
                            r.shipping?.let { SummaryRow("Shipping", "£${"%.2f".format(it)}") }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(r.totalAmount?.let { "£${"%.2f".format(it)}" } ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            // Impulse score row
                            r.impulseLabel?.let { label ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Impulse risk", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(when (label.uppercase()) { "HIGH" -> Error100; "MEDIUM" -> Warning100; else -> Success100 }).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text("$label  ${r.impulseScore ?: 0}/100", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = when (label.uppercase()) { "HIGH" -> Error700; "MEDIUM" -> Warning700; else -> Success700 })
                                    }
                                }
                            }
                        }
                    }
                }

                // Regret + sentiment
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = when { r.regretScore == null -> MaterialTheme.colorScheme.surface; r.regretScore!! >= 7 -> Error100; r.regretScore!! >= 4 -> Warning100; else -> Success100 }), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Regret", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(r.regretScore?.let { "$it/10" } ?: "Not rated", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = when { r.regretScore == null -> MaterialTheme.colorScheme.onSurfaceVariant; r.regretScore!! >= 7 -> Error700; r.regretScore!! >= 4 -> Warning700; else -> Success700 })
                            }
                        }
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Sentiment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Text(r.userSentimentLabel ?: "Not rated", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = when (r.userSentimentLabel) { "GOOD" -> Success700; "BAD" -> Error700; "MIXED" -> Warning700; else -> MaterialTheme.colorScheme.onSurfaceVariant })
                            }
                        }
                    }
                }

                // Mood summary banner — live, updates as user taps
                item {
                    val (moodBg, moodFg, moodText) = when {
                        totalRated == 0 -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Rate the items below ↓")
                        positives >= negatives + 2 -> Triple(Success100, Success700, "Good shop 🙂  ·  $positives happy, $negatives regret")
                        negatives >= positives + 2 -> Triple(Error100, Error700, "Regretful shop 🙁  ·  $negatives regret, $positives happy")
                        else -> Triple(Warning100, Warning700, "Mixed feelings 😐  ·  $positives happy, $negatives regret")
                    }
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(moodBg).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(moodText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = moodFg)
                    }
                }

                // Items header
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("${items.size} item${if (items.size != 1) "s" else ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── OPTION A: items with inline reaction buttons ──
                if (items.isEmpty()) {
                    item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { Text("No items parsed for this receipt", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } } }
                } else {
                    items(items, key = { it.id }) { item ->
                        InlineReactionItemCard(
                            item = item,
                            selectedReaction = draftMap[item.id],
                            onReact = { value ->
                                draftMap[item.id] = value
                                viewModel.saveItemReactions(receiptId, draftMap.toMap())
                            }
                        )
                    }
                }

            } else {
                // Edit mode (unchanged)
                item {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Edit receipt details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            OutlinedTextField(value = editStoreName, onValueChange = { editStoreName = it }, label = { Text("Store name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                            OutlinedTextField(value = editDate, onValueChange = { editDate = it }, label = { Text("Purchase date") }, placeholder = { Text("e.g. 28/11/2025") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                            OutlinedTextField(value = editTime, onValueChange = { input -> if (input.length <= 5 && input.all { c -> c.isDigit() || c == ':' }) editTime = input }, label = { Text("Purchase time") }, placeholder = { Text("e.g. 14:30") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(10.dp))
                            OutlinedTextField(value = editTotal, onValueChange = { editTotal = it }, label = { Text("Total (£)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(10.dp))
                            OutlinedTextField(value = editSubtotal, onValueChange = { editSubtotal = it }, label = { Text("Subtotal (£)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(10.dp))
                            OutlinedTextField(value = editTax, onValueChange = { editTax = it }, label = { Text("Tax (£)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(10.dp))
                        }
                    }
                }
                item { Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) }
                items(itemDrafts, key = { it.id }) { draft ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = draft.name, onValueChange = { draft.name = it }, label = { Text("Item name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = draft.price, onValueChange = { draft.price = it }, label = { Text("Price (£)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(10.dp))
                                OutlinedTextField(value = draft.quantity, onValueChange = { draft.quantity = it }, label = { Text("Qty") }, modifier = Modifier.width(80.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(10.dp))
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { itemDrafts.add(ItemDraft(-System.currentTimeMillis(), "", "", "1")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add missing item", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── OPTION A: inline reactions always visible ──────────────────────────────

@Composable
private fun InlineReactionItemCard(item: ItemEntity, selectedReaction: Int?, onReact: (Int) -> Unit) {
    val reactionBorder = when (selectedReaction) {
        1  -> Success700.copy(alpha = 0.4f)
        -1 -> Error700.copy(alpha = 0.4f)
        0  -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else -> Color.Transparent
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selectedReaction != null) androidx.compose.foundation.BorderStroke(1.5.dp, reactionBorder) else null,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Item info row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(item.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("£${"%.2f".format(item.price)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("x${item.quantity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Reaction buttons — always visible
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "😊  Happy", 0 to "😐  Ok", -1 to "🙁  Regret").forEach { (value, label) ->
                    val isSelected = selectedReaction == value
                    val bg = if (isSelected) when (value) { 1 -> Success100; -1 -> Error100; else -> MaterialTheme.colorScheme.surfaceVariant } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val fg = if (isSelected) when (value) { 1 -> Success700; -1 -> Error700; else -> MaterialTheme.colorScheme.onSurfaceVariant } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(bg).clickable { onReact(value) }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = fg, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────

@Composable private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun parseReasons(json: String?): List<String> {
    if (json.isNullOrBlank())
        return emptyList();
    return try { val arr = org.json.JSONArray(json);
        List(arr.length()) {
        i -> arr.getString(i) } }
    catch (e: Exception) { emptyList() } }

@Composable
private fun ReceiptImageViewer(imageUri: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }; var offset by remember { mutableStateOf(Offset.Zero) }
    val ts = rememberTransformableState { z, p, _ -> scale = (scale * z).coerceIn(1f, 5f); offset += p }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(model = Uri.parse(imageUri), contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y).transformable(ts))
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }
    }
}