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

/**
 * Monthly spending goal screen — budget tracking, saving goals, and mindful spending insights.
 *
 * The screen operates in three states controlled by [isEditingBudget]:
 *
 * - **null** — the database has not yet emitted a value. A loading spinner is shown.
 * - **false** — a budget exists and the user is in view mode. Shows the [GoalDonutHero],
 *   [DailyAllowanceCard], [SavingGoalsSection], [MindfulSpendingCard], and optionally
 *   [KiraGoalWarning] when spend reaches 80% or more of the limit.
 * - **true** — no budget exists yet, or the user tapped "Edit monthly budget". Shows
 *   the [BudgetEditForm].
 *
 * **Critical bug fix — isEditingBudget initialisation:**
 *
 * `collectAsState(initial = null)` emits null for one frame before Room returns the
 * real database value. A naive `LaunchedEffect(goal) { isEditingBudget = goal == null }`
 * would incorrectly open the edit form on every screen open because it reacts to the
 * placeholder null before the real emission arrives.
 *
 * Fix: `LaunchedEffect(Unit)` calls `viewModel.getGoal().first()` which suspends until
 * Room emits the actual stored value — completely bypassing the placeholder null. This
 * ensures the edit form only opens when no budget genuinely exists.
 *
 * The screen also calculates:
 * - **Daily allowance** — remaining budget divided by days left in the month
 * - **High impulse count** — receipts labelled HIGH in the current month
 * - **Low impulse spend** — total of receipts labelled LOW in the current month
 *
 * @param paddingValues Padding applied by the parent [Scaffold]
 * @param viewModel The shared [ReceiptViewModel] providing all goal and receipt data
 * @param onBack Callback invoked when the user taps the back arrow in the header
 */
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

    // Filter to receipts in the current month with a parseable DD/MM/YYYY date
    val monthReceipts = allReceipts.filter { receipt ->
        receipt.purchaseDate?.matches(Regex("""\d{2}/\d{2}/\d{4}""")) == true &&
                receipt.purchaseDate.substring(3, 5).toIntOrNull() == currentMonth &&
                receipt.purchaseDate.substring(6, 10).toIntOrNull() == currentYear
    }

    // Mindful spending stats for the current month
    val highImpulseCount = monthReceipts.count { it.impulseLabel == "HIGH" }
    val lowImpulseSpend  = monthReceipts.filter { it.impulseLabel == "LOW" }
        .sumOf { it.totalAmount ?: 0.0 }

    // ── UI state ───────────────────────────────────────────────────────────
    //
    // Three-value state machine:
    //   null  = DB hasn't emitted yet — show a loading spinner
    //   false = budget exists → show goals/progress view (default when goal is set)
    //   true  = show budget edit form (first-time setup OR explicit "Edit budget" tap)
    //
    // CRITICAL FIX — collectAsState(initial = null) emits null for one frame BEFORE
    // Room returns the real value. Reacting to that null in LaunchedEffect(goal)
    // would incorrectly open the edit form before the real DB value arrives.
    //
    // Fix: use .first() to collect the real first DB emission directly,
    // completely bypassing the collectAsState placeholder null.
    var isEditingBudget by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        // .first() suspends until Room emits the actual stored value — never the placeholder
        val realGoal = viewModel.getGoal().first()
        // Only open edit form when no budget exists; always open in view mode if one does
        isEditingBudget = realGoal == null
    }

    // Keep the budget text field in sync when goal data changes
    var limitInput by remember { mutableStateOf("") }
    LaunchedEffect(goal) { goal?.let { limitInput = it.monthlyLimit.toString() } }

    // Dialog state for saving goal add/edit/delete operations
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalBeingEdited by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingGoalEntity?>(null) }

    // ── Screen shell ───────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // Midnight navy header with back button and optional delete budget icon
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(top = paddingValues.calculateTopPadding() + 12.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Text("Monthly Goal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                    if (goal != null) {
                        Spacer(Modifier.weight(1f))
                        // Delete budget — only available when a budget exists
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

        // ── Body — three-state switch ──────────────────────────────────────
        when {

            // State 1 — DB has not yet emitted; show loading spinner
            isEditingBudget == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal700, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                }
            }

            // State 2 — Budget exists and view mode is active
            isEditingBudget == false && goal != null -> {
                val spent = monthlySpend ?: 0.0
                val limit = goal!!.monthlyLimit
                val remaining = (limit - spent).coerceAtLeast(0.0)
                val isOverBudget = spent > limit
                val progress = (spent / limit).coerceIn(0.0, 1.0).toFloat()
                // Daily allowance is zero when over budget or no days remain
                val dailyAllowance = if (daysLeft > 0 && !isOverBudget) remaining / daysLeft else 0.0

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GoalDonutHero(spent = spent, limit = limit, daysLeft = daysLeft)

                    // Edit budget button — placed directly below the donut for discoverability
                    OutlinedButton(
                        onClick = { isEditingBudget = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal700),
                        border = BorderStroke(1.dp, Teal700.copy(alpha = 0.4f))
                    ) { Text("Edit monthly budget", fontWeight = FontWeight.SemiBold) }

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

                    // KIRA warning card — only shown when spend reaches 80% of the limit
                    if (progress >= 0.8f) {
                        KiraGoalWarning(
                            isOverBudget = isOverBudget,
                            remaining = remaining,
                            topGoal = savingGoals.minByOrNull { it.priority }
                        )
                    }
                }
            }

            // State 3 — No budget set yet, or "Edit monthly budget" was tapped
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BudgetEditForm(
                        isNew = goal == null,
                        limitInput = limitInput,
                        onLimitChange = { limitInput = it },
                        // Cancel is only available when a budget already exists
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
                            isEditingBudget = false
                        }
                    )
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    // Add goal dialog
    if (showAddGoalDialog) {
        AddEditGoalDialog(
            existingGoal = null,
            onSave = { name, target -> viewModel.addSavingGoal(name, target); showAddGoalDialog = false },
            onDismiss = { showAddGoalDialog = false }
        )
    }

    // Edit goal dialog — shown when a goal card's Edit button is tapped
    goalBeingEdited?.let { editing ->
        AddEditGoalDialog(
            existingGoal = editing,
            onSave = { name, target -> viewModel.updateSavingGoal(editing.copy(name = name, targetAmount = target)); goalBeingEdited = null },
            onDismiss = { goalBeingEdited = null }
        )
    }

    // Delete goal confirmation dialog
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

/**
 * Section displaying all saving goals with add, edit, delete, and priority controls.
 *
 * Goals are sorted by [SavingGoalEntity.priority] ascending (1 = highest priority)
 * and displayed as [SavingGoalCard] composables. The section header shows the goal
 * count and an Add button. An empty state card is shown when no goals exist.
 *
 * The months-to-reach estimate on each card is calculated cumulatively — goal 2
 * accounts for the time needed to complete goal 1 first, and so on.
 *
 * @param goals The current list of [SavingGoalEntity] records (unsorted)
 * @param monthlyRemaining The budget remaining for the current month in GBP,
 *                         used to calculate months-to-reach estimates
 * @param onAdd Callback invoked when the Add button is tapped
 * @param onEdit Callback invoked with the goal to edit when Edit is tapped
 * @param onDelete Callback invoked with the goal to delete when Remove is tapped
 * @param onMoveUp Callback invoked with the goal to promote in priority order
 * @param onMoveDown Callback invoked with the goal to demote in priority order
 * @param modifier Optional [Modifier] applied to the root [Column]
 */
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Teal700.copy(alpha = 0.1f), contentColor = Teal700)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (goals.isEmpty()) {
            // Empty state — prompts the user to create their first saving goal
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No saving goals yet", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Tap + Add to create your first goal — a holiday, gadget, or anything you're working toward.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            // Render each goal card with cumulative target for accurate months-to-reach
            sorted.forEachIndexed { index, goal ->
                SavingGoalCard(
                    goal = goal,
                    displayRank = index + 1,
                    totalGoals = sorted.size,
                    cumulativeTarget = sorted.take(index + 1).sumOf { it.targetAmount },
                    monthlyRemaining = monthlyRemaining,
                    onEdit = { onEdit(goal) },
                    onDelete = { onDelete(goal) },
                    // ↑ button disabled on the first goal; ↓ disabled on the last
                    onMoveUp = if (index > 0) ({ onMoveUp(goal) }) else null,
                    onMoveDown = if (index < sorted.size - 1) ({ onMoveDown(goal) }) else null
                )
            }
        }
    }
}

/**
 * Card displaying a single saving goal with priority rank, months-to-reach estimate,
 * target amount, and priority control arrows.
 *
 * The rank badge colour cycles: teal for rank 1, warm stone for rank 2, and muted
 * for all subsequent ranks. The priority label text ("Top priority", "Second priority",
 * etc.) adapts based on both [displayRank] and [totalGoals].
 *
 * The months-to-reach estimate is calculated from [cumulativeTarget] — this accounts
 * for goals that must be completed before the current one in priority order. A goal
 * at rank 3 shows the total months needed for goals 1, 2, and 3 combined.
 *
 * The ↑ and ↓ priority buttons are disabled (null callbacks) when the goal is
 * already at the top or bottom of the list respectively.
 *
 * @param goal The [SavingGoalEntity] to display
 * @param displayRank The 1-indexed position in the sorted priority list
 * @param totalGoals The total number of saving goals — used to label the lowest priority
 * @param cumulativeTarget The sum of target amounts for this goal and all higher-priority
 *                         goals, used to calculate the sequential months-to-reach estimate
 * @param monthlyRemaining The remaining monthly budget in GBP
 * @param onEdit Callback invoked when the Edit button is tapped
 * @param onDelete Callback invoked when the Remove button is tapped
 * @param onMoveUp Callback invoked when ↑ is tapped, or null if already at top
 * @param onMoveDown Callback invoked when ↓ is tapped, or null if already at bottom
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
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
    // Badge colours cycle by rank — teal for #1, stone for #2, muted for the rest
    val badgeColor = when (displayRank) { 1 -> Teal700; 2 -> Terra500; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    val badgeBg   = when (displayRank) { 1 -> Teal700.copy(alpha = 0.12f); 2 -> Terra500.copy(alpha = 0.12f); else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f) }

    // Months-to-reach = ceiling(cumulative target / monthly remaining)
    // null when monthlyRemaining is zero (over budget) to avoid division by zero
    val monthsToReach: Int? = if (monthlyRemaining > 0.0) ceil(cumulativeTarget / monthlyRemaining).toInt() else null

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Rank badge circle
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
                    Text(displayRank.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = badgeColor)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    // Months estimate — label differs for rank 1 vs lower priorities
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
                // Priority arrows — greyed out when at the top or bottom of the list
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
                // Priority label badge — last goal gets "Lowest priority"
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

/**
 * Dialog for creating a new saving goal or editing an existing one.
 *
 * Pre-populates the name and target fields from [existingGoal] when editing.
 * The Save button is disabled until both a non-blank name and a valid decimal
 * target amount are entered. A hint is shown for new goals explaining that they
 * are added at the lowest priority and can be promoted with the ↑ arrows.
 *
 * @param existingGoal The goal to edit, or null when creating a new goal
 * @param onSave Callback invoked with the goal name and target amount when saved
 * @param onDismiss Callback invoked when the dialog is dismissed without saving
 */
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
                OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("What are you saving for?") }, placeholder = { Text("e.g. Holiday, PS5, New bike") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal700, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
                OutlinedTextField(value = targetInput, onValueChange = { targetInput = it }, label = { Text("Target amount") }, placeholder = { Text("e.g. 800") }, leadingIcon = { Text("£", fontWeight = FontWeight.Bold, color = Terra500, modifier = Modifier.padding(start = 4.dp)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terra500, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
                // Priority hint — only shown when adding a new goal
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

/**
 * Card form for setting or editing the monthly spending budget.
 *
 * Shows a single amount field with a £ prefix icon. The title and save button
 * label adapt between "Set your monthly budget" (first-time setup) and
 * "Edit monthly budget" (subsequent edits) based on [isNew].
 *
 * A Cancel button is shown alongside Save when [onCancel] is non-null — this
 * is only the case when a budget already exists and the user chose to edit it.
 * First-time setup has no cancel option since there is nothing to cancel to.
 *
 * @param isNew True when no budget exists yet — adjusts the form title and button label
 * @param limitInput The current text field value for the monthly limit
 * @param onLimitChange Callback invoked on each keystroke in the limit field
 * @param onCancel Callback invoked when Cancel is tapped, or null to hide Cancel
 * @param onSave Callback invoked when the Save / Set budget button is tapped
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
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
            OutlinedTextField(value = limitInput, onValueChange = onLimitChange, label = { Text("Monthly spending limit") }, placeholder = { Text("e.g. 500") }, leadingIcon = { Text("£", fontWeight = FontWeight.Bold, color = Teal700, modifier = Modifier.padding(start = 4.dp)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal700, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onCancel != null) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                }
                Button(onClick = onSave, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal700, contentColor = Color.White)) {
                    Text(if (isNew) "Set budget" else "Save", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Large animated donut arc hero card showing monthly budget progress.
 *
 * The donut arc is drawn on a [Canvas] and animates smoothly from 0% to the
 * current progress using [animateFloatAsState] with a 1000ms tween. The arc
 * colour shifts based on the spend level:
 * - Teal (midnight navy) — below 80% of budget
 * - Warm stone — at or above 80%
 * - Error red — over budget
 *
 * The centre label shows either the remaining budget or the overspend amount.
 * A three-column legend below the arc shows Spent, Budget, and Days left.
 *
 * @param spent The total amount spent this month in GBP
 * @param limit The monthly budget limit in GBP
 * @param daysLeft The number of days remaining in the current month
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
@Composable
private fun GoalDonutHero(spent: Double, limit: Double, daysLeft: Int, modifier: Modifier = Modifier) {
    val rawProgress = (spent / limit).coerceIn(0.0, 1.0).toFloat()
    val isOver = spent > limit
    val remaining = (limit - spent).coerceAtLeast(0.0)
    // Arc colour shifts at 80% and again at 100% to signal increasing urgency
    val arcColor = when { isOver -> Error700; rawProgress >= 0.8f -> Terra500; else -> Teal700 }
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, animationSpec = tween(1000), label = "donutProgress")

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(modifier = Modifier.size(188.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sw = 20.dp.toPx(); val inset = sw / 2f
                    val tl = Offset(inset, inset); val sz = Size(size.width - sw, size.height - sw)
                    // Background track — full circle at low opacity
                    drawArc(color = arcColor.copy(alpha = 0.1f), startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = sz, style = Stroke(width = sw, cap = StrokeCap.Round))
                    // Filled arc — sweeps clockwise proportional to animated progress
                    if (animatedProgress > 0f)
                        drawArc(color = arcColor, startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false, topLeft = tl, size = sz, style = Stroke(width = sw, cap = StrokeCap.Round))
                }
                // Centre label — shows remaining or overspend depending on budget state
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("£${"%.2f".format(if (isOver) spent - limit else remaining)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = if (isOver) Error700 else MaterialTheme.colorScheme.onSurface)
                    Text(if (isOver) "over budget" else "remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Three-column legend row below the donut
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

/**
 * A single label + value item in the [GoalDonutHero] legend row.
 *
 * Displays a small coloured dot, a label, and a bold value stacked vertically.
 *
 * @param dotColor The colour of the legend indicator dot
 * @param label The descriptive label shown in a smaller muted style
 * @param value The formatted value shown in bold
 */
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

/**
 * Card showing the calculated daily spending allowance for the remainder of the month.
 *
 * Computed as `remaining budget / days left`. Only shown when the user is under
 * budget and at least one day remains in the month.
 *
 * @param dailyAllowance The calculated daily allowance in GBP
 * @param daysLeft The number of days remaining in the current month
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
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

/**
 * Card showing mindful spending statistics for the current month.
 *
 * Displays two metrics side by side:
 * - **Low impulse spend** — total amount spent on receipts labelled LOW
 * - **High impulse buys** — count of receipts labelled HIGH
 *
 * Helps the user see at a glance whether their intentional purchases outweigh
 * their impulsive ones for the month.
 *
 * @param lowImpulseSpend Total GBP spent on LOW-labelled receipts this month
 * @param highImpulseCount Number of HIGH-labelled receipts this month
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
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

/**
 * A single statistic item displayed inside [MindfulSpendingCard].
 *
 * @param label The descriptive label shown in a small muted style
 * @param value The formatted value string displayed in bold
 * @param color The colour applied to the value text
 */
@Composable
private fun SavingsStatItem(label: String, value: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Teal700.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = color)
    }
}

/**
 * KIRA-branded warning card shown when the user approaches or exceeds their budget.
 *
 * Only rendered when spend is at or above 80% of the monthly limit. The card
 * adapts its colour, title, and message for two states:
 * - **Approaching limit** (80–100%) — warm stone styling, mentions remaining amount
 *   and the top-priority saving goal by name if one exists
 * - **Over budget** — error red styling, encourages the user to avoid further
 *   non-essential purchases for the rest of the month
 *
 * The KIRA "K" avatar in the corner reinforces that this message comes from the
 * AI spending coach, making it feel personal rather than a generic warning.
 *
 * @param isOverBudget True when spend exceeds the monthly limit
 * @param remaining The remaining budget in GBP — used in the approaching-limit message
 * @param topGoal The highest-priority [SavingGoalEntity], or null if no goals exist.
 *                Referenced by name in the warning message to personalise it.
 * @param modifier Optional [Modifier] applied to the root [Card]
 */
@Composable
private fun KiraGoalWarning(isOverBudget: Boolean, remaining: Double, topGoal: SavingGoalEntity?, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isOverBudget) Error100 else Terra50), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            // KIRA "K" avatar — colour matches the warning severity
            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(if (isOverBudget) Error700 else Terra700), contentAlignment = Alignment.Center) {
                Text("K", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (isOverBudget) "You have gone over budget" else "You are close to your limit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isOverBudget) Error700 else Terra700)
                // Message is personalised with the top saving goal name when available
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