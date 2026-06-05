package com.example.impulsepurchaserecoverykit.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.first
import com.example.impulsepurchaserecoverykit.database.entities.GoalEntity
import com.example.impulsepurchaserecoverykit.database.entities.SavingGoalEntity
import com.example.impulsepurchaserecoverykit.ui.theme.*
import com.example.impulsepurchaserecoverykit.viewmodel.ReceiptViewModel
import java.util.Calendar
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingGoalScreen(
    paddingValues: PaddingValues,
    viewModel: ReceiptViewModel,
    onBack: () -> Unit
) {
    // ── Data ───────────────────────────────────────────────────────────────
    val goal by viewModel.getGoal().collectAsState(initial = null)
    val savingGoals by viewModel.getSavingGoals().collectAsState(initial = emptyList())

    val now = remember { Calendar.getInstance() }
    val currentMonth = now.get(Calendar.MONTH) + 1
    val currentYear = now.get(Calendar.YEAR)
    val daysInMonth = remember { now.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val dayOfMonth = remember { now.get(Calendar.DAY_OF_MONTH) }
    val daysLeft = remember { (daysInMonth - dayOfMonth).coerceAtLeast(0) }

    val monthlySpend by viewModel.getMonthlySpend(currentYear, currentMonth)
        .collectAsState(initial = 0.0)
    val allReceipts by viewModel.getAllReceipts().collectAsState(initial = emptyList())

    val monthReceipts = allReceipts.filter { receipt ->
        receipt.purchaseDate?.matches(Regex("""\d{2}/\d{2}/\d{4}""")) == true &&
                receipt.purchaseDate.substring(3, 5).toIntOrNull() == currentMonth &&
                receipt.purchaseDate.substring(6, 10).toIntOrNull() == currentYear
    }
    val highImpulseCount = monthReceipts.count { it.impulseLabel == "HIGH" }
    val lowImpulseSpend = monthReceipts.filter { it.impulseLabel == "LOW" }
        .sumOf { it.totalAmount ?: 0.0 }

    // ── UI state ───────────────────────────────────────────────────────────
    //
    // Three-value state:
    //   null  = DB hasn't emitted yet — show a loading spinner
    //   false = budget exists → show goals/progress view  ← default when goal set
    //   true  = show budget edit form (first-time setup OR explicit "Edit budget" tap)
    //
    // ROOT CAUSE FIX:
    // collectAsState(initial = null) gives goal = null for one frame BEFORE
    // Room emits the real DB value. Reacting to that null in LaunchedEffect(goal)
    // incorrectly set isEditingBudget = true before the real value arrived.
    //
    // Fix: use .first() to collect the REAL first DB emission directly,
    // completely bypassing the collectAsState placeholder null.
    var isEditingBudget by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        // .first() suspends until Room emits the actual stored value.
        // This is never the collectAsState placeholder — it's the real DB row.
        val realGoal = viewModel.getGoal().first()
        // Only show the edit form if no budget has ever been set.
        // If a budget exists, always open in goals view mode.
        isEditingBudget = realGoal == null
    }

    // Keep the budget text field in sync when the goal changes
    var limitInput by remember { mutableStateOf("") }
    LaunchedEffect(goal) {
        goal?.let { limitInput = it.monthlyLimit.toString() }
    }

    // Saving goal dialogs
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalBeingEdited by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingGoalEntity?>(null) }

    // ── Screen shell ───────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(
                    top = paddingValues.calculateTopPadding() + 12.dp,
                    bottom = 20.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text("Monthly Goal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                    if (goal != null) {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteGoal(); onBack() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete budget", tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                        }
                    }
                }
                Text(
                    "Track your spending and celebrate savings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 48.dp)
                )
            }
        }

        // ── Body ───────────────────────────────────────────────────────────
        when {

            // 1. DB hasn't emitted yet — show a centred spinner
            isEditingBudget == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Teal700,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // 2. Budget exists and not explicitly editing — show goals view
            isEditingBudget == false && goal != null -> {
                val spent = monthlySpend ?: 0.0
                val limit = goal!!.monthlyLimit
                val remaining = (limit - spent).coerceAtLeast(0.0)
                val isOverBudget = spent > limit
                val progress = (spent / limit).coerceIn(0.0, 1.0).toFloat()
                val dailyAllowance = if (daysLeft > 0 && !isOverBudget) remaining / daysLeft else 0.0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GoalDonutHero(spent = spent, limit = limit, daysLeft = daysLeft)

                    // Edit budget — just below the donut, easy to find
                    OutlinedButton(
                        onClick = { isEditingBudget = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal700),
                        border = BorderStroke(1.dp, Teal700.copy(alpha = 0.4f))
                    ) {
                        Text("Edit monthly budget", fontWeight = FontWeight.SemiBold)
                    }

                    if (dailyAllowance > 0.0) {
                        DailyAllowanceCard(dailyAllowance = dailyAllowance, daysLeft = daysLeft)
                    }

                    SavingGoalsSection(
                        goals = savingGoals,
                        monthlyRemaining = remaining,
                        onAdd = { showAddGoalDialog = true },
                        onEdit = { goalBeingEdited = it },
                        onDelete = { goalToDelete = it },
                        onMoveUp = { viewModel.moveSavingGoalUp(savingGoals, it) },
                        onMoveDown = { viewModel.moveSavingGoalDown(savingGoals, it) }
                    )

                    MindfulSpendingCard(lowImpulseSpend = lowImpulseSpend, highImpulseCount = highImpulseCount)

                    if (progress >= 0.8f) {
                        KiraGoalWarning(
                            isOverBudget = isOverBudget,
                            remaining = remaining,
                            topGoal = savingGoals.minByOrNull { it.priority }
                        )
                    }

                }
            }

            // 3. No budget set yet, OR user tapped "Edit monthly budget"
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = paddingValues.calculateBottomPadding() + 16.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BudgetEditForm(
                        isNew = goal == null,
                        limitInput = limitInput,
                        onLimitChange = { limitInput = it },
                        // Cancel returns to goals view — only available if a budget already exists
                        onCancel = if (goal != null) ({ isEditingBudget = false }) else null,
                        onSave = {
                            val l = limitInput.toDoubleOrNull() ?: return@BudgetEditForm
                            val cal = Calendar.getInstance()
                            viewModel.upsertGoal(
                                GoalEntity(
                                    monthlyLimit = l,
                                    goalMonth = cal.get(Calendar.MONTH) + 1,
                                    goalYear = cal.get(Calendar.YEAR)
                                )
                            )
                            isEditingBudget = false  // always return to goals view after saving
                        }
                    )
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────
    if (showAddGoalDialog) {
        AddEditGoalDialog(
            existingGoal = null,
            onSave = { name, target ->
                viewModel.addSavingGoal(name, target)
                showAddGoalDialog = false
            },
            onDismiss = { showAddGoalDialog = false }
        )
    }

    goalBeingEdited?.let { editing ->
        AddEditGoalDialog(
            existingGoal = editing,
            onSave = { name, target ->
                viewModel.updateSavingGoal(editing.copy(name = name, targetAmount = target))
                goalBeingEdited = null
            },
            onDismiss = { goalBeingEdited = null }
        )
    }

    goalToDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            title = { Text("Remove goal?", fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${toDelete.name}\" from your saving goals?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteSavingGoal(toDelete); goalToDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error700)
                ) { Text("Remove", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Saving Goals Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavingGoalsSection(
    goals: List<SavingGoalEntity>,
    monthlyRemaining: Double,
    onAdd: () -> Unit,
    onEdit: (SavingGoalEntity) -> Unit,
    onDelete: (SavingGoalEntity) -> Unit,
    onMoveUp: (SavingGoalEntity) -> Unit,
    onMoveDown: (SavingGoalEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val sorted = goals.sortedBy { it.priority }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Saving Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    if (goals.isEmpty()) "Add things you're working towards"
                    else "${goals.size} goal${if (goals.size > 1) "s" else ""}, ordered by priority",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = onAdd,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Teal700.copy(alpha = 0.1f),
                    contentColor = Teal700
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (goals.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No saving goals yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Tap + Add to create your first goal — a holiday, gadget, or anything you're working toward.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            sorted.forEachIndexed { index, goal ->
                SavingGoalCard(
                    goal = goal,
                    displayRank = index + 1,
                    totalGoals = sorted.size,
                    cumulativeTarget = sorted.take(index + 1).sumOf { it.targetAmount },
                    monthlyRemaining = monthlyRemaining,
                    onEdit = { onEdit(goal) },
                    onDelete = { onDelete(goal) },
                    onMoveUp = if (index > 0) ({ onMoveUp(goal) }) else null,
                    onMoveDown = if (index < sorted.size - 1) ({ onMoveDown(goal) }) else null
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual Saving Goal Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SavingGoalCard(
    goal: SavingGoalEntity,
    displayRank: Int,
    totalGoals: Int,
    cumulativeTarget: Double,
    monthlyRemaining: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (displayRank) { 1 -> Teal700; 2 -> Terra500; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val badgeBg   = when (displayRank) { 1 -> Teal700.copy(alpha = 0.12f); 2 -> Terra500.copy(alpha = 0.12f); else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f) }

    val monthsToReach: Int? = if (monthlyRemaining > 0.0) ceil(cumulativeTarget / monthlyRemaining).toInt() else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
                    Text(displayRank.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = badgeColor)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (monthsToReach != null) {
                        Text(
                            if (displayRank == 1) "~$monthsToReach months at current saving rate"
                            else "~$monthsToReach months (after higher priorities)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text("£${"%.0f".format(goal.targetAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = badgeColor)
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Increase priority", modifier = Modifier.size(16.dp), tint = if (onMoveUp != null) Teal700 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                    IconButton(onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Decrease priority", modifier = Modifier.size(16.dp), tint = if (onMoveDown != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(badgeBg).padding(horizontal = 7.dp, vertical = 3.dp)) {
                    Text(
                        when (displayRank) { 1 -> "Top priority"; 2 -> "Second priority"; totalGoals -> "Lowest priority"; else -> "Priority $displayRank" },
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = badgeColor
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Error700.copy(alpha = 0.7f))
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", style = MaterialTheme.typography.labelSmall, color = Error700.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add / Edit Goal Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddEditGoalDialog(
    existingGoal: SavingGoalEntity?,
    onSave: (name: String, targetAmount: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(existingGoal?.name ?: "") }
    var targetInput by remember { mutableStateOf(existingGoal?.targetAmount?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (existingGoal != null) "Edit saving goal" else "Add saving goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    label = { Text("What are you saving for?") },
                    placeholder = { Text("e.g. Holiday, PS5, New bike") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal700, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )
                OutlinedTextField(
                    value = targetInput, onValueChange = { targetInput = it },
                    label = { Text("Target amount") }, placeholder = { Text("e.g. 800") },
                    leadingIcon = { Text("£", fontWeight = FontWeight.Bold, color = Terra500, modifier = Modifier.padding(start = 4.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terra500, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
                )
                if (existingGoal == null) {
                    Text("New goals are added at the lowest priority. Use the ↑ arrows to promote them.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = { targetInput.toDoubleOrNull()?.let { onSave(nameInput.trim(), it) } },
                        enabled = nameInput.isNotBlank() && targetInput.toDoubleOrNull() != null,
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700, contentColor = Color.White)
                    ) { Text(if (existingGoal != null) "Save" else "Add goal", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Budget Edit Form
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BudgetEditForm(
    isNew: Boolean,
    limitInput: String,
    onLimitChange: (String) -> Unit,
    onCancel: (() -> Unit)?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (isNew) "Set your monthly budget" else "Edit monthly budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("This is the total you aim to spend each month. Add individual saving goals once the budget is set.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = limitInput, onValueChange = onLimitChange,
                label = { Text("Monthly spending limit") }, placeholder = { Text("e.g. 500") },
                leadingIcon = { Text("£", fontWeight = FontWeight.Bold, color = Teal700, modifier = Modifier.padding(start = 4.dp)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal700, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onCancel != null) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                }
                Button(
                    onClick = onSave, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700, contentColor = Color.White)
                ) { Text(if (isNew) "Set budget" else "Save", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GoalDonutHero(spent: Double, limit: Double, daysLeft: Int, modifier: Modifier = Modifier) {
    val rawProgress = (spent / limit).coerceIn(0.0, 1.0).toFloat()
    val isOver = spent > limit
    val remaining = (limit - spent).coerceAtLeast(0.0)
    val arcColor = when { isOver -> Error700; rawProgress >= 0.8f -> Terra500; else -> Teal700 }
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, animationSpec = tween(1000), label = "donutProgress")
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(modifier = Modifier.size(188.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = 20.dp.toPx(); val inset = sw / 2f
                    val tl = Offset(inset, inset); val sz = Size(size.width - sw, size.height - sw)
                    drawArc(color = arcColor.copy(alpha = 0.1f), startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = sz, style = Stroke(width = sw, cap = StrokeCap.Round))
                    if (animatedProgress > 0f) drawArc(color = arcColor, startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false, topLeft = tl, size = sz, style = Stroke(width = sw, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("£${"%.2f".format(if (isOver) spent - limit else remaining)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = if (isOver) Error700 else MaterialTheme.colorScheme.onSurface)
                    Text(if (isOver) "over budget" else "remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                DonutLegendItem(dotColor = arcColor, label = "Spent", value = "£${"%.2f".format(spent)}")
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
                DonutLegendItem(dotColor = MaterialTheme.colorScheme.onSurfaceVariant, label = "Budget", value = "£${"%.2f".format(limit)}")
                Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
                DonutLegendItem(dotColor = Teal700.copy(alpha = 0.7f), label = "Days left", value = "$daysLeft")
            }
        }
    }
}

@Composable
private fun DonutLegendItem(dotColor: Color, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Canvas(modifier = Modifier.size(7.dp)) { drawCircle(color = dotColor) }
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DailyAllowanceCard(dailyAllowance: Double, daysLeft: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Teal50), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Teal700.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Teal700, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Daily allowance", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Teal700)
                    Text("You can spend this per day for $daysLeft more days", style = MaterialTheme.typography.bodySmall, color = Teal700.copy(alpha = 0.65f))
                }
            }
            Text("£${"%.2f".format(dailyAllowance)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Teal700)
        }
    }
}

@Composable
private fun MindfulSpendingCard(lowImpulseSpend: Double, highImpulseCount: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Teal50), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Teal700, modifier = Modifier.size(18.dp))
                Text("Mindful spending this month", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Teal700)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SavingsStatItem("Low impulse spend", "£${"%.2f".format(lowImpulseSpend)}", Teal700)
                SavingsStatItem("High impulse buys", "$highImpulseCount receipts", Terra500)
            }
        }
    }
}

@Composable
private fun SavingsStatItem(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Teal700.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun KiraGoalWarning(isOverBudget: Boolean, remaining: Double, topGoal: SavingGoalEntity?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isOverBudget) Error100 else Terra50), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isOverBudget) Error700 else Terra700), contentAlignment = Alignment.Center) {
                Text("K", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isOverBudget) "You have gone over budget" else "You are close to your limit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isOverBudget) Error700 else Terra700)
                val message = if (isOverBudget) buildString {
                    append("You have exceeded your monthly budget. ")
                    if (topGoal != null) append("Every extra pound now is a pound less towards your ${topGoal.name}. ")
                    append("Hold off on non-essential purchases for the rest of the month.")
                } else buildString {
                    append("You only have £${"%.2f".format(remaining)} left. ")
                    if (topGoal != null) append("Stay under your limit to keep on track for your ${topGoal.name}. ")
                    append("Pause before impulse purchases — ask KIRA if you need help.")
                }
                Text(message, style = MaterialTheme.typography.bodySmall, color = if (isOverBudget) Error700 else Terra700, lineHeight = 18.sp)
            }
        }
    }
}